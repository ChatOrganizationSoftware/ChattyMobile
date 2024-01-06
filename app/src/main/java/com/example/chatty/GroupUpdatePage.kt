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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import java.util.UUID

class GroupUpdatePage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var editProfilePhoto: ImageView

    private lateinit var nameEditText: EditText
    private lateinit var aboutEditText: EditText

    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button

    private lateinit var oldImage: String
    private var newImage: Uri? = null

    private lateinit var group: Group

    private lateinit var addMembers: RecyclerView
    private val groupAdapter = GroupAdapter<GroupieViewHolder>()

    private var members = hashMapOf<String, Boolean>()

    private val databaseRef = FirebaseDatabase.getInstance()

    private var clicked = false

    private var updates = hashMapOf<String, Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.group_update_page)

        val groupId = intent.getStringExtra("GROUP_ID").toString()

        databaseRef.getReference("/GroupChats/$groupId")
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    group = snapshot.getValue(Group::class.java)!!
                    oldImage = group.groupPhoto

                    if(oldImage != "")
                        Picasso.get().load(oldImage).into(editProfilePhoto)     // Replace imageView with your ImageView reference

                    // Update TextViews with user information
                    nameEditText.setText(group.name)
                    aboutEditText.setText(group.about)

                    fetchFriends()
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        nameEditText = findViewById(R.id.nameEditText)
        aboutEditText = findViewById(R.id.aboutEditText)

        confirmButton = findViewById(R.id.confirmButton)
        cancelButton = findViewById(R.id.cancelButton)

        editProfilePhoto = findViewById(R.id.imageEdit)

        addMembers = findViewById(R.id.membersRecyclerview)
        addMembers.adapter = groupAdapter

        auth = FirebaseAuth.getInstance()

        editProfilePhoto.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        cancelButton.setOnClickListener{
            if(!clicked){
                //val intent = Intent(this, GroupAdminProfilePage::class.java)
                //intent.putExtra("GROUP_ID", group.groupId)
                //startActivity(intent)
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
    }

    private fun fetchFriends(){
        val ref = FirebaseDatabase.getInstance().getReference("/users/${FirebaseAuth.getInstance().uid}/friends")
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot){
                groupAdapter.clear()
                snapshot.children.forEach {
                    val userId = it.key

                    if (!group.members.containsKey(userId) || !group.members[userId]!!) {
                        FirebaseDatabase.getInstance().getReference("/users/${userId}")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val user = snapshot.getValue(User::class.java)
                                    if (user != null) {
                                        groupAdapter.add(GroupUserItem(user))
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {

                                }
                            })

                    }
                }

                groupAdapter.setOnItemClickListener { item, view ->
                    val userItem = item as GroupUserItem
                    if(!userItem.selected){
                        userItem.selected = true
                        members.put(userItem.user.userId, true)
                        view.findViewById<ConstraintLayout>(R.id.chat_row_background).setBackgroundColor(
                            Color.parseColor("#504F4F"))
                        view.findViewById<TextView>(R.id.username_newfriend_row).setTextColor(Color.WHITE)
                    }
                    else{
                        userItem.selected = false
                        members.remove(userItem.user.userId)
                        view.findViewById<ConstraintLayout>(R.id.chat_row_background).setBackgroundColor(
                            Color.parseColor("#e6e3e3"))
                        view.findViewById<TextView>(R.id.username_newfriend_row).setTextColor(Color.BLACK)
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

        group.members.putAll(members)
         if(profileImageUri!="") {
            databaseRef.getReference("/GroupChats/${group.groupId}/groupPhoto").setValue(profileImageUri)
        }


        databaseRef.getReference("/GroupChats/${group.groupId}/name").setValue(updates["name"])
        databaseRef.getReference("/GroupChats/${group.groupId}/about").setValue(updates["about"])
        databaseRef.getReference("/GroupChats/${group.groupId}/members").setValue(group.members)
            .addOnCompleteListener {
                if(members.keys.size != 0) {
                    val time = Timestamp.now()
                    for (member in members.keys) {
                        showToast(member)
                        FirebaseDatabase.getInstance()
                            .getReference("/users/${member}/chats/${group.groupId}/id")
                            .setValue(group.groupId)
                        FirebaseDatabase.getInstance()
                            .getReference("/users/${member}/chats/${group.groupId}/time")
                            .setValue(time)
                        FirebaseDatabase.getInstance()
                            .getReference("/users/${member}/chats/${group.groupId}/group")
                            .setValue(true)

                        //val intent = Intent(this, GroupAdminProfilePage::class.java)
                        //intent.putExtra("GROUP_ID", group.groupId)
                        //startActivity(intent)
                    }
                }
                finish()
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}