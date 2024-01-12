package com.example.chatty

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

class GroupProfilePage : AppCompatActivity() {
    private var group = Group()
    private lateinit var membersRecyclerView: RecyclerView
    private lateinit var nameField: TextView
    private lateinit var aboutField: TextView
    private lateinit var leaveGroupButton: Button
    private val groupAdapter = GroupAdapter<GroupieViewHolder>()
    private val databaseRef = FirebaseDatabase.getInstance()
    private lateinit var adminView: RecyclerView
    private var adminAdapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {

        val isDarkTheme = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            .getBoolean("DARK_THEME", false)

        if (isDarkTheme) {
            setTheme(R.style.Theme_Chatty_Dark)  // Önceden tanımlanmış karanlık tema
        } else {
            setTheme(R.style.Theme_Chatty_Light)  // Önceden tanımlanmış aydınlık tema
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.group_profile_page)

        val groupId = intent.getStringExtra("GROUP_ID")!!

        val toolbar = findViewById<Toolbar>(R.id.toolbar2)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        nameField = findViewById(R.id.nameField)
        aboutField = findViewById(R.id.aboutField)
        leaveGroupButton = findViewById(R.id.leaveGroupButton)
        membersRecyclerView = findViewById(R.id.membersRecyclerview)
        membersRecyclerView.adapter = groupAdapter

        adminView = findViewById(R.id.adminView)
        adminView.adapter = adminAdapter

        // Gets the group information from the firebase
        val database = databaseRef.getReference("/GroupChats/${groupId}")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.child("deleted").exists())
                    finish()
                // Parse the user data from snapshot and update the UI
                group = snapshot.getValue(Group::class.java)!!

                if(!group.members[FirebaseAuth.getInstance().uid]!!)
                    finish()

                var i = 0
                for(mem in group.members.keys){
                    databaseRef.getReference("/users/$mem/username")
                        .addListenerForSingleValueEvent(object : ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                group.currentMembers.put(mem, snapshot.getValue(String::class.java)!!)
                                i++
                                if(i == group.members.keys.size){
                                    fetchMembers()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                            }
                        })
                }
                if(group.groupPhoto != "")
                    Picasso.get().load(group.groupPhoto).into(findViewById<CircleImageView>(R.id.friend_profile_photo))

                nameField.text = group.name
                aboutField.text = group.about

            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors that occur while fetching data
            }
        })

        leaveGroupButton.setOnClickListener {
            val uid = FirebaseAuth.getInstance().uid
            for(mem in group.members){
                if(!mem.value)
                    group.members.remove(mem.key)
            }
            databaseRef.getReference("/users/$uid")
                .addListenerForSingleValueEvent(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        group.members.remove(uid)
                        if(group.members.size <= 1){
                            FirebaseStorage.getInstance().getReference("/Profile Photos/$groupId").delete()
                            for(mem in group.members.keys){
                                if(group.members[mem] == true) {
                                    databaseRef.getReference("/users/$mem/chats/$groupId")
                                        .removeValue()
                                        .addOnSuccessListener {
                                            databaseRef.getReference("/users/$mem/notifications")
                                                .push()
                                                .setValue("{${group.name}} is closed.")
                                        }
                                }
                            }

                            databaseRef.getReference("/GroupChats/$groupId/deleted").setValue(true)
                                .addOnCompleteListener {
                                    databaseRef.getReference("/users/$uid/chats/$groupId").removeValue().addOnSuccessListener {
                                        finish()
                                    }
                                }

                        }
                        else {
                            val username = snapshot.child("username").getValue(String::class.java)
                            databaseRef.getReference("/GroupChats/$groupId/members/$uid")
                                .removeValue()
                            databaseRef.getReference("/GroupChats/$groupId/prevMembers/$uid")
                                .setValue(username)
                            databaseRef.getReference("/users/$uid/chats/$groupId").removeValue()
                                .addOnSuccessListener {
                                    finish()
                                }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun fetchMembers(){
        groupAdapter.clear()
        for(member in group.members) {
            if (member.value) {
                databaseRef.getReference("/users/${member.key}")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            // Parse the user data from snapshot and update the UI
                            val user = snapshot.getValue(User::class.java)!!
                            if(user.userId == group.adminId){
                                if(adminView.childCount == 0){
                                    adminAdapter.add(GroupMemberItem(user))
                                }
                            }
                            else {
                                groupAdapter.add(GroupMemberItem(user))
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        // Handle any errors that occur while fetching data
                        }
                    })
            }
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
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

// Class to display the group members
class GroupMemberItem(val user: User): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.username_newfriend_row).text = user.username
        viewHolder.itemView.findViewById<TextView>(R.id.visibility_newfriend_row).text = user.visibility
        if(user.profilePhoto!="")
            Picasso.get().load(user.profilePhoto).into(viewHolder.itemView.findViewById<CircleImageView>(R.id.image_newfriend_row))
    }

    override fun getLayout(): Int {
        return R.layout.group_member_row
    }
}