package com.example.phonecoach_beta.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.phonecoach_beta.R

class PermissionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        //이미 동의했는지 확인
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("agreed", false)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val btnNotification = findViewById<Button>(R.id.btnNotification)
        val btnBattery = findViewById<Button>(R.id.btnBattery)
        val btnUsageStats = findViewById<Button>(R.id.btnUsageStats)
        val cbAgree = findViewById<CheckBox>(R.id.cbAgree)
        val btnNext = findViewById<Button>(R.id.btnNext)

        // 알림 접근 권한 설정으로 이동
        btnNotification.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        // 배터리 최적화 해제 설정으로 이동
        btnBattery.setOnClickListener {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }

        // 사용 정보 접근 허용 설정으로 이동
        btnUsageStats.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        // 다음 버튼
        btnNext.setOnClickListener {
            if (!cbAgree.isChecked) {
                Toast.makeText(this, "개인정보 수집 및 이용에 동의해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            //동의기록저장 : SharedPreferences에 저장
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("agreed", true)
                .apply()
            // 동의 완료, 로그인 화면으로
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // 배터리 최적화 해제 됐는지 확인
    private fun isBatteryOptimizationIgnored(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }
}