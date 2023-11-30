package com.example.chatty

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class LoginPage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    // Components on page
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var forgotPasswordLink : TextView
    private lateinit var loginButton: Button
    private lateinit var signupLink : TextView
    private lateinit var eye1: ImageView

    // Show/Hide password
    private var showPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        auth = FirebaseAuth.getInstance()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_page)

        // Components on page
        emailEditText = findViewById(R.id.emailField)
        passwordEditText = findViewById(R.id.passwordField)
        forgotPasswordLink = findViewById(R.id.forgotPassword)
        loginButton = findViewById(R.id.loginButton)
        signupLink = findViewById(R.id.signUp)
        eye1 = findViewById(R.id.eye1)

        // Click listener for eye icon to show/hide password
        eye1.setOnClickListener{
            if(!showPassword){
                passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                showPassword = true
            }
            else{
                passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                showPassword = false
            }
        }

        // Click listener for Forgot Password
        forgotPasswordLink.setOnClickListener {
            val intent = Intent(this, ForgotPasswordPage::class.java)
            startActivity(intent)
        }

        // Click listener for Login Button
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showToast("Please enter both email and password fields")
            } else {
                signIn(email, password);
            }
        }

        // Click listener for Sign Up
        signupLink.setOnClickListener {
            val intent = Intent(this, SignupPage::class.java)
            startActivity(intent)
        }
    }

    // Shows a message on screen
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Sign In function
    private fun signIn(email:String, password: String){
        this.auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login Successful
                    val intent = Intent(this, MainPage::class.java)
                    startActivity(intent)
                } else {
                    // Login Failed
                    showToast("Login failed: Please enter valid Email and Password");
                }
            }
    }
}