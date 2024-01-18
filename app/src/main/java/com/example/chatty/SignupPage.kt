package com.example.chatty

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.CheckBox
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
    private lateinit var kvkkLink: TextView
    private lateinit var kvkkCheckBox: CheckBox
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
        kvkkCheckBox = findViewById(R.id.kvkkCheckBox)

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
            // Önce KVKK kutusunu kontrol et
            if (!kvkkCheckBox.isChecked) {
                showToast("Please read and agree to the User Agreement and Privacy Policy")
                return@setOnClickListener
            }

            // Eğer clicked daha önce ayarlanmamışsa, kayıt işlemini başlat
            if (!clicked) {
                val name = nameField.text.toString().trim()
                val email = emailField.text.toString().trim()
                val password = passwordField.text.toString().trim()
                val confirmPass = confirmField.text.toString().trim()

                if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
                    showToast("Please fill all the fields")
                } else if (password != confirmPass) {
                    showToast("Password and Confirm Password should match")
                } else {
                    clicked = true
                    register(email, password)
                }
            }
        }

            kvkkLink = findViewById(R.id.kvkkLink)
            kvkkLink.setOnClickListener {
                // KVKK metnini göstermek için Dialog aç
                AlertDialog.Builder(this)
                    .setTitle("User Agreement and Privacy Policy")
                    .setMessage("Welcome!\n" +
                            "Before registering for our application, please carefully read the following User Agreement and Privacy Policy. By continuing, you accept these terms.\n\n" +
                            "1. Security and Privacy\n" +
                            "Our application is committed to protecting your security and privacy. In this regard, we continuously review and improve our security infrastructure.\n\n" +
                            "Our security measures are supported by Firebase services. This means that our security protocols and applications are based on the security standards and technologies provided by Firebase. However, we have limited information about the specific details of these protocols.\n\n" +
                            "The privacy of your personal data is extremely important to us. These data are used only to provide and improve our services and are processed in compliance with applicable laws.\n\n" +
                            "2. Data Retention and Usage\n" +
                            "The information you provide during registration will be used to manage your account and improve our services.\n\n" +
                            "Your messages will be stored in our system for a certain period and then automatically deleted.\n\n" +
                            "Anonymous data collection for statistical analysis and system improvements may be conducted.\n\n" +
                            "3. User Responsibility\n" +
                            "As a user, you commit to not using the application for illegal purposes.\n\n" +
                            "Respecting copyright, intellectual property, and other legal rights is your responsibility.\n\n" +
                            "4. Changes and Updates\n" +
                            "Our application and policies may be updated from time to time. Please check this document regularly to be aware of such changes.\n\n" +
                            "Contact and Support\n" +
                            "If you have any questions, feedback, or concerns about our application and services, please do not hesitate to contact us. We value your opinions to continuously improve the user experience.\n\n" +
                            "Contact Information:\n" +
                            "Product Owner: Lorem Ipsum\n" +
                            "Product Manager: Lorem Ipsum\n" +
                            "Our door is always open for any feedback, suggestions, and questions. We commit to getting back to you as soon as possible.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
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
        ref.setValue(user)
            .addOnCompleteListener {
                sendVerificationEmail()
            }
    }

    private fun navigateToEmailVerificationPage() {
        val intent = Intent(this, EmailVerificationPage::class.java)
        startActivity(intent)
        finish()
    }

    private fun sendVerificationEmail() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        user?.sendEmailVerification()
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    showToast("Verification email sent to ${user.email}")
                    navigateToEmailVerificationPage()
                } else {
                    showToast("Failed to send verification email: ${task.exception?.message}")
                }
            }
    }


    // Shows a message on the screen
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}