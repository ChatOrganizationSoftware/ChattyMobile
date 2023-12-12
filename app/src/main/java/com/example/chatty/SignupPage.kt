package com.example.chatty

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class SignupPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: CollectionReference


    // Components on Page
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmEditText: EditText
    private lateinit var signUpButton: Button
    private lateinit var returnLoginButton: Button
    private lateinit var eye1: ImageView
    private lateinit var eye2: ImageView
    private lateinit var emailVerification: EmailVerificationPage // Create an instance

    private var showPassword = false
    private var showConfirm = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_page)

        auth = FirebaseAuth.getInstance()
        emailVerification = EmailVerificationPage()

        // Components on Page
        nameEditText = findViewById(R.id.nameField)
        emailEditText = findViewById(R.id.emailField)
        passwordEditText = findViewById(R.id.passwordField)
        confirmEditText = findViewById(R.id.confirmPasswordField)
        signUpButton = findViewById(R.id.signUpButton)
        returnLoginButton = findViewById(R.id.returnLoginButton)

        // Eye icons for show/hide password and confirm password
        eye1 = findViewById(R.id.eye1)
        eye2 = findViewById(R.id.eye2)

        // Show/hide Password
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

        // Show/hide Confirm Password
        eye2.setOnClickListener{
            if(!showConfirm){
                confirmEditText.transformationMethod = HideReturnsTransformationMethod()
                showConfirm = true
            }
            else{
                confirmEditText.transformationMethod = PasswordTransformationMethod()
                showConfirm = false
            }
        }

        // Click listener for Continue Button
        signUpButton.setOnClickListener {
            val userName = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmEditText.text.toString().trim()

            if(userName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() )
                showToast("Please fill all the fields")
            else if(password != confirmPassword)
                showToast("Password and Confirm Password should match")
            else{
                register(userName, email, password)
            }
        }

        // Click listener for Return to Login Button
        returnLoginButton.setOnClickListener {
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)
        }
    }

    // Sign Up Method
    private fun register(userName:String, email: String, password: String){
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this){
            if(it.isSuccessful){
                val user = auth.currentUser
                val userId : String = user!!.uid
                val newUser = User(userId, userName)
                database = FirebaseFirestore.getInstance().collection("Users")

                // Add new user to database
                database.add(newUser)
                    .addOnSuccessListener(OnSuccessListener<DocumentReference?> {
                        // Adding the user is successful. Direct to Main Page
                        val intent = Intent(this, MainPage::class.java)
                        startActivity(intent)

                        emailVerification.sendEmailForVerification(user)
                    })
                    .addOnFailureListener(OnFailureListener {
                        // Adding the user is failed. Show error message.
                        showToast("Sign Up:Failed ")
                    })
            }
        }
    }

    // Shows a message on the screen
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}