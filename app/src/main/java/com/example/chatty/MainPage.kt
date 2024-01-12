package com.example.chatty

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainPage : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var createGroupIcon: ImageView
    private var groupAdapter = GroupAdapter<GroupieViewHolder>()
    private var databaseRef = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {

        val isDarkTheme = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            .getBoolean("DARK_THEME", false)

        if (isDarkTheme) {
            setTheme(R.style.Theme_Chatty_Dark)  // Önceden tanımlanmış karanlık tema
        } else {
            setTheme(R.style.Theme_Chatty_Light)  // Önceden tanımlanmış aydınlık tema
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_page)

        databaseRef.getReference("/users/${FirebaseAuth.getInstance().uid}/active")
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists())
                        snapshot.ref.setValue(true)
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

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

            val chatItem = item as ChatItem
            val chat = chatItem.chat
            if(!chat.group) {
                val intent = Intent(view.context, FriendChatPage::class.java)
                intent.putExtra("CHAT_ID", chat.id)
                intent.putExtra("FRIEND_ID", chat.friendId)
                startActivity(intent)
            }
            else {
                    val intent = Intent(view.context, GroupChatPage::class.java)
                    intent.putExtra(NewFriendsPage.USER_KEY, chat.id)
                    startActivity(intent)
            }
        }

    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun displayChats(){
        databaseRef.getReference("/users/${FirebaseAuth.getInstance().uid}/chats").orderByChild("time")
            .addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()) {
                    groupAdapter.clear()
                    for (data in snapshot.children) {
                        if(!data.child("group").exists()) {
                            databaseRef.getReference("/users/${data.key}")
                                .addListenerForSingleValueEvent(object: ValueEventListener{
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val chat = Chat(data.child("id").getValue(String::class.java)!!, false)
                                        chat.name = snapshot.child("username").getValue(String::class.java)
                                        chat.photoURI = snapshot.child("profilePhoto").getValue(String::class.java)
                                        chat.read = !(data.child("read").exists() && data.child("read").getValue(Boolean::class.java) == false )
                                        chat.friendId = data.key
                                        groupAdapter.add(ChatItem(chat))
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                    }
                                })
                        }
                        else{
                            databaseRef.getReference("/GroupChats/${data.key}")
                                .addListenerForSingleValueEvent(object: ValueEventListener{
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val chat = Chat(data.child("groupId").getValue(String::class.java)!!, true)
                                        chat.name = snapshot.child("name").getValue(String::class.java)
                                        chat.photoURI = snapshot.child("profilePhoto").getValue(String::class.java)
                                        chat.read = (data.child("read").exists() && data.child("read").getValue(Boolean::class.java) == true )
                                        groupAdapter.add(ChatItem(chat))
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item?.itemId) {
            R.id.main_notifications -> {
                val intent = Intent(this, NotificationsPage::class.java)
                startActivity(intent)
            }

            R.id.main_new_friends -> {
                val intent = Intent(this, NewFriendsPage::class.java)
                startActivity(intent)
            }

            R.id.main_profile -> {
                val intent = Intent(this, ProfilePage::class.java)
                startActivity(intent)
            }

            R.id.main_settings -> {
                val intent = Intent(this, SettingsPage::class.java)
                startActivity(intent)
            }

            R.id.main_signout -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginPage::class.java)
                startActivity(intent)
                finishAffinity()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

// Class to display the chats
class ChatItem(val chat: Chat): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        if(!chat.read)
            viewHolder.itemView.findViewById<ImageView>(R.id.newMessageAlert).visibility = View.VISIBLE
        else
            viewHolder.itemView.findViewById<ImageView>(R.id.newMessageAlert).visibility = View.INVISIBLE
        viewHolder.itemView.findViewById<TextView>(R.id.username_newfriend_row).text = chat.name
        if(chat.photoURI!="")
            Picasso.get().load(chat.photoURI).into(viewHolder.itemView.findViewById<CircleImageView>(R.id.image_newfriend_row))
    }

    override fun getLayout(): Int {
        return R.layout.chat_row
    }
}