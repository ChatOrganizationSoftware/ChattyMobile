package com.example.chatty

import android.annotation.SuppressLint
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import de.hdodenhof.circleimageview.CircleImageView
import com.google.firebase.Timestamp
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener


class FriendChatPage : AppCompatActivity() {
    private lateinit var enteredMessage: EditText
    private lateinit var recyclerChatLog: RecyclerView
    private lateinit var friendChatProfilePhoto: CircleImageView
    private lateinit var sendIcon: ImageView
    private lateinit var chatName: TextView
    private var chat: IndividualChat? = null
    private var friend: User? = null
    private var groupAdapter = GroupAdapter<GroupieViewHolder>()
    private var databaseRef = FirebaseDatabase.getInstance()
    private var uid = FirebaseAuth.getInstance().uid

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {

        val isDarkTheme = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            .getBoolean("DARK_THEME", false)

        if (isDarkTheme) {
            setTheme(R.style.Theme_Chatty_Dark)  // Önceden tanımlanmış karanlık tema
        } else {
            setTheme(R.style.Theme_Chatty_Light)  // Önceden tanımlanmış aydınlık tema
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.friend_chat_page)

        val chatId = intent.getStringExtra("CHAT_ID")!!

        databaseRef.getReference("/users/$uid/chats/$chatId/read")
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

        databaseRef.getReference("/IndividualChats/$chatId/deleted")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists())
                        finish()
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        databaseRef.getReference("/IndividualChats/$chatId")
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()) {
                        chat = snapshot.getValue(IndividualChat::class.java)

                        var ref: DatabaseReference? = null
                        if (uid == chat!!.user1)
                            ref = databaseRef.getReference("/users/${chat!!.user2}")
                        else
                            ref = databaseRef.getReference("/users/${chat!!.user1}")

                        ref.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                // Parse the user data from snapshot and update the UI
                                friend = snapshot.getValue(User::class.java)
                                if (friend == null)
                                    showToast("Error: Couldn't get the user data")
                                else {
                                    chatName.text = friend!!.username
                                    val image = friend!!.profilePhoto
                                    if (image != "") {
                                        Picasso.get().load(image).into(friendChatProfilePhoto)
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Handle any errors that occur while fetching data
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
        listenMessages(chatId)

        // Go to user's profile page
        friendChatProfilePhoto.setOnClickListener {
            enteredMessage.clearFocus() // Clear focus from EditText
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(enteredMessage.windowToken, 0)
            val intent = Intent(this@FriendChatPage, FriendProfilePage::class.java)
            intent.putExtra(USER_KEY, friend!!.userId)
            intent.putExtra("CHAT_ID", chatId)
            startActivity(intent, null)
        }

        // Send message icon
        sendIcon.setOnClickListener{
            val text = enteredMessage.text.toString().trimEnd()
            if(text!="") {
                val ref = databaseRef.getReference("/IndividualChats/${chatId}/Messages").push()
                val message = IndividualMessage( ref.key!!, text, null, uid!!)
                ref.setValue(message).addOnSuccessListener {
                    val time = Timestamp.now().seconds
                    databaseRef.getReference("/users/${friend?.userId}/chats/${chat!!.id}/read").setValue(false)
                    databaseRef.getReference("/users/${chat!!.user1}/chats/${chat!!.id}/time").setValue(time)
                    databaseRef.getReference("/users/${chat!!.user2}/chats/${chat!!.id}/time").setValue(time)

                    enteredMessage.setText("")
                }
            }
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    // Gets the all previous messages in the chat from the firebase
    private fun listenMessages(chatId: String){
        databaseRef.getReference("/IndividualChats/${chatId}/Messages").addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if(snapshot.exists()) {
                    val message = IndividualMessage()
                    message.message = snapshot.child("message").getValue(String::class.java)
                    message.senderId = snapshot.child("senderId").getValue(String::class.java)!!
                    message.photoURI = snapshot.child("photoURI").getValue(String::class.java)
                    message.id = snapshot.key.toString()

                    if (uid == message.senderId) {
                        groupAdapter.add(FriendChatToItem(message.message!!))
                        recyclerChatLog.scrollToPosition(groupAdapter.itemCount - 1)
                    } else
                        groupAdapter.add(FriendChatFromItem(message.message!!))
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

// Class to display messages get from the other user
class FriendChatFromItem(val text: String): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.textViewFromRow).text = text
    }

    override fun getLayout(): Int {
        return R.layout.friend_chat_from_row
    }
}

// Class to display messages the current user has sent
class FriendChatToItem(val text: String): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.textViewToRow).text = text
    }

    override fun getLayout(): Int {
        return R.layout.friend_chat_to_row
    }
}