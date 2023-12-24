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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import de.hdodenhof.circleimageview.CircleImageView

class GroupProfilePage : AppCompatActivity() {
    private lateinit var group: Group
    private lateinit var members: HashMap<String, User>
    private lateinit var membersRecyclerView: RecyclerView
    private lateinit var nameField: TextView
    private lateinit var aboutField: TextView
    private lateinit var leaveGroupButton: Button
    private val groupAdapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.group_profile_page)

        group = intent.getParcelableExtra<Group>("Group")!!
        val bundle = intent.getBundleExtra("Members")!!
        members = bundle.getSerializable("memberTable") as HashMap<String, User>

        val toolbar = findViewById<Toolbar>(R.id.toolbar2)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        nameField = findViewById(R.id.nameField)
        aboutField = findViewById(R.id.aboutField)
        leaveGroupButton = findViewById(R.id.leaveGroupButton)
        membersRecyclerView = findViewById(R.id.membersRecyclerview)
        membersRecyclerView.adapter = groupAdapter

        // Gests the group information from the firebase
        val database = FirebaseDatabase.getInstance().getReference("/GroupChats/${group.groupId}")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Parse the user data from snapshot and update the UI
                group = snapshot.getValue(Group::class.java)!!
                nameField.text = group.name
                aboutField.text = group.about

                fetchMembers()

                val image = group.groupPhoto
                if(image != "") {
                    Picasso.get().load(image).into(findViewById<CircleImageView>(R.id.friend_profile_photo))
                }

            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors that occur while fetching data
            }
        })

        leaveGroupButton.setOnClickListener {

        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun fetchMembers(){
        for(member in group.members){
            FirebaseDatabase.getInstance().getReference("/users/${member}")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Parse the user data from snapshot and update the UI
                        val user = snapshot.getValue(User::class.java)!!
                        groupAdapter.add(GroupMemberItem(user))
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