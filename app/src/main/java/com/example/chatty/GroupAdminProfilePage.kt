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
                group.name = snapshot.child("name").getValue(String::class.java).toString()
                group.groupId = snapshot.child("groupId").getValue(String::class.java).toString()
                group.groupPhoto =snapshot.child("groupPhoto").getValue(String::class.java).toString()
                if(group.groupPhoto != "")
                    Picasso.get().load(group.groupPhoto).into(findViewById<CircleImageView>(R.id.friend_profile_photo))

                group.about = snapshot.child("about").getValue(String::class.java).toString()
                nameField.text = group.name
                aboutField.text = group.about

                group.members = mutableListOf()
                group.admins = mutableListOf()
                for(mem in snapshot.child("members").children)
                    mem.getValue(String::class.java)?.let { group.members.add(it) }
                for(admin in snapshot.child("admins").children)
                    admin.getValue(String::class.java)?.let { group.admins.add(it) }

                fetchMembers()

            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors that occur while fetching data
            }
        })

        leaveGroupButton.setOnClickListener {

        }

        closeGroupButton.setOnClickListener {
            databaseRef.getReference("/users/${uid}/username")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val adminName = snapshot.getValue(String::class.java)
                        for (member in group.members) {
                            if(member != uid) {
                                databaseRef.getReference("/users/${member}/chats/${group.groupId}")
                                    .removeValue()

                                val not = databaseRef.getReference("/users/${member}/notifications").push()
                                not.setValue("$adminName has closed the group {${group.name}}.")
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })

            databaseRef.getReference("/GroupChats/${group.groupId}").removeValue()
            FirebaseStorage.getInstance().getReference("/Profile Photos/${group.groupId}").delete()
            databaseRef.getReference("/users/${uid}/chats/${group.groupId}").removeValue().addOnSuccessListener {
                val intent = Intent(this, MainPage::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun fetchMembers(){
        members = mutableListOf()
        for(member in group.members){
            databaseRef.getReference("/users/${member}")
                .addValueEventListener(object : ValueEventListener {
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
        menuInflater.inflate(R.menu.friend_profile_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> {
                val intent = Intent(this@GroupAdminProfilePage, GroupChatPage::class.java)
                intent.putExtra(NewFriendsPage.USER_KEY, group.groupId)
                startActivity(intent)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}