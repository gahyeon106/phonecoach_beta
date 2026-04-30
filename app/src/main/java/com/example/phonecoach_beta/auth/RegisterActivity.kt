package com.example.phonecoach_beta.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.phonecoach_beta.R
import com.example.phonecoach_beta.util.PasswordUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etLoginId = findViewById<EditText>(R.id.etLoginId)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etPasswordConfirm = findViewById<EditText>(R.id.etPasswordConfirm)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val loginId = etLoginId.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val passwordConfirm = etPasswordConfirm.text.toString()

            // 입력값 검사
            if (loginId.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "비밀번호는 6자 이상이어야 합니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            //비밀번호 2번 확인
            if (password != passwordConfirm) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //아이디 중복 확인
            db.collection("users")
                .whereEqualTo("loginId", loginId)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        Toast.makeText(this, "이미 사용 중인 아이디입니다", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    register(loginId, email, password)
                }
        }
    }

    private fun register(loginId: String, email: String, password: String) {
        val salt = PasswordUtils.generateSalt()
        val passwordHash = PasswordUtils.hashPassword(password, salt)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                // Firestore에 정보 저장
                val userData = hashMapOf(
                    "uid" to uid,
                    "loginId" to loginId,
                    "email" to email,
                    "passwordHash" to passwordHash,
                    "salt" to salt,
                    "role" to null,
                    "familyId" to null
                )

                db.collection("users").document(uid)
                    .set(userData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "회원가입 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}