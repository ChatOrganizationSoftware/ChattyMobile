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
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import de.hdodenhof.circleimageview.CircleImageView

class GroupAdminProfilePage : AppCompatActivity() {
    private lateinit var membersRecyclerView: RecyclerView
    private lateinit var nameField: TextView
    private lateinit var aboutField: TextView
    private lateinit var closeGroupButton: Button
    private val groupAdapter = GroupAdapter<GroupieViewHolder>()
    private var databaseRef = FirebaseDatabase.getInstance()
    private lateinit var adminView: RecyclerView
    private var adminAdapter = GroupAdapter<GroupieViewHolder>()
    private var members : MutableList<String>? = null
    private val uid = FirebaseAuth.getInstance().uid
    private var username : String? = null
    private lateinit var groupId: String

    override fun onCreate(savedInstanceState: Bundle?) {

        val isDarkTheme = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            .getBoolean("DARK_THEME", false)

        if (isDarkTheme) {
            setTheme(R.style.Theme_Chatty_Dark)  // Önceden tanımlanmış karanlık tema
        } else {
            setTheme(R.style.Theme_Chatty_Light)  // Önceden tanımlanmış aydınlık tema
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.group_admin_profile_page)

        groupId = intent.getStringExtra("GROUP_ID")!!

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
        databaseRef.getReference("/GroupChats/${groupId}")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(!snapshot.exists()) {
                        startActivity(Intent(this@GroupAdminProfilePage, MainPage::class.java))
                        finishAffinity()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle any errors that occur while fetching data
                }
            })

        databaseRef.getReference("/users/$uid/username")
            .addValueEventListener(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(!snapshot.exists()){
                        FirebaseAuth.getInstance().signOut()
                        startActivity(Intent(this@GroupAdminProfilePage, LoginPage::class.java))
                        finishAffinity()
                    }
                    else{
                        username = snapshot.getValue(String::class.java)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        // Gets the group information from the firebase
        databaseRef.getReference("/GroupChats/${groupId}/name")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        nameField.text = snapshot.getValue(String::class.java)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        databaseRef.getReference("/GroupChats/${groupId}/groupPhoto")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val image = snapshot.getValue(String::class.java)
                        if(image != "")
                            Picasso.get().load(image).into(findViewById<CircleImageView>(R.id.friend_profile_photo))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        databaseRef.getReference("/GroupChats/${groupId}/about")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        aboutField.text = snapshot.getValue(String::class.java)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        databaseRef.getReference("/GroupChats/$groupId/members")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.childrenCount.toInt() == 1){
                        databaseRef.getReference("/users/$uid/chats/$groupId").removeValue().addOnSuccessListener {
                            databaseRef.getReference("/GroupChats/$groupId").removeValue()
                            startActivity(Intent(this@GroupAdminProfilePage, MainPage::class.java))

                            finishAffinity()
                        }
                    }
                    val genericType = object : GenericTypeIndicator<HashMap< String,String>>() {}
                    members = snapshot.getValue(genericType)?.values?.toMutableList()
                    if(members != null) {
                        if (!members!!.contains(uid)){
                            startActivity(Intent(this@GroupAdminProfilePage, MainPage::class.java))
                            finishAffinity()
                        }

                        fetchMembers()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        closeGroupButton.setOnClickListener {
            members!!.remove(uid)

            for (member in members!!) {
                databaseRef.getReference("/users/$member/chats/$groupId").removeValue()
                databaseRef.getReference("/users/$member/notifications").push().setValue("{${nameField.text}} is closed.")
            }

            databaseRef.getReference("/users/$uid/chats/$groupId").removeValue().addOnSuccessListener {
                databaseRef.getReference("/GroupChats/$groupId").removeValue()
                startActivity(Intent(this, MainPage::class.java))

                finishAffinity()
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun fetchMembers(){
        groupAdapter.clear()
        for(member in members!!) {
                databaseRef.getReference("/users/$member")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if(snapshot.exists()) {
                                val user = snapshot.getValue(User::class.java)!!
                                if (user.userId == uid) {
                                    if (adminView.childCount == 0) {
                                        adminAdapter.add(GroupMemberItem(user))
                                    }
                                } else {
                                    groupAdapter.add(
                                        GroupAdminMemberItem(
                                            groupId,
                                            nameField.text.toString(),
                                            user
                                        )
                                    )
                                }
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
                intent.putExtra("GROUP_ID", groupId)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    class GroupAdminMemberItem(val groupId: String, val name: String, val user: User): Item<GroupieViewHolder>(){
        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.findViewById<TextView>(R.id.username_newfriend_row).text = user.username
            viewHolder.itemView.findViewById<TextView>(R.id.visibility_newfriend_row).text = user.visibility

            viewHolder.itemView.findViewById<ImageView>(R.id.removeMember).setOnClickListener {
                FirebaseDatabase.getInstance().getReference("/GroupChats/$groupId/members").orderByValue().equalTo(user.userId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (childSnapshot in snapshot.children) {
                            // Remove the user from the members list
                            childSnapshot.ref.removeValue()
                            FirebaseDatabase.getInstance().getReference("/GroupChats/$groupId/prevMembers/${user.userId}").setValue(user.username)
                            FirebaseDatabase.getInstance().getReference("/GroupChats/$groupId/members/${user.userId}").removeValue()
                            FirebaseDatabase.getInstance().getReference("/users/${user.userId}/chats/$groupId").removeValue()
                            FirebaseDatabase.getInstance().getReference("/users/${user.userId}/notifications").push().setValue("Your are removed from the group {$name}.")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle the error
                    }
                })
            }
            if(user.profilePhoto!="")
                Picasso.get().load(user.profilePhoto).into(viewHolder.itemView.findViewById<CircleImageView>(R.id.image_newfriend_row))
        }

        override fun getLayout(): Int {
            return R.layout.group_admin_member_row
        }
    }
}
