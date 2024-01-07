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

class DisplayImagePage : AppCompatActivity() {
    private var selectedPhotoURI: String? = null
    private var chatId: String? = null
    private var friendId: String? = null
    private lateinit var photo: ImageView
    private lateinit var declineSend: ImageView
    private lateinit var confirmSend: ImageView
    private var clicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.display_image_page)
        selectedPhotoURI = intent.getStringExtra("PHOTO_SELECTED")
        chatId = intent.getStringExtra("CHAT_ID")
        friendId = intent.getStringExtra("FRIEND_ID")

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
            val ref = FirebaseStorage.getInstance().getReference("/${chatId}/${filename}")

            ref.putFile(Uri.parse(selectedPhotoURI)).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener {
                    saveMessagetoFirebase(it.toString())
                }
            }
        }
    }

    private fun saveMessagetoFirebase(imageUri: String){

        val ref = FirebaseDatabase.getInstance().getReference("/IndividualChats/${chatId}/Messages").push()
        val message = IndividualMessage( ref.key!!, null, imageUri, FirebaseAuth.getInstance().uid!!,)
        ref.setValue(message).addOnCompleteListener {
            val time = Timestamp.now().seconds
            FirebaseDatabase.getInstance().getReference("/users/${FirebaseAuth.getInstance().uid}/chats/${chatId}/time").setValue(time)
            FirebaseDatabase.getInstance().getReference("/users/${friendId}/chats/${chatId}/time").setValue(time)
            FirebaseDatabase.getInstance().getReference("/users/${friendId}/chats/${chatId}/read").setValue(false)
            finish()
        }
    }


}