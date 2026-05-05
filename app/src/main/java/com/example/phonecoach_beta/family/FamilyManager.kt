package com.example.phonecoach_beta.family

import android.content.Context
import android.view.View
import android.widget.*
import com.example.phonecoach_beta.util.UserSession
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class FamilyManager(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    var pendingInvitationId: String? = null

    //역할 따라 다르게 보임
    fun setupFamilySection(
        tvFamilyStatus: TextView,
        tvIndividualMsg: TextView,
        layoutParentSection: LinearLayout,
        layoutChildSection: LinearLayout
    ) {
        // 가족 ID 상태 표시
        tvFamilyStatus.text = if (UserSession.familyId != null) {
            "가족 ID: ${UserSession.familyId}"
        } else {
            "가족 ID: 없음"
        }

        // 역할에 따른 UI 보여주기
        when (UserSession.role) {
            "individual" -> {
                // 개인
                tvIndividualMsg.visibility = View.VISIBLE
                layoutParentSection.visibility = View.GONE
                layoutChildSection.visibility = View.GONE
            }

            "parent" -> {
                // 부모
                tvIndividualMsg.visibility = View.GONE
                layoutParentSection.visibility = View.VISIBLE
                layoutChildSection.visibility = View.GONE
            }

            "child" -> {
                // 자녀
                tvIndividualMsg.visibility = View.GONE
                layoutParentSection.visibility = View.GONE
                layoutChildSection.visibility = View.VISIBLE
            }
        }
    }

    //부모 : 가족 그룹 만들기
    fun createFamilyGroup(
        tvFamilyStatus: TextView,
        btnCreateFamily: Button,
        onSuccess: (String) -> Unit
    ) {
        if (UserSession.familyId != null) {
            Toast.makeText(context, "이미 가족 그룹이 있습니다: ${UserSession.familyId}", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val newFamilyId = UUID.randomUUID().toString().take(8)

        db.collection("users").document(UserSession.uid)
            .update("familyId", newFamilyId)
            .addOnSuccessListener {
                UserSession.familyId = newFamilyId
                tvFamilyStatus.text = "가족 ID: $newFamilyId"
                btnCreateFamily.isEnabled = false
                btnCreateFamily.text = "가족 그룹 이미 있음"
                Toast.makeText(context, "가족 그룹 생성! ID: $newFamilyId", Toast.LENGTH_LONG).show()
                onSuccess(newFamilyId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    //부모 : 자녀에게 초대 보내기 (자녀 id 이용)
    fun sendInvitation( childLoginId: String, onSuccess: () -> Unit) {
        if (childLoginId.isEmpty()) {
            Toast.makeText(context, "자녀 아이디를 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        val currentFamilyId = UserSession.familyId
        if (currentFamilyId == null) {
            Toast.makeText(context, "먼저 가족 그룹을 만들어주세요", Toast.LENGTH_SHORT).show()
            return
        }

        // 자녀 아이디로 UID 찾기
        db.collection("users")
            .whereEqualTo("loginId", childLoginId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(context, "존재하지 않는 아이디입니다", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val childDoc = documents.documents[0]
                val childUid = childDoc.getString("uid") ?: childDoc.id

                // 초대장 invitation 컬렉션에 저장
                val invitation = hashMapOf<String, Any>(
                    "fromUid" to UserSession.uid,
                    "toUid" to childUid,
                    "fromLoginId" to UserSession.loginId,
                    "familyId" to currentFamilyId,
                    "status" to "pending"
                )

                db.collection("invitations")
                    .add(invitation)
                    .addOnSuccessListener {
                        Toast.makeText(context, "${childLoginId}님에게 초대를 보냈습니다!", Toast.LENGTH_SHORT)
                            .show()
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "초대 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "검색 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    //자녀 : 받은 초대 확인
    fun checkInvitation(
        tvInvitation: TextView,
        btnAccept: Button
    ) {
        // 이미 연동됐으면
        if (UserSession.familyId != null) {
            tvInvitation.text = "가족 연동 완료! ID: ${UserSession.familyId}"
            btnAccept.visibility = View.GONE
            return
        }

        db.collection("invitations")
            .whereEqualTo("toUid", UserSession.uid)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    tvInvitation.text = "받은 초대가 없습니다"
                    btnAccept.visibility = View.GONE
                } else {
                    val doc = documents.documents[0]
                    pendingInvitationId = doc.id
                    val fromId = doc.getString("fromLoginId") ?: "알 수 없음"
                    tvInvitation.text = "${fromId}님이 가족 연동을 요청했습니다"
                    btnAccept.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "초대 확인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    //자녀 : 초대 수락
    fun acceptInvitation(
        tvFamilyStatus: TextView,
        tvInvitation: TextView,
        btnAccept: Button,
        onSuccess: (String) -> Unit
    ) {
        val invitationId = pendingInvitationId ?: return

        db.collection("invitations").document(invitationId)
            .get()
            .addOnSuccessListener { doc ->
                val familyId = doc.getString("familyId") ?: return@addOnSuccessListener

                db.collection("users").document(UserSession.uid)
                    .update("familyId", familyId)
                    .addOnSuccessListener {
                        UserSession.familyId = familyId

                        // invitation 컬렉션에서 초대장 삭제
                        db.collection("invitations").document(invitationId).delete()

                        tvFamilyStatus.text = "가족 ID: $familyId"
                        tvInvitation.text = "가족 연동 완료!"
                        btnAccept.visibility = View.GONE

                        Toast.makeText(context, "가족 연동 완료!", Toast.LENGTH_SHORT).show()
                        onSuccess(familyId)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "연동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    //user의 data 저장 (임의. 스마트폰 사용 기록data저장 아님 불러오기테스트용)
    //저장되는 데이터 : uid, loginId, familyId, data
    fun saveUserData() {
        if (UserSession.uid.isEmpty()) return

        // 내 데이터 있는지 확인
        db.collection("data")
            .whereEqualTo("uid", UserSession.uid)
            .get()
            .addOnSuccessListener { documents ->
                //data 컬렉션에 저장할 데이터
                val userData = hashMapOf<String, Any?>(
                    "uid" to UserSession.uid,
                    "loginId" to UserSession.loginId,
                    "familyId" to UserSession.familyId,  // null이어도 저장
                    "data" to "abcd"    //임시로 abcd
                )

                if (documents.isEmpty) {
                    // 처음이면 새로 저장
                    db.collection("data")
                        .add(userData)
                        .addOnSuccessListener {
                            android.util.Log.d("DATA", "데이터 저장 성공!")
                        }
                } else {
                    // 있으면 업데이트 (familyId 바뀔 경우 고려...
                    val docId = documents.documents[0].id
                    db.collection("data")
                        .document(docId)
                        .set(userData)
                        .addOnSuccessListener {
                            android.util.Log.d("DATA", "데이터 업데이트 성공!")
                        }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("DATA", "저장 실패: ${e.message}")
            }
    }


    fun loadSpecificChildData(
        childUid: String,
        onSuccess: (String) -> Unit
    ) {
        db.collection("data")
            .whereEqualTo("uid", childUid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(context, "데이터가 없습니다", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val data = documents.documents[0].getString("data") ?: "데이터 없음"
                onSuccess(data)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * 부모: 연동된 자녀 목록 가져오기
     */
    fun loadChildList(
        onSuccess: (List<Map<String, Any>>) -> Unit
    ) {
        val familyId = UserSession.familyId
        if (familyId == null) return

        db.collection("users")
            .whereEqualTo("familyId", familyId)
            .whereEqualTo("role", "child")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) return@addOnSuccessListener

                val childList = documents.map { it.data }
                onSuccess(childList)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FAMILY", "자녀 목록 조회 실패: ${e.message}")
            }
    }


}