package com.example.chatty

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class DisplayImageGroupPage : AppCompatActivity() {
    private var selectedPhotoURI: String? = null
    private var group: Group? = null
    private lateinit var photo: ImageView
    private lateinit var declineSend: ImageView
    private lateinit var confirmSend: ImageView
    var clicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.display_image_group_page)

        selectedPhotoURI = intent.getStringExtra("PHOTO_SELECTED")
        group = intent.getParcelableExtra("GROUP")

        photo = findViewById(R.id.selectedImage)
        declineSend = findViewById(R.id.declineSend)
        confirmSend = findViewById(R.id.confirmSend)

        if(selectedPhotoURI == null || selectedPhotoURI == ""){
            finish()
        }
        else{
            photo.setImageURI(Uri.parse(selectedPhotoURI))
        }

        declineSend.setOnClickListener{
            if(!clicked)
                finish()
        }

        confirmSend.setOnClickListener {
            if(!clicked) {
                clicked = true
                uploadImagetoFirebase()
            }
        }
    }

    private fun uploadImagetoFirebase(){
        if(selectedPhotoURI == null)
            finish()
        else {
            val filename = UUID.randomUUID().toString()
            val ref = FirebaseStorage.getInstance().getReference("/${group?.groupId}/${filename}")

            ref.putFile(Uri.parse(selectedPhotoURI)).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener {
                    saveMessagetoFirebase(it.toString())
                }
            }
        }
    }

    private fun saveMessagetoFirebase(imageUri: String){
        val ref = FirebaseDatabase.getInstance().getReference("/GroupChats/${group?.groupId}/Messages").push()
        val message = IndividualMessage( ref.key!!, null, imageUri, FirebaseAuth.getInstance().uid!!,)
        ref.setValue(message).addOnSuccessListener {
            val time = Timestamp.now().seconds
            for(member in group?.members!!)
                FirebaseDatabase.getInstance().getReference("/users/${member}/chats/${group?.groupId}/time").setValue(time)
            finish()
        }
    }
}