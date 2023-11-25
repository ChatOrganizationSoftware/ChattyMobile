package com.example.chatty

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SignupPage : AppCompatActivity() {
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmEditText: EditText
    private lateinit var signUpButton: Button
    private lateinit var returnLoginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_page)

        nameEditText = findViewById(R.id.nameField)
        emailEditText = findViewById(R.id.emailField)
        passwordEditText = findViewById(R.id.passwordField)
        confirmEditText = findViewById(R.id.confirmPasswordField)
        signUpButton = findViewById(R.id.signUpButton)
        returnLoginButton = findViewById(R.id.returnLoginButton)

        signUpButton.setOnClickListener {
            val intent = Intent(this, EmailVerificationPage::class.java)
            startActivity(intent)
        }

        returnLoginButton.setOnClickListener {
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)
        }
    }
}