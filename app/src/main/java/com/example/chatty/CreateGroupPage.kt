package com.example.chatty

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import de.hdodenhof.circleimageview.CircleImageView
import java.util.UUID

class CreateGroupPage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var editProfilePhoto: ImageView

    private lateinit var nameEditText: EditText
    private lateinit var aboutEditText: EditText

    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button

    private lateinit var usersList: RecyclerView
    private var groupAdapter = GroupAdapter<GroupieViewHolder>()

    private var newImage: Uri? = null

    private var members = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_group_page)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create Group"

        nameEditText = findViewById(R.id.nameEditText)
        aboutEditText = findViewById(R.id.aboutEditText)

        confirmButton = findViewById(R.id.confirmButton)
        cancelButton = findViewById(R.id.cancelButton)

        editProfilePhoto = findViewById(R.id.groupProfilePhoto)

        usersList = findViewById(R.id.recyclerviewUsers)

        usersList.adapter = groupAdapter

        auth = FirebaseAuth.getInstance()

        editProfilePhoto.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        fetchFriends()

        cancelButton.setOnClickListener{
            finish()
        }

        confirmButton.setOnClickListener{
            if(members.size==0){
                showToast("Error: You must select at least 1 member for a group")
            }
            else if(nameEditText.text.toString()==""){
                showToast("Error: You should provide a group name")
            }
            else{
                saveNewImage()
            }

        }
    }

    private fun fetchFriends(){
        val ref = FirebaseDatabase.getInstance().getReference("/users/${FirebaseAuth.getInstance().uid}/friends")
        ref.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot){
                snapshot.children.forEach{
                    val userId = it.getValue(String::class.java)

                    FirebaseDatabase.getInstance().getReference("/users/${userId}").addValueEventListener(object: ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val user = snapshot.getValue(User::class.java)
                            if (user != null){
                                groupAdapter.add(GroupUserItem(user))
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    })

                }

                groupAdapter.setOnItemClickListener { item, view ->
                    val userItem = item as GroupUserItem
                    if(!userItem.selected){
                        userItem.selected = true
                        members.add(userItem.user.userId)
                        view.findViewById<ConstraintLayout>(R.id.chat_row_background).setBackgroundColor(Color.parseColor("#504F4F"))
                        view.findViewById<TextView>(R.id.username_newfriend_row).setTextColor(Color.WHITE)
                    }
                    else{
                        userItem.selected = false
                        members.remove(userItem.user.userId)
                        view.findViewById<ConstraintLayout>(R.id.chat_row_background).setBackgroundColor(Color.parseColor("#e6e3e3"))
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
            createGroup("")
        else {
            val filename = UUID.randomUUID().toString()
            val ref = FirebaseStorage.getInstance().getReference("/Profile Photos/${filename}")

            ref.putFile(newImage!!).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener {
                    createGroup(it.toString())
                }
            }.addOnFailureListener {
                showToast("Failed to save the photo: ${it.message}")
            }
        }
    }

    private fun createGroup(profileImageUri: String){
        val groupId = UUID.randomUUID().toString()
        val ref = FirebaseDatabase.getInstance().getReference("/GroupChats/${groupId}")
        FirebaseAuth.getInstance().uid?.let { members.add(it) }
        var group = Group(groupId, auth.uid.toString(), nameEditText.text.toString(), profileImageUri, aboutEditText.text.toString(), members)

        if(profileImageUri!="") {
            group.groupPhoto = profileImageUri
        }

        ref.setValue(group).addOnSuccessListener {
            val time = Timestamp.now()
            for(member in members){
                FirebaseDatabase.getInstance().getReference("/users/${member}/chats/${groupId}/id").setValue(groupId)
                FirebaseDatabase.getInstance().getReference("/users/${member}/chats/${groupId}/time").setValue(time)
                FirebaseDatabase.getInstance().getReference("/users/${member}/chats/${groupId}/group").setValue(true)
            }

            finish()
        }.addOnFailureListener{
            showToast("Failed to store the data: ${it.message}")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.friend_profile_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

class GroupUserItem(val user: User): Item<GroupieViewHolder>(){
    var selected = false
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.username_newfriend_row).text = user.username
        if(user.profilePhoto!="")
            Picasso.get().load(user.profilePhoto).into(viewHolder.itemView.findViewById<CircleImageView>(R.id.image_newfriend_row))
    }


    override fun getLayout(): Int {
        return R.layout.chat_row // Use the normal layout
    }
}