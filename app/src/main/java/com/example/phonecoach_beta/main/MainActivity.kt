package com.example.phonecoach_beta.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.phonecoach_beta.R
import com.example.phonecoach_beta.auth.LoginActivity
import com.example.phonecoach_beta.family.FamilyManager
import com.example.phonecoach_beta.util.UserSession
import com.google.firebase.auth.FirebaseAuth
import android.widget.ScrollView

class MainActivity : AppCompatActivity() {

    // 각 페이지 레이아웃
    private lateinit var layoutHome: LinearLayout
    private lateinit var layoutCategory: LinearLayout
    private lateinit var layoutHistory: LinearLayout
    private lateinit var layoutFeedback: LinearLayout
    private lateinit var layoutSettings: ScrollView

    // 하단 버튼들
    private lateinit var btnNavHome: Button
    private lateinit var btnNavCategory: Button
    private lateinit var btnNavHistory: Button
    private lateinit var btnNavFeedback: Button
    private lateinit var btnNavSettings: Button

    // 가족연동
    private lateinit var familyManager: FamilyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        familyManager = FamilyManager(this)

        //연결
        // 레이아웃 연결
        layoutHome = findViewById(R.id.layoutHome)
        layoutCategory = findViewById(R.id.layoutCategory)
        layoutHistory = findViewById(R.id.layoutHistory)
        layoutFeedback = findViewById(R.id.layoutFeedback)
        layoutSettings = findViewById(R.id.layoutSettings)

        // 하단 버튼 연결
        btnNavHome = findViewById(R.id.btnNavHome)
        btnNavCategory = findViewById(R.id.btnNavCategory)
        btnNavHistory = findViewById(R.id.btnNavHistory)
        btnNavFeedback = findViewById(R.id.btnNavFeedback)
        btnNavSettings = findViewById(R.id.btnNavSettings)

        // 로그아웃 버튼 연결
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        showPage("home")

        // 하단 버튼 클릭
        btnNavHome.setOnClickListener {
            showPage("home")
            setupHomePage()
        }
        btnNavCategory.setOnClickListener { showPage("category") }
        btnNavHistory.setOnClickListener { showPage("history") }
        btnNavFeedback.setOnClickListener { showPage("feedback") }
        btnNavSettings.setOnClickListener {
            showPage("settings")
            setupSettingsPage()
        }

        // 로그아웃
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut() // 토큰 삭제
            UserSession.clear()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun showPage(page: String) {
        layoutHome.visibility = View.GONE
        layoutCategory.visibility = View.GONE
        layoutHistory.visibility = View.GONE
        layoutFeedback.visibility = View.GONE
        layoutSettings.visibility = View.GONE

        when (page) {
            "home" -> layoutHome.visibility = View.VISIBLE
            "category" -> layoutCategory.visibility = View.VISIBLE
            "history" -> layoutHistory.visibility = View.VISIBLE
            "feedback" -> layoutFeedback.visibility = View.VISIBLE
            "settings" -> layoutSettings.visibility = View.VISIBLE
        }
    }

    // 설정 페이지 가족연동 UI어쩌구
    private fun setupSettingsPage() {
        val tvFamilyStatus = findViewById<TextView>(R.id.tvFamilyStatus)
        val tvIndividualMsg = findViewById<TextView>(R.id.tvIndividualMsg)
        val layoutParentSection = findViewById<LinearLayout>(R.id.layoutParentSection)
        val layoutChildSection = findViewById<LinearLayout>(R.id.layoutChildSection)

        familyManager.setupFamilySection(
            tvFamilyStatus,
            tvIndividualMsg,
            layoutParentSection,
            layoutChildSection
        )

        // 부모 버튼들
        if (UserSession.role == "parent") {
            val btnCreateFamily = findViewById<Button>(R.id.btnCreateFamily)
            val etChildId = findViewById<EditText>(R.id.etChildId)
            val btnInviteChild = findViewById<Button>(R.id.btnInviteChild)

            if (UserSession.familyId != null) {
                btnCreateFamily.isEnabled = false
                btnCreateFamily.text = "가족 그룹 이미 있음"
            }

            btnCreateFamily.setOnClickListener {
                familyManager.createFamilyGroup(tvFamilyStatus, btnCreateFamily) {}
            }

            btnInviteChild.setOnClickListener {
                val childId = etChildId.text.toString().trim()
                familyManager.sendInvitation(childId) {
                    etChildId.text.clear()
                }
            }
        }

        // 자녀 버튼들
        if (UserSession.role == "child") {
            val tvInvitation = findViewById<TextView>(R.id.tvInvitation)
            val btnAccept = findViewById<Button>(R.id.btnAcceptInvitation)

            familyManager.checkInvitation(tvInvitation, btnAccept)

            btnAccept.setOnClickListener {
                familyManager.acceptInvitation(tvFamilyStatus, tvInvitation, btnAccept) {}
            }
        }
    }

    private fun setupHomePage() {
        val layoutChildButtons = findViewById<LinearLayout>(R.id.layoutChildButtons)
        val layoutChildButtonList = findViewById<LinearLayout>(R.id.layoutChildButtonList)
        val scrollChildData = findViewById<ScrollView>(R.id.scrollChildData)
        val tvSelectedChild = findViewById<TextView>(R.id.tvSelectedChild)
        val tvChildDataResult = findViewById<TextView>(R.id.tvChildDataResult)
        val tvHomeDefault = findViewById<TextView>(R.id.tvHomeDefault)

        // 사용자가 부모 + familyid 있는 경우
        if (UserSession.isParent && UserSession.familyId != null) {
            tvHomeDefault.visibility = View.GONE
            layoutChildButtons.visibility = View.VISIBLE

            // 기존 버튼 초기화
            layoutChildButtonList.removeAllViews()

            // 자녀 목록 불러오기
            familyManager.loadChildList { childList ->

                if (childList.isEmpty()) {
                    tvHomeDefault.visibility = View.VISIBLE
                    tvHomeDefault.text = "연동된 자녀가 없습니다"
                    return@loadChildList
                }

                // 자녀마다 버튼 동적 생성
                childList.forEach { child ->
                    val childUid = child["uid"] as? String ?: return@forEach
                    val childLoginId = child["loginId"] as? String ?: "알 수 없음"

                    // 버튼 만들기
                    val btn = Button(this).apply {
                        text = childLoginId          // 버튼에 자녀 아이디 표시
                        textSize = 16f
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, 12)  // 버튼 간격
                        }
                    }

                    // 버튼 클릭 시 해당 자녀 data 불러오기
                    btn.setOnClickListener {
                        tvSelectedChild.text = "${childLoginId}의 데이터"
                        scrollChildData.visibility = View.VISIBLE

                        familyManager.loadSpecificChildData(childUid) { data ->
                            tvChildDataResult.text = data
                        }
                    }

                    // 버튼 목록에 추가
                    layoutChildButtonList.addView(btn)
                }
            }
        } else {
            // 부모가 아니거나 familyId 없으면=> 기본 화면
            tvHomeDefault.visibility = View.VISIBLE
            layoutChildButtons.visibility = View.GONE
            scrollChildData.visibility = View.GONE

        }
    }
}