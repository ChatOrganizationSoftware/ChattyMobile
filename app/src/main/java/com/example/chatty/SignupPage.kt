package com.example.chatty

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID


class SignupPage : AppCompatActivity() {

    private lateinit var nameField: EditText
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var confirmField: EditText
    private lateinit var signupEye1: ImageView
    private lateinit var signupEye2: ImageView
    private lateinit var signUpButton: Button
    private lateinit var returnLoginButton: Button
    private lateinit var selectPhotoButton: ImageView
    private lateinit var selectPhotoText: TextView
    private var selectedPhotoUri: Uri? = null

    private var showPassword = false
    private var showConfirm = false
    private var clicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_page)

        // Components on Page
        selectPhotoButton = findViewById(R.id.selectPhotoButton)
        selectPhotoText = findViewById(R.id.selectPhotoTextSignup)
        nameField = findViewById(R.id.nameField)
        emailField = findViewById(R.id.emailField)
        passwordField = findViewById(R.id.passwordField)
        confirmField = findViewById(R.id.confirmPasswordField)
        signupEye1 = findViewById(R.id.signupEye1)
        signupEye2 = findViewById(R.id.signupEye2)
        signUpButton = findViewById(R.id.signUpButton)
        returnLoginButton = findViewById(R.id.returnLoginButton)


        selectPhotoButton.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        // Show/hide Password
        signupEye1.setOnClickListener{
            if(!showPassword){
                passwordField.transformationMethod = HideReturnsTransformationMethod.getInstance()
                showPassword = true
            }
            else{
                passwordField.transformationMethod = PasswordTransformationMethod.getInstance()
                showPassword = false
            }
        }

        // Show/hide Confirm Password
        signupEye2.setOnClickListener{
            if(!showConfirm){
                confirmField.transformationMethod = HideReturnsTransformationMethod()
                showConfirm = true
            }
            else{
                confirmField.transformationMethod = PasswordTransformationMethod()
                showConfirm = false
            }
        }

        // Click listener for Continue Button
        signUpButton.setOnClickListener {
            if (!clicked) {
                val name = nameField.text.toString().trim()
                val email = emailField.text.toString().trim()
                val password = passwordField.text.toString().trim()
                val confirmPass = confirmField.text.toString().trim()
                if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPass.isEmpty())
                    showToast("Please fill all the fields")
                else if (password != confirmPass)
                    showToast("Password and Confirm Password should match")
                else {
                    clicked = true
                    register(email, password)
                }
            }
        }

        // Click listener for Return to Login Button
        returnLoginButton.setOnClickListener {
            if(!clicked)
                finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode==0 && resultCode==Activity.RESULT_OK && data!=null){
            selectedPhotoUri = data.data

            selectPhotoText.visibility = View.GONE
            selectPhotoButton.background = null
            selectPhotoButton.setImageURI(selectedPhotoUri)
        }
    }

    // Sign Up Method
    private fun register(email: String, password: String){
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener(this){
            if(it.isSuccessful){
                uploadImagetoFirebase()
            }
        }.addOnFailureListener{
            showToast("Failed to Sign Up: ${it.message} ")
        }
    }

    private  fun uploadImagetoFirebase(){
        if(selectedPhotoUri == null)
            saveUsertoFirebase("")
        else {
            val filename = UUID.randomUUID().toString()
            val ref = FirebaseStorage.getInstance().getReference("/Profile Photos/${filename}")

            ref.putFile(selectedPhotoUri!!).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener {
                    saveUsertoFirebase(it.toString())
                }
            }.addOnFailureListener {
                showToast("Failed to save the photo: ${it.message}")
            }
        }
    }

    private fun saveUsertoFirebase(profileImageUri: String){
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("users/$uid")

        val user = User(uid!!, nameField.text.toString().trim(), profileImageUri)
        ref.setValue(user).addOnSuccessListener {
            val intent = Intent(this, MainPage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }.addOnFailureListener{
            showToast("Failed to store the data: ${it.message}")
        }
    }

    // Shows a message on the screen
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}