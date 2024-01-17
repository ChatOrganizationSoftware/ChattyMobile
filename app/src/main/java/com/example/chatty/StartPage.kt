package com.example.chatty

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth

class StartPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // Redirect based on authentication status
        if (currentUser != null) {
            // User is already logged in, start the main activity
            startActivity(Intent(this, MainPage::class.java))
        } else {
            // User is not logged in, start the login activity
            startActivity(Intent(this, LoginPage::class.java))
        }
        finish()
    }
}