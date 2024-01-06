package com.example.chatty

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import de.hdodenhof.circleimageview.CircleImageView

class GroupAdminProfilePage : AppCompatActivity() {
    private var group = Group()
    private lateinit var membersRecyclerView: RecyclerView
    private lateinit var nameField: TextView
    private lateinit var aboutField: TextView
    private lateinit var closeGroupButton: Button
    private val groupAdapter = GroupAdapter<GroupieViewHolder>()
    private var databaseRef = FirebaseDatabase.getInstance()
    private var uid = FirebaseAuth.getInstance().uid
    private lateinit var adminView: RecyclerView
    private var adminAdapter = GroupAdapter<GroupieViewHolder>()

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
        closeGroupButton = findViewById(R.id.closeGroupButton)
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
                                TODO("Not yet implemented")
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

        closeGroupButton.setOnClickListener {
            for(mem in group.members){
                if(!mem.value)
                    group.members.remove(mem.key)
            }
            databaseRef.getReference("/users/${uid}/username")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val adminName = snapshot.getValue(String::class.java)
                        for (member in group.members.keys) {
                            if (member != uid && group.members[member] == true) {
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
        groupAdapter.clear()
        for(member in group.members) {
            if (member.value) {
                databaseRef.getReference("/users/${member.key}")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val user = snapshot.getValue(User::class.java)!!
                            if (user.userId == group.adminId) {
                                if (adminView.childCount == 0) {
                                    adminAdapter.add(GroupMemberItem(user))
                                }
                            } else {
                                groupAdapter.add(GroupAdminMemberItem(group, user))
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
            }
        }
        return super.onOptionsItemSelected(item)
    }

    class GroupAdminMemberItem(val group: Group, val user: User): Item<GroupieViewHolder>(){
        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.findViewById<TextView>(R.id.username_newfriend_row).text = user.username
            viewHolder.itemView.findViewById<TextView>(R.id.visibility_newfriend_row).text = user.visibility

            viewHolder.itemView.findViewById<ImageView>(R.id.removeMember).setOnClickListener {
                FirebaseDatabase.getInstance().getReference("/GroupChats/${group.groupId}/members/${user.userId}").setValue(false)
                FirebaseDatabase.getInstance().getReference("/users/${user.userId}/chats/${group.groupId}").removeValue()
                FirebaseDatabase.getInstance().getReference("/users/${user.userId}/notifications").push().setValue("Your are removed from the group {${group.name}}")
            }
            if(user.profilePhoto!="")
                Picasso.get().load(user.profilePhoto).into(viewHolder.itemView.findViewById<CircleImageView>(R.id.image_newfriend_row))
        }

        override fun getLayout(): Int {
            return R.layout.group_admin_member_row
        }
    }
}
