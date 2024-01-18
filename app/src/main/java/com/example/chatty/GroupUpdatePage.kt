package com.example.chatty

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import java.util.UUID

class GroupUpdatePage : AppCompatActivity() {
    private var uid = FirebaseAuth.getInstance().uid

    private lateinit var editProfilePhoto: ImageView

    private lateinit var nameEditText: EditText
    private lateinit var aboutEditText: EditText

    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button

    private var about: String? = null
    private var name: String? = null
    private var oldImage: String? = null
    private var newImage: Uri? = null

    private lateinit var addMembers: RecyclerView
    private val groupAdapter = GroupAdapter<GroupieViewHolder>()

    private var members : MutableList<String>? = null

    private var newMembers = mutableListOf<String>()

    private val databaseRef = FirebaseDatabase.getInstance()

    private var clicked = false

    private var updates = hashMapOf<String, Any>()

    private var groupId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        val isDarkTheme = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            .getBoolean("DARK_THEME", false)

        if (isDarkTheme) {
            setTheme(R.style.Theme_Chatty_Dark)  // Önceden tanımlanmış karanlık tema
        } else {
            setTheme(R.style.Theme_Chatty_Light)  // Önceden tanımlanmış aydınlık tema
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.group_update_page)

        groupId = intent.getStringExtra("GROUP_ID").toString()


        databaseRef.getReference("/GroupChats/$groupId")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(!snapshot.exists()){
                        startActivity(Intent(this@GroupUpdatePage, MainPage::class.java))
                        finishAffinity()
                    }

                    val genericType = object : GenericTypeIndicator<HashMap< String,String>>() {}
                    members = snapshot.child("/members").getValue(genericType)?.values?.toMutableList()
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        databaseRef.getReference("/GroupChats/$groupId")
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(!snapshot.exists()){
                        startActivity(Intent(this@GroupUpdatePage, MainPage::class.java))
                        finishAffinity()
                    }

                    oldImage = snapshot.child("groupPhoto").getValue(String::class.java)
                    if(oldImage != "")
                        Picasso.get().load(oldImage).into(editProfilePhoto)     // Replace imageView with your ImageView reference

                    name = snapshot.child("name").getValue(String::class.java)
                    nameEditText.setText(name)
                    about = snapshot.child("about").getValue(String::class.java)
                    aboutEditText.setText(about)

                    fetchFriends()
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        nameEditText = findViewById(R.id.nameEditText)
        aboutEditText = findViewById(R.id.aboutEditText)

        confirmButton = findViewById(R.id.confirmButton)
        cancelButton = findViewById(R.id.cancelButton)

        editProfilePhoto = findViewById(R.id.imageEdit)

        addMembers = findViewById(R.id.membersRecyclerview)
        addMembers.adapter = groupAdapter

        editProfilePhoto.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        cancelButton.setOnClickListener{
            if(!clicked){
                finish()
            }
        }

        confirmButton.setOnClickListener{
            if(!clicked){
                updates["name"] = nameEditText.text.toString().trim()
                updates["about"] = aboutEditText.text.toString().trim()

                if(updates["name"].toString().isEmpty())
                    showToast("You can't leave group name empty")
                else {
                    clicked = true
                    saveNewImage()
                }
            }
            saveNewImage()
        }

        groupAdapter.setOnItemClickListener { item, view ->
            val userItem = item as GroupUserItem
            if (!userItem.selected) {
                userItem.selected = true
                newMembers.add(userItem.chat.id)

                view.findViewById<ConstraintLayout>(R.id.chat_row_background).setBackgroundColor(
                    Color.parseColor("#504F4F")
                )
                view.findViewById<TextView>(R.id.username_newfriend_row).setTextColor(Color.WHITE)
            } else {
                userItem.selected = false
                newMembers.remove(userItem.chat.id)
                view.findViewById<ConstraintLayout>(R.id.chat_row_background).setBackgroundColor(
                    Color.parseColor("#e6e3e3")
                )
                view.findViewById<TextView>(R.id.username_newfriend_row).setTextColor(Color.BLACK)
            }
        }
    }

    private fun fetchFriends(){
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid/chats")
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot){
                groupAdapter.clear()
                newMembers = mutableListOf()
                snapshot.children.forEach {
                    if(it.exists() && !it.child("group").exists()) {
                        val userId = it.key

                        if (!members!!.contains(userId)) {
                            FirebaseDatabase.getInstance().getReference("/users/${userId}")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
                                            val chat = Chat(userId!!, false)
                                            chat.name = snapshot.child("username")
                                                .getValue(String::class.java).toString()
                                            chat.photoURI = snapshot.child("profilePhoto")
                                                .getValue(String::class.java)
                                            groupAdapter.add(GroupUserItem(chat))
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {

                                    }
                                })

                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode==0 && resultCode== Activity.RESULT_OK && data!=null){
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
                    if(oldImage != "" && oldImage!=null){
                        FirebaseStorage.getInstance().getReferenceFromUrl(oldImage!!).delete()
                    }
                    saveUpdates(it.toString())
                }
            }.addOnFailureListener {
                showToast("Failed to save the photo: ${it.message}")
            }
        }
    }

    private fun saveUpdates(profileImageUri: String){

        //group.members.putAll(members)
         if(profileImageUri!="") {
            databaseRef.getReference("/GroupChats/$groupId/groupPhoto").setValue(profileImageUri)
        }
        if(name!=updates["name"])
            databaseRef.getReference("/GroupChats/$groupId/name").setValue(updates["name"])
        if(about!=updates["about"])
            databaseRef.getReference("/GroupChats/$groupId/about").setValue(updates["about"])
        if(newMembers.size != 0) {
            val time = Timestamp.now().seconds
            for(mem in newMembers.toList()){
                databaseRef.getReference("/GroupChats/$groupId/members").push().setValue(mem)
                databaseRef.getReference("/GroupChats/$groupId/prevMembers/$mem")
                    .addListenerForSingleValueEvent(object: ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if(snapshot.exists())
                                snapshot.ref.removeValue()
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                FirebaseDatabase.getInstance()
                    .getReference("/users/$mem/chats/$groupId/id")
                    .setValue(groupId)
                FirebaseDatabase.getInstance()
                    .getReference("/users/$mem/chats/$groupId/time")
                    .setValue(time)
                FirebaseDatabase.getInstance()
                    .getReference("/users/$mem/chats/$groupId/group")
                    .setValue(true)
            }
            newMembers.clear()
        }
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}