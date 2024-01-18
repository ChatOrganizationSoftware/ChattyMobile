package com.example.chatty

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailFieldChangePasswordPage: EditText
    private lateinit var resetPasswordButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.change_password_page)

        auth = FirebaseAuth.getInstance()

        // reset password button
        resetPasswordButton = findViewById(R.id.resetPasswordButton)
        emailFieldChangePasswordPage = findViewById(R.id.emailFieldChangePasswordPage)

        resetPasswordButton.setOnClickListener {
            val email = emailFieldChangePasswordPage.text.toString().trim()
            if (email.isEmpty()) {
                showToast("Please enter email")
            } else {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            showToast("Email for resetting password sent to $email")
                            finish()
                        } else {
                            showToast("Email for resetting password not sent to $email")
                        }
                    }
            }
        }
    }

    // Shows a message on the screen
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}