package com.example.phonecoach_beta.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.phonecoach_beta.R
import com.example.phonecoach_beta.main.MainActivity
import com.example.phonecoach_beta.util.UserSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //토큰확인
        val currentUser = auth.currentUser
        if (currentUser != null) {
            //토큰 유효 확인, 자동로그인
            loadUserAndGoMain(currentUser.uid)
            return
        }



        setContentView(R.layout.activity_login)

        val etLoginId = findViewById<EditText>(R.id.etLoginId)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
//        val btnForgotPassword = findViewById<Button>(R.id.btnForgotPassword)

        btnLogin.setOnClickListener {
            val loginId = etLoginId.text.toString().trim()
            val password = etPassword.text.toString()

            if (loginId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            login(loginId, password)
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

//        btnForgotPassword.setOnClickListener {
//            startActivity(Intent(this, ForgotPasswordActivity::class.java))
//        }
    }

    private fun login(loginId: String, password: String) {
        //이메일로 로그인아이디 (loginId)찾기
        db.collection("users")
            .whereEqualTo("loginId", loginId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "존재하지 않는 아이디입니다", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val email = documents.documents[0].getString("email") ?: ""

                // 기본 로그인
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: return@addOnSuccessListener
                        loadUserAndGoMain(uid)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "비밀번호가 틀렸습니다", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserAndGoMain(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                UserSession.uid = uid
                UserSession.loginId = doc.getString("loginId") ?: ""
                UserSession.email = doc.getString("email") ?: ""
                UserSession.role = doc.getString("role") ?: ""
                UserSession.familyId = doc.getString("familyId")

                //역할...
                if (UserSession.role.isEmpty()) {
                    startActivity(Intent(this, RoleSelectActivity::class.java))
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                finish()
            }
    }
}