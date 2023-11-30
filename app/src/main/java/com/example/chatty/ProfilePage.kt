package com.example.chatty

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class ProfilePage: AppCompatActivity() {
    private lateinit var name: String
    private lateinit var about: String
    private lateinit var profileImage: String

    // Icon for returning back to Main Page
    private lateinit var leftArrow: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_page)

        leftArrow = findViewById(R.id.leftArrow)

        // Return back to Main Page
        leftArrow.setOnClickListener{
            val intent = Intent(this, MainPage::class.java)
            startActivity(intent)
        }
    }
}