package com.example.chatty

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.util.UUID

class UpdateProfile: AppCompatActivity() {
    private lateinit var database: DatabaseReference

    private lateinit var editProfilePhoto: ImageView

    private lateinit var nameEditText: EditText
    private lateinit var aboutEditText: EditText

    private lateinit var radioGroup: RadioGroup
    private lateinit var privateCheck: RadioButton
    private lateinit var publicCheck: RadioButton

    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button

    private var visibility = "Public"
    private var uid = FirebaseAuth.getInstance().uid

    private lateinit var oldImage: String
    private var newImage: Uri? = null

    private var clicked = false

    private var updates = hashMapOf<String, Any>()

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {

        val isDarkTheme = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            .getBoolean("DARK_THEME", false)

        if (isDarkTheme) {
            setTheme(R.style.Theme_Chatty_Dark)  // Önceden tanımlanmış karanlık tema
        } else {
            setTheme(R.style.Theme_Chatty_Light)  // Önceden tanımlanmış aydınlık tema
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.updateprofile_page)

        nameEditText = findViewById(R.id.nameEditText)
        aboutEditText = findViewById(R.id.aboutEditText)

        radioGroup = findViewById(R.id.radioGroup)
        publicCheck = findViewById(R.id.publicCheck)
        privateCheck = findViewById(R.id.privateCheck)

        confirmButton = findViewById(R.id.confirmButton)
        cancelButton = findViewById(R.id.cancelButton)

        editProfilePhoto = findViewById(R.id.imageEdit)

        FirebaseDatabase.getInstance().getReference("users/$uid")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(!snapshot.exists()){
                        startActivity(Intent(this@UpdateProfile, LoginPage::class.java))

                        finishAffinity()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        FirebaseDatabase.getInstance().getReference("users/$uid")
            .addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && !snapshot.child("deleted").exists()) {
                    // Retrieve user data
                    val userData = snapshot.getValue(User::class.java)
                    oldImage = userData!!.profilePhoto

                    if(oldImage != "")
                        Picasso.get().load(oldImage).into(editProfilePhoto)     // Replace imageView with your ImageView reference

                    if(userData.visibility == "Public")
                        radioGroup.check(R.id.publicCheck)
                    else
                        radioGroup.check(R.id.privateCheck)

                    // Update TextViews with user information
                    nameEditText.setText(userData.username)
                    aboutEditText.setText(userData.about)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })

        editProfilePhoto.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        radioGroup.setOnCheckedChangeListener { _, _ ->
            run {
                if (publicCheck.isChecked) {
                    visibility = "Public"
                    privateCheck.isChecked = false
                } else {
                    visibility = "Private"
                    privateCheck.isChecked = true
                }
            }
        }

        cancelButton.setOnClickListener{
            if(!clicked)
                finish()
        }

        confirmButton.setOnClickListener{
            if(!clicked) {
                updates["username"] = nameEditText.text.toString().trim()
                updates["about"] = aboutEditText.text.toString().trim()
                updates["visibility"] = visibility

                if(updates["username"].toString().length > 20 )
                    showToast("Your name can't be longer than 20 characters")
                else if(updates["username"].toString().isEmpty())
                    showToast("You can't leave your name empty")
                else if(updates["about"].toString().length > 150)
                    showToast("Your about field can't be longer than 150 characters")
                else {
                    clicked = true
                    saveNewImage()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode==0 && resultCode==Activity.RESULT_OK && data!=null){
            newImage = data.data

            editProfilePhoto.background = null
            editProfilePhoto.setImageURI(newImage)
        }
    }

    private fun saveNewImage(){
        if(newImage == null)
            saveUpdates("")
        else {
            val filename = UUID.randomUUID().toString()
            val ref = FirebaseStorage.getInstance().getReference("/Profile Photos/${filename}")

            ref.putFile(newImage!!).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener {
                    if(oldImage != ""){
                        FirebaseStorage.getInstance().getReferenceFromUrl(oldImage).delete()
                    }
                    saveUpdates(it.toString())
                }
            }.addOnFailureListener {
                showToast("Failed to save the photo: ${it.message}")
            }
        }
    }

    private fun saveUpdates(profileImageUri: String){
        val ref = FirebaseDatabase.getInstance().getReference("users/$uid")

        if(profileImageUri!="") {
            updates["profilePhoto"] = profileImageUri
        }

        ref.updateChildren(updates).addOnSuccessListener {
            finish()
        }.addOnFailureListener{
            showToast("Failed to store the data: ${it.message}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}