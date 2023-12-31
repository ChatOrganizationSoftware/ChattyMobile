package com.example.chatty

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import de.hdodenhof.circleimageview.CircleImageView
import java.util.UUID

class MainPage : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var createGroupIcon: ImageView
    private var groupAdapter = GroupAdapter<GroupieViewHolder>()
    private lateinit var inputChats: MutableList<String>
    private var databaseRef = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        showToast("MAIN")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_page)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.recyclerviewChats)
        recyclerView.adapter = groupAdapter

        displayChats()

        createGroupIcon = findViewById(R.id.createGroupIcon)
        createGroupIcon.setOnClickListener{
            val intent = Intent(this, CreateGroupPage::class.java)
            startActivity(intent)
        }


        groupAdapter.setOnItemClickListener { item, view ->

            if (item is ChatItem) {
                val chatItem = item as ChatItem
                val chat = chatItem.chat

                val intent = Intent(view.context, FriendChatPage::class.java)
                intent.putExtra(NewFriendsPage.USER_KEY, chat.id)
                startActivity(intent)
            } else {
                val groupItem = item as GroupItem
                val group = groupItem.group

                val intent = Intent(view.context, GroupChatPage::class.java)
                intent.putExtra(NewFriendsPage.USER_KEY, group.groupId)
                startActivity(intent)
            }
        }

    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun displayChats(){
        val ref = databaseRef.getReference("/users/${FirebaseAuth.getInstance().uid}/chats")
        ref.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                groupAdapter.clear()
                val chats = snapshot.children.sortedByDescending { it.child("time/seconds").getValue(Long::class.java) }
                val inputChats = mutableListOf<String>()
                val isGroup = mutableListOf<Boolean>()
                var size = 0;
                for(chatSnapshot in chats){
                    chatSnapshot.child("id").getValue(String::class.java)?.let { inputChats.add(it) }
                    isGroup.add(chatSnapshot.child("group").exists())
                    size += 1
                }
                for(i in 0..<size) {
                        if(!isGroup[i]){
                            var friend: User? = null
                            var chat: IndividualChat? = null
                            val chatRef = databaseRef.getReference("/IndividualChats/${inputChats[i]}")
                            var userRef: DatabaseReference? = null
                            chatRef.addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    // Parse the user data from snapshot and update the UI
                                    chat = snapshot.getValue(IndividualChat::class.java)
                                    if(FirebaseAuth.getInstance().uid == chat?.user1) {
                                        userRef = databaseRef.getReference("/users/${chat?.user2}")
                                    }
                                    else{
                                        userRef = databaseRef.getReference("/users/${chat?.user1}")
                                    }
                                    userRef?.addValueEventListener(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            // Parse the user data from snapshot and update the UI
                                            friend = snapshot.getValue(User::class.java)
                                            groupAdapter.add(ChatItem(chat!!, friend!!))
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            // Handle any errors that occur while fetching data
                                        }
                                    })
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    // Handle any errors that occur while fetching data
                                }
                            })
                        }
                        else{
                            var group = Group()
                            databaseRef.getReference("/GroupChats/${inputChats[i]}")
                                .addValueEventListener(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        // Parse the user data from snapshot and update the UI
                                        group.name = snapshot.child("name").getValue(String::class.java).toString()
                                        group.groupId = snapshot.child("groupId").getValue(String::class.java).toString()
                                        if(snapshot.child("groupPhoto").exists())
                                            group.groupPhoto = snapshot.child("groupPhoto").getValue(String::class.java).toString()
                                        groupAdapter.add(GroupItem(group))
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        // Handle any errors that occur while fetching data
                                    }
                                })
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item?.itemId){
            R.id.main_new_friends ->{
                val intent = Intent(this, NewFriendsPage::class.java)
                startActivity(intent)
            }
            R.id.main_profile ->{
                val intent = Intent(this, ProfilePage::class.java)
                startActivity(intent)
            }
            R.id.main_settings ->{
                val intent = Intent(this, SettingsPage::class.java)
                startActivity(intent)
            }
            R.id.main_signout ->{
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginPage::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

// Class to display the chats
class ChatItem(val chat: IndividualChat, val user: User): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.username_newfriend_row).text = user.username
        if(user.profilePhoto!="")
            Picasso.get().load(user.profilePhoto).into(viewHolder.itemView.findViewById<CircleImageView>(R.id.image_newfriend_row))
    }

    override fun getLayout(): Int {
        return R.layout.chat_row
    }
}

// Class to display the groups
class GroupItem(val group: Group): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.username_newfriend_row).text = group.name
        if(group.groupPhoto!="")
            Picasso.get().load(group.groupPhoto).into(viewHolder.itemView.findViewById<CircleImageView>(R.id.image_newfriend_row))
    }

    override fun getLayout(): Int {
        return R.layout.chat_row
    }
}