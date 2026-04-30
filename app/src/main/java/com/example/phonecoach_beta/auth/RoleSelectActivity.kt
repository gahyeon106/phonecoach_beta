package com.example.phonecoach_beta.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.phonecoach_beta.R
import com.example.phonecoach_beta.main.MainActivity
import com.example.phonecoach_beta.util.UserSession
import com.google.firebase.firestore.FirebaseFirestore

class RoleSelectActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_select)

        val btnIndividual = findViewById<Button>(R.id.btnIndividual)
        val btnParent = findViewById<Button>(R.id.btnParent)
        val btnChild = findViewById<Button>(R.id.btnChild)

        btnIndividual.setOnClickListener {
            saveRole("individual")
        }

        btnParent.setOnClickListener {
            saveRole("parent")
        }

        btnChild.setOnClickListener {
            saveRole("child")
        }
    }

    private fun saveRole(role: String) {
        // DB에 역할 저장
        db.collection("users").document(UserSession.uid)
            .update("role", role)
            .addOnSuccessListener {
                // UserSession에도 저장
                UserSession.role = role

                Toast.makeText(this, "역할이 설정되었습니다!", Toast.LENGTH_SHORT).show()

                // 메인화면으로 이동
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}