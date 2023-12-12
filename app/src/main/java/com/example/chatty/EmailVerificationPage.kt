package com.example.chatty

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class EmailVerificationPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.emailverification_page)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        currentUser?.let {
            sendEmailForVerification(it)
        }
    }

    fun sendEmailForVerification(user: FirebaseUser) {
        user.sendEmailVerification().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showVerificationSuccessDialog()
            } else {
                showVerificationFailureDialog()
            }
        }
    }

    private fun showVerificationSuccessDialog() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Verification Link Sent")
            .setMessage("Link has successfully sent to your email. Please verify your email as soon as possible.")
            .setPositiveButton("OK") { dialog, which ->
                // You can add any additional actions after successful verification
                // For example, navigate to another activity
                val intent = Intent(this, AnotherActivity::class.java)
                startActivity(intent)
            }
            .show()
    }

    private fun showVerificationFailureDialog() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Error")
            .setMessage("Something went wrong. Not able to send verification link to your email.")
            .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->


            })
            .show()
    }
}
