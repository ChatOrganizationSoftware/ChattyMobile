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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.util.UUID

class UpdateProfile: AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
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

    private lateinit var oldImage: String
    private var newImage: Uri? = null

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
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

        auth = FirebaseAuth.getInstance()

        val user = FirebaseAuth.getInstance().currentUser!!.uid
        database = FirebaseDatabase.getInstance().getReference("users/$user")
        database.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Retrieve user data
                    val userData = documentSnapshot.getValue<User>()!!
                    oldImage = userData.profilePhoto

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
            .addOnFailureListener {
                showToast("Error accessing the database")
            }

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
            finish()
        }

        confirmButton.setOnClickListener{
            saveNewImage()
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
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("users/$uid")

        var updates = HashMap<String, Any>()
        updates["username"] = nameEditText.text.toString()
        updates["about"] = aboutEditText.text.toString()
        updates["visibility"] = visibility
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