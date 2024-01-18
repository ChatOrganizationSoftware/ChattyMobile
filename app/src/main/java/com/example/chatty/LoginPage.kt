package com.example.chatty

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginPage : AppCompatActivity() {
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var loginEye: ImageView
    private lateinit var forgotPassword: TextView
    private lateinit var loginButton: Button
    private lateinit var signUp: TextView
    // Show/Hide password
    private var showPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_page)

        // Components on page
        emailField = findViewById(R.id.emailField)
        passwordField = findViewById(R.id.passwordField)
        loginEye = findViewById(R.id.loginEye)
        forgotPassword = findViewById(R.id.forgotPassword)
        loginButton = findViewById(R.id.loginButton)
        signUp = findViewById(R.id.signUp)

        // Click listener for eye icon to show/hide password
        loginEye.setOnClickListener {
            if (!showPassword) {
                passwordField.transformationMethod = HideReturnsTransformationMethod.getInstance()
                showPassword = true
            } else {
                passwordField.transformationMethod = PasswordTransformationMethod.getInstance()
                showPassword = false
            }
        }

        // Click listener for Forgot Password
        forgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordPage::class.java)
            startActivity(intent)
        }

        // Click listener for Login Button
        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                showToast("Please enter both email and password fields")
            } else {
                signIn(email, password)
            }
        }

        // Click listener for Sign Up
        signUp.setOnClickListener {
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
        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // Login Failed
                if (!task.isSuccessful)
                    return@addOnCompleteListener
                // Login Successful

                if(FirebaseAuth.getInstance().currentUser!!.isEmailVerified) {
                    FirebaseDatabase.getInstance().getReference("/users/${FirebaseAuth.getInstance().uid}/active").setValue(true)

                    val intent = Intent(this, MainPage::class.java)
                    startActivity(intent)
                    finish()
                }
                else{
                    val intent = Intent(this, EmailVerificationPage::class.java)
                    startActivity(intent)
                }

            }
            .addOnFailureListener{
                showToast("Failed to Login: ${it.message}")
            }
    }
}