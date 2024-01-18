package com.example.chatty

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Base64
import java.util.UUID
import javax.crypto.KeyGenerator

class CreateGroupPage : AppCompatActivity() {

    private lateinit var editProfilePhoto: ImageView

    private lateinit var nameEditText: EditText
    private lateinit var aboutEditText: EditText
    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button
    private lateinit var usersList: RecyclerView
    private var groupAdapter = GroupAdapter<GroupieViewHolder>()
    private var newImage: Uri? = null
    private var members = hashMapOf<String, Boolean>()
    private var clicked = false
    private var group = Group()
    private var uid = FirebaseAuth.getInstance().uid

    private val databaseRef = FirebaseDatabase.getInstance()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        val isDarkTheme = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            .getBoolean("DARK_THEME", false)

        if (isDarkTheme) {
            setTheme(R.style.Theme_Chatty_Dark)  // Önceden tanımlanmış karanlık tema
        } else {
            setTheme(R.style.Theme_Chatty_Light)  // Önceden tanımlanmış aydınlık tema
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_group_page)

        members.put(uid!!, true)

        databaseRef.getReference("/users/$uid")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(!snapshot.exists()){
                        startActivity(Intent(this@CreateGroupPage, LoginPage::class.java))

                        finishAffinity()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        // Toolbar above the screen
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

        // Click listener to select photo for the group
        editProfilePhoto.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        // Fetch friends to select who to add the group
        fetchFriends()

        // Cancel the group creation
        cancelButton.setOnClickListener{
            if(!clicked) {
                clicked = true
                finish()
            }
        }

        // Create the group
        confirmButton.setOnClickListener{
            if(!clicked) {
                clicked = true
                group.name = nameEditText.text.toString().trim()
                group.about = aboutEditText.text.toString().trim()

                if (members.size == 0) {
                    showToast("Error: You must select at least 1 member for a group")
                    clicked = false
                }
                else if (group.name.isEmpty()) {
                    showToast("Error: You should provide a group name")
                    clicked = false
                }
                else
                    saveNewImage()          // Start creating the group with saving the group image
            }
        }

        groupAdapter.setOnItemClickListener { item, view ->
            if(!clicked) {
                val userItem = item as GroupUserItem
                if (!userItem.selected) {
                    userItem.selected = true
                    members.put(userItem.chat.id, true)
                    view.findViewById<ConstraintLayout>(R.id.chat_row_background)
                        .setBackgroundColor(Color.parseColor("#504F4F"))
                    view.findViewById<TextView>(R.id.username_newfriend_row)
                        .setTextColor(Color.WHITE)
                } else {
                    userItem.selected = false
                    members.remove(userItem.chat.id)
                    view.findViewById<ConstraintLayout>(R.id.chat_row_background)
                        .setBackgroundColor(Color.parseColor("#e6e3e3"))
                    view.findViewById<TextView>(R.id.username_newfriend_row)
                        .setTextColor(Color.BLACK)
                }
            }
        }
    }

    // Fetch friends to select the ones added to group
    private fun fetchFriends(){
        databaseRef.getReference("/users/${FirebaseAuth.getInstance().uid}/chats")
            .addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot){
                if(snapshot.exists()) {
                    snapshot.children.forEach {
                        if(it.exists() && !it.child("group").exists()) {
                            val userId = it.key

                            databaseRef.getReference("/users/${userId}")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val chat = Chat(userId!!, false)
                                        chat.name = snapshot.child("username").getValue(String::class.java).toString()
                                        chat.photoURI = snapshot.child("profilePhoto")
                                            .getValue(String::class.java)
                                        groupAdapter.add(GroupUserItem(chat))
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

    // About selecting the image from device gallery
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode==0 && resultCode== Activity.RESULT_OK && data!=null){
            newImage = data.data

            editProfilePhoto.background = null
            editProfilePhoto.setImageURI(newImage)
        }
    }

    // Stores the group image in the Firebase Storage
    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveNewImage(){
        val groupId = UUID.randomUUID().toString()
        if(newImage == null)            // If the user didn't select any image
            createGroup("", groupId)
        else {
            val ref = FirebaseStorage.getInstance().getReference("/Profile Photos/$groupId")

            ref.putFile(newImage!!).addOnCompleteListener{
                ref.downloadUrl.addOnCompleteListener {
                    createGroup(it.toString(), groupId)
                }
            }.addOnFailureListener {
                showToast("Failed to save the photo: ${it.message}")
            }
        }
    }

    // Creates the group with the given image URI
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createGroup(profileImageUri: String, groupId: String){
        group.groupId = groupId
        group.adminId = uid!!
        group.groupPhoto = profileImageUri
        group.key = generateKey()
        // Add the group to all the members' chat list
        databaseRef.getReference("/GroupChats/${groupId}").setValue(group).addOnCompleteListener {
            val time = Timestamp.now().seconds
            for(member in members.keys){
                databaseRef.getReference("/GroupChats/$groupId/members").push().setValue(member)
                if(member != group.adminId) {
                    val chat = Chat(groupId, true, time)
                    chat.read = false
                    databaseRef.getReference("/users/$member/chats/${chat.id}").setValue(chat)
                }
            }
            val chat = Chat(groupId, true, time)
            databaseRef.getReference("/users/${group.adminId}/chats/${chat.id}").setValue(chat)
                .addOnCompleteListener {
                    val intent = Intent(this, GroupChatPage::class.java)
                    intent.putExtra("GROUP_ID", groupId)
                    startActivity(intent)
                    finish()
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateKey(): String {
        val key = KeyGenerator.getInstance("AES").generateKey()
        return Base64.getEncoder().encodeToString(key.encoded)
    }

    // Inserting menu onto toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.friend_profile_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // Handling menu options
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> {
                if(!clicked)
                    finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

// Class to display the users for adding users to group
class GroupUserItem(val chat: Chat): Item<GroupieViewHolder>(){
    var selected = false
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.username_newfriend_row).text = chat.name
        if(chat.photoURI!="")
            Picasso.get().load(chat.photoURI).into(viewHolder.itemView.findViewById<CircleImageView>(R.id.image_newfriend_row))
    }


    override fun getLayout(): Int {
        return R.layout.chat_row // Use the normal layout
    }
}