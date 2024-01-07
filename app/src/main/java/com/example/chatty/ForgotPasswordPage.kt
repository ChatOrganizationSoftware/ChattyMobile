package com.example.chatty

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailFieldForgotPasswordPage: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgotpassword_page)

        auth = FirebaseAuth.getInstance()



        // reset password button
        val resetPasswordButton = findViewById<Button>(R.id.resetPasswordButton)
        resetPasswordButton.setOnClickListener {
            val email = findViewById<EditText>(R.id.emailFieldForgotPasswordPage).text.toString().trim()
            if (email.isEmpty()) {
                showToast("Please enter email")
            } else {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            showToast("Email sent to $email")
                            finish()
                        } else {
                            showToast("Email not sent to $email")
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
