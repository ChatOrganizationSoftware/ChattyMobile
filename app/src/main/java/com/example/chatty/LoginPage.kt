package com.example.chatty

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class LoginPage : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var forgotPasswordLink : TextView
    private lateinit var loginButton: Button
    private lateinit var signupLink : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_page)

        emailEditText = findViewById(R.id.emailField)
        passwordEditText = findViewById(R.id.passwordField)
        forgotPasswordLink = findViewById(R.id.forgotPassword)
        loginButton = findViewById(R.id.loginButton)
        signupLink = findViewById(R.id.signUp)

        forgotPasswordLink.setOnClickListener {
            val intent = Intent(this, ForgotPasswordPage::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showToast("Please enter both email and password")
            } else {
                // Here you can perform login authentication using the email and password
                // For example, you can call a function like authenticateUser(email, password)
                // and handle the authentication logic there
                // For simplicity, just displaying a success message for now
                showToast("Login successful!")
            }
        }

        signupLink.setOnClickListener {
            val intent = Intent(this, SignupPage::class.java)
            startActivity(intent)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}