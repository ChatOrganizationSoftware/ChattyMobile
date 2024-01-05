package com.example.chatty

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
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

class GroupAdminProfilePage : AppCompatActivity() {
    private var group = Group()
    private lateinit var members: MutableList<String>
    private lateinit var membersRecyclerView: RecyclerView
    private lateinit var nameField: TextView
    private lateinit var aboutField: TextView
    private lateinit var leaveGroupButton: Button
    private lateinit var closeGroupButton: Button
    private val groupAdapter = GroupAdapter<GroupieViewHolder>()
    private var databaseRef = FirebaseDatabase.getInstance()
    private var uid = FirebaseAuth.getInstance().uid
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.group_admin_profile_page)

        val groupId = intent.getStringExtra("GROUP_ID")!!

        val toolbar = findViewById<Toolbar>(R.id.toolbar2)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        nameField = findViewById(R.id.nameField)
        aboutField = findViewById(R.id.aboutField)
        leaveGroupButton = findViewById(R.id.leaveGroupButton)
        closeGroupButton = findViewById(R.id.closeGroupButton)
        membersRecyclerView = findViewById(R.id.membersRecyclerview)
        membersRecyclerView.adapter = groupAdapter


        // Gets the group information from the firebase
        val database = databaseRef.getReference("/GroupChats/${groupId}")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(!snapshot.exists())
                    finish()
                group = snapshot.getValue(Group::class.java)!!
                if(!group.members.keys.contains(uid))
                    finish()
                if(group.groupPhoto != "")
                    Picasso.get().load(group.groupPhoto).into(findViewById<CircleImageView>(R.id.friend_profile_photo))

                nameField.text = group.name
                aboutField.text = group.about

                fetchMembers()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors that occur while fetching data
            }
        })

        leaveGroupButton.setOnClickListener {
            val uid = FirebaseAuth.getInstance().uid
            databaseRef.getReference("/users/$uid/username")
                .addListenerForSingleValueEvent(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        group.members.remove(uid)
                            if (group.members.keys.size > 1) {
                                val username = snapshot.getValue(String::class.java)
                                databaseRef.getReference("/GroupChats/$groupId/members/$uid")
                                    .removeValue()
                                databaseRef.getReference("/GroupChats/$groupId/prevMembers/$uid")
                                    .setValue(username)
                                databaseRef.getReference("/GroupChats/$groupId/adminId")
                                    .setValue(group.members.keys.sorted()[0])
                                databaseRef.getReference("/users/$uid/chats/$groupId").removeValue().addOnCompleteListener {
                                    val intent =
                                        Intent(this@GroupAdminProfilePage, MainPage::class.java)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                            } else {
                                for (mem in group.members.keys) {
                                    databaseRef.getReference("/users/$mem/chats/$groupId")
                                        .removeValue()
                                    databaseRef.getReference("/users/$mem/notifications").push()
                                        .setValue("{${group.name}} is closed")
                                }
                                databaseRef.getReference("/GroupChats/$groupId/deleted").setValue(true)
                                    .addOnCompleteListener {
                                        databaseRef.getReference("/users/$uid/chats/$groupId").removeValue().addOnSuccessListener {
                                            finish()
                                        }
                                    }
                            }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })
        }

        closeGroupButton.setOnClickListener {
            databaseRef.getReference("/users/${uid}/username")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val adminName = snapshot.getValue(String::class.java)
                        for (member in group.members.keys) {
                            if (member != uid) {
                                databaseRef.getReference("/users/${member}/chats/${group.groupId}").removeValue()
                                databaseRef.getReference("/users/${member}/notifications").push().setValue("$adminName has closed the group {${group.name}}.")
                            }
                        }
                        databaseRef.getReference("/GroupChats/$groupId/deleted").setValue(true)
                            .addOnCompleteListener {
                                databaseRef.getReference("/users/$uid/chats/$groupId").removeValue().addOnSuccessListener {
                                    finish()
                                }
                            }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun fetchMembers(){
        members = mutableListOf()
        for(member in group.members.keys){
            databaseRef.getReference("/users/${member}")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Parse the user data from snapshot and update the UI
                        val user = snapshot.getValue(User::class.java)!!
                        if(!members.contains(user.userId)) {
                            groupAdapter.add(GroupMemberItem(user))
                            members.add(user.userId)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle any errors that occur while fetching data
                    }
                })
        }
        groupAdapter.setOnItemClickListener { item, view ->

        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.profile_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> {
                finish()
            }
            R.id.profile_edit_profile ->{
                val intent = Intent(this, GroupUpdatePage::class.java)
                intent.putExtra("GROUP_ID", group.groupId)
                startActivity(intent)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}