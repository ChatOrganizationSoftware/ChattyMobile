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
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import de.hdodenhof.circleimageview.CircleImageView

class MainPage : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var createGroupIcon: ImageView
    private var groupAdapter = GroupAdapter<GroupieViewHolder>()
    private var databaseRef = FirebaseDatabase.getInstance()
    private val uid = FirebaseAuth.getInstance().uid
    private val chats = hashMapOf<String, ChatItem>()

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

        databaseRef.getReference("/users/$uid")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(!snapshot.exists()){
                        startActivity(Intent(this@MainPage, LoginPage::class.java))

                        finishAffinity()
                    }
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
                val intent = Intent(this, GroupChatPage::class.java)
                intent.putExtra("GROUP_ID", chat.id)
                startActivity(intent)
            }
        }

    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun displayChats(){
        databaseRef.getReference("/users/$uid/chats").orderByChild("time")
            .addChildEventListener(object: ChildEventListener{
                override fun onChildAdded(snap: DataSnapshot, previousChildName: String?) {
                    if (snap.exists()) {
                        val chat = Chat(
                            snap.child("id").getValue(String::class.java)!!,
                            snap.child("group").exists()
                        )
                        chat.read = snap.child("read").getValue(Boolean::class.java) != false
                        chat.friendId = snap.key
                        var chatItem = ChatItem(chat)
                        chats[chat.id] = chatItem
                        groupAdapter.add(0, chatItem)
                        if (!chat.group) {
                            databaseRef.getReference("/users/${chat.friendId}/username")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists())
                                            chat.name =
                                                snapshot.getValue(String::class.java).toString()

                                        chatItem.notifyChanged()
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                    }
                                })
                            databaseRef.getReference("/users/${chat.friendId}/profilePhoto")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists())
                                            chat.photoURI = snapshot.getValue(String::class.java)
                                        chatItem.notifyChanged()
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                    }
                                })
                        } else {
                            databaseRef.getReference("/GroupChats/${chat.id}/name")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists())
                                            chat.name =
                                                snapshot.getValue(String::class.java).toString()
                                        chatItem.notifyChanged()
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                    }
                                })
                            databaseRef.getReference("/GroupChats/${chat.id}/groupPhoto")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists())
                                            chat.photoURI = snapshot.getValue(String::class.java)
                                        chatItem.notifyChanged()
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                    }
                                })
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val chatItem = chats[snapshot.child("id").getValue(String::class.java)]
                    groupAdapter.remove(chatItem!!)
                    groupAdapter.add(0, chatItem)
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val id = snapshot.child("id").getValue(String::class.java)
                    val chatItem = chats[id]
                    groupAdapter.remove(chatItem!!)
                    chats.remove(id)
                }

                override fun onCancelled(error: DatabaseError) {
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
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
class ChatItem(val chat: Chat = Chat("", false)): Item<GroupieViewHolder>(){
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