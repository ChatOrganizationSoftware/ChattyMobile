package com.example.chatty

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
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
import com.google.firebase.database.getValue
import com.google.protobuf.Value


class FriendChatPage : AppCompatActivity() {
    private lateinit var enteredMessage: EditText
    private lateinit var recyclerChatLog: RecyclerView
    private lateinit var friendChatProfilePhoto: CircleImageView
    private lateinit var sendIcon: ImageView
    private lateinit var chatName: TextView
    private lateinit var chat: IndividualChat
    private var friend: User? = null
    private lateinit var databaseRef: DatabaseReference
    private var groupAdapter = GroupAdapter<GroupieViewHolder>()
    private val messages = mutableListOf<String>()

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.friend_chat_page)

        chat = intent.getParcelableExtra<IndividualChat>(NewFriendsPage.USER_KEY)!!

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

        var ref: DatabaseReference? = null
        if(FirebaseAuth.getInstance().uid==chat.user1)
            ref = FirebaseDatabase.getInstance().getReference("/users/${chat.user2}")
        else
            ref = FirebaseDatabase.getInstance().getReference("/users/${chat.user2}")

            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Parse the user data from snapshot and update the UI
                    friend = snapshot.getValue(User::class.java)
                    if(friend==null)
                        showToast("Error: Couldn't get the user data")
                    else{
                        chatName.text = friend!!.username
                        val image = friend!!.profilePhoto
                        if(image != "") {
                            Picasso.get().load(image).into(friendChatProfilePhoto)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle any errors that occur while fetching data
                }
            })

        databaseRef = FirebaseDatabase.getInstance().getReference("/IndividualChats/${chat.id}/Messages")

        listenMessages()

        friendChatProfilePhoto.setOnClickListener {
            val intent = Intent(this, FriendProfilePage::class.java)
            intent.putExtra(USER_KEY, friend)
            startActivity(intent)
        }

        sendIcon.setOnClickListener{
            val text = enteredMessage.text.toString().trimEnd()
            if(text!="") {
                val ref = databaseRef.push()
                val message = IndividualMessage( ref.key!!, text, FirebaseAuth.getInstance().uid!!,Timestamp.now())
                ref.setValue(message).addOnSuccessListener {
                    val time = Timestamp.now()
                    FirebaseDatabase.getInstance().getReference("/users/${chat.user1}/chats/${chat.id}/time").setValue(time)
                    FirebaseDatabase.getInstance().getReference("/users/${chat.user2}/chats/${chat.id}/time").setValue(time)
                    enteredMessage.setText("")
                }
            }
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private fun listenMessages(){
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val messagesList = mutableListOf<String>()
                val sendersList = mutableListOf<String>()
                val idList = mutableListOf<String>()

                var size = 0
                for (snapshot in dataSnapshot.children) {
                    snapshot.child("message").getValue<String>()?.let { messagesList.add(it) }
                    snapshot.child("senderId").getValue<String>()?.let { sendersList.add(it) }
                    size += 1
                    snapshot.child("id").getValue<String>()?.let { idList.add(it) }
                }

                for(i in 0..<size){
                    if(!messages.contains(idList[i])) {
                        if (FirebaseAuth.getInstance().uid == sendersList[(i)])
                            groupAdapter.add(FriendChatToItem(messagesList[i]))
                        else
                            groupAdapter.add(FriendChatFromItem(messagesList[i]))
                        messages.add(idList[i])
                        recyclerChatLog.post {
                            recyclerChatLog.scrollToPosition(groupAdapter.itemCount - 1)
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors if any
                println("Error: ${databaseError.message}")
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

class FriendChatFromItem(val text: String): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.textViewFromRow).text = text
    }

    override fun getLayout(): Int {
        return R.layout.friend_chat_from_row
    }
}
class FriendChatToItem(val text: String): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.textViewToRow).text = text
    }

    override fun getLayout(): Int {
        return R.layout.friend_chat_to_row
    }
}
