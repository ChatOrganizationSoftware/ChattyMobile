package com.example.chatty

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
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

class GroupChatPage : AppCompatActivity() {
    private lateinit var enteredMessage: EditText
    private lateinit var recyclerChatLog: RecyclerView
    private lateinit var friendChatProfilePhoto: CircleImageView
    private lateinit var sendIcon: ImageView
    private lateinit var chatName: TextView
    private var group = Group()
    private lateinit var groupId: String
    private var groupAdapter = GroupAdapter<GroupieViewHolder>()
    private var databaseRef = FirebaseDatabase.getInstance()
    private var uid = FirebaseAuth.getInstance().uid
    private var listened = false

    override fun onCreate(savedInstanceState: Bundle?) {

        val isDarkTheme = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            .getBoolean("DARK_THEME", false)

        if (isDarkTheme) {
            setTheme(R.style.Theme_Chatty_Dark)  // Önceden tanımlanmış karanlık tema
        } else {
            setTheme(R.style.Theme_Chatty_Light)  // Önceden tanımlanmış aydınlık tema
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.group_chat_page)

        groupId = intent.getStringExtra(NewFriendsPage.USER_KEY)!!

        databaseRef.getReference("/users/$uid/chats/$groupId/read")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists() && snapshot.getValue(Boolean::class.java) == false)
                        snapshot.ref.setValue(true)
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val actionBar = supportActionBar
        actionBar?.setDisplayShowTitleEnabled(false)

        sendIcon = findViewById(R.id.messageSendIcon)
        enteredMessage = findViewById(R.id.enteredMessage)
        friendChatProfilePhoto = findViewById(R.id.friendChatProfilePhoto)
        chatName = findViewById(R.id.friend_chat_name)

        recyclerChatLog = findViewById(R.id.recyclerViewChatLog)
        var llm = LinearLayoutManager(this)
        llm.stackFromEnd = true
        llm.reverseLayout = false
        recyclerChatLog.layoutManager = llm

        recyclerChatLog.adapter = groupAdapter

        // Gets the group information from the firebase
        databaseRef.getReference("/GroupChats/${groupId}")
            .addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.child("deleted").exists())
                    finish()
                // Parse the user data from snapshot and update the UI
                group = snapshot.getValue(Group::class.java)!!

                if(!group.members[uid]!!)
                    finish()
                var i = 0

                for(mem in group.members.keys){
                    databaseRef.getReference("/users/$mem/username")
                        .addListenerForSingleValueEvent(object : ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                group.currentMembers.put(mem, snapshot.getValue(String::class.java)!!)
                                i++
                                if(i == group.members.keys.size){
                                    if(!listened) {
                                        listened = true
                                        listenMessages()
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                TODO("Not yet implemented")
                            }
                    })
                }
                if(group.groupPhoto != "")
                    Picasso.get().load(group.groupPhoto).into(friendChatProfilePhoto)
                chatName.text = group.name
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors that occur while fetching data
            }
        })

        friendChatProfilePhoto.setOnClickListener {
            if (group.adminId == uid) {
                enteredMessage.clearFocus() // Clear focus from EditText
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(enteredMessage.windowToken, 0)
                val intent = Intent(this, GroupAdminProfilePage::class.java)
                intent.putExtra("GROUP_ID", group.groupId)
                startActivity(intent)
            }
            else {
                enteredMessage.setText("")
                enteredMessage.clearFocus() // Clear focus from EditText
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(enteredMessage.windowToken, 0)

                val intent = Intent(this, GroupProfilePage::class.java)
                intent.putExtra("GROUP_ID", group.groupId)
                startActivity(intent)
            }
        }

        sendIcon.setOnClickListener{
            val text = enteredMessage.text.toString().trimEnd()
            if(text!="") {
                val ref = databaseRef.getReference("/GroupChats/${groupId}/Messages").push()

                val message = IndividualMessage( ref.key!!, text, null, uid!!)
                ref.setValue(message).addOnSuccessListener {
                    val time = Timestamp.now().seconds
                    for(member in group.members.keys){
                        databaseRef.getReference("/users/${member}/chats/${group.groupId}/time").setValue(time)
                        databaseRef.getReference("/users/${member}/chats/${group.groupId}/read").setValue(false)
                    }
                    enteredMessage.setText("")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Gets the all messages in the group rom the firebase
    private fun listenMessages(){
        databaseRef.getReference("/GroupChats/${groupId}/Messages")
            .addChildEventListener(object: ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(IndividualMessage::class.java)

                if(message == null)
                    showToast("NULL")
                else {
                    if (uid == message.senderId) {
                        groupAdapter.add(FriendChatToItem(message.message!!))
                        recyclerChatLog.scrollToPosition(groupAdapter.itemCount - 1)
                    } else {
                        val sender = group.currentMembers[message.senderId]
                        groupAdapter.add(GroupChatFromItem(message.message!!, sender!!))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                TODO("Not yet implemented")
            }
        })
    }

    companion object{
        const val USER_KEY = "USER_KEY"
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            // Handle other menu items if needed
        }
        return super.onOptionsItemSelected(item)
    }
}

// Class to display the messages coming from other users
class GroupChatFromItem(val text: String, val username:String): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.textViewFromRow).text = text
        viewHolder.itemView.findViewById<TextView>(R.id.chat_row_username).text = username
    }

    override fun getLayout(): Int {
        return R.layout.group_chat_from_row
    }
}