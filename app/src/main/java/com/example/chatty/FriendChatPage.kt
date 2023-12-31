package com.example.chatty

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
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
import com.google.firebase.database.getValue
import com.google.protobuf.Value


class FriendChatPage : AppCompatActivity() {
    private lateinit var enteredMessage: EditText
    private lateinit var recyclerChatLog: RecyclerView
    private lateinit var friendChatProfilePhoto: CircleImageView
    private lateinit var sendIcon: ImageView
    private lateinit var chatName: TextView
    private var chat: IndividualChat? = null
    private var friend: User? = null
    private lateinit var databaseRef: DatabaseReference
    private var groupAdapter = GroupAdapter<GroupieViewHolder>()
    private lateinit var sendImageIcon: ImageView
    private var selectedPhoto: Uri? = null

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        showToast("CHAT")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.friend_chat_page)

        chat = intent.getParcelableExtra<IndividualChat>(NewFriendsPage.USER_KEY)!!
        if(chat==null){
            val intent = Intent(this, MainPage::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val actionBar = supportActionBar
        actionBar?.setDisplayShowTitleEnabled(false)

        sendImageIcon = findViewById(R.id.sendImageIcon)
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
        if(FirebaseAuth.getInstance().uid==chat!!.user1)
            ref = FirebaseDatabase.getInstance().getReference("/users/${chat!!.user2}")
        else
            ref = FirebaseDatabase.getInstance().getReference("/users/${chat!!.user1}")

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

        databaseRef = FirebaseDatabase.getInstance().getReference("/IndividualChats/${chat!!.id}/Messages")
        listenMessages()

        // Go to user's profile page
        friendChatProfilePhoto.setOnClickListener {
            enteredMessage.clearFocus() // Clear focus from EditText
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(enteredMessage.windowToken, 0)
            val intent = Intent(this, FriendProfilePage::class.java)
            intent.putExtra(USER_KEY, friend)
            startActivity(intent)
            finish()
        }

        // Send message icon
        sendIcon.setOnClickListener{
            val text = enteredMessage.text.toString().trimEnd()
            if(text!="") {
                val ref = databaseRef.push()
                val message = IndividualMessage( ref.key!!, text, null, FirebaseAuth.getInstance().uid!!,Timestamp.now())
                ref.setValue(message).addOnSuccessListener {
                    val time = Timestamp.now()
                    FirebaseDatabase.getInstance().getReference("/users/${chat!!.user1}/chats/${chat!!.id}/time").setValue(time)
                    FirebaseDatabase.getInstance().getReference("/users/${chat!!.user2}/chats/${chat!!.id}/time").setValue(time)
                    enteredMessage.setText("")
                }
            }
        }

        sendImageIcon.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    // Gets the all previous messages in the chat from the firebase
    private fun listenMessages(){
        databaseRef.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = IndividualMessage()
                message.message = snapshot.child("message").getValue(String::class.java)
                message.senderId = snapshot.child("senderId").getValue(String::class.java)!!
                message.photoURI = snapshot.child("photoURI").getValue(String::class.java)
                message.id = snapshot.key.toString()

                if(message.photoURI == null) {
                    if (FirebaseAuth.getInstance().uid == message.senderId) {
                        groupAdapter.add(FriendChatToItem(message.message!!))
                        recyclerChatLog.scrollToPosition(groupAdapter.itemCount - 1)
                    }
                    else
                        groupAdapter.add(FriendChatFromItem(message.message!!))
                }
                else{
                    if (FirebaseAuth.getInstance().uid == message.senderId) {
                        groupAdapter.add(FriendChatToPhoto(message.photoURI!!))
                        recyclerChatLog.scrollToPosition(groupAdapter.itemCount - 1)
                    }
                    else
                        groupAdapter.add(FriendChatFromPhoto(message.photoURI!!))
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode==0 && resultCode== Activity.RESULT_OK && data!=null){
            selectedPhoto = data.data

            val intent = Intent(this, DisplayImagePage::class.java)
            intent.putExtra("PHOTO_SELECTED", selectedPhoto.toString())
            intent.putExtra("CHAT_ID", chat!!.id)
            intent.putExtra("FRIEND_ID", friend?.userId)
            startActivity(intent)
        }
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

class FriendChatToPhoto(val photoURI: String): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        Picasso.get().load(photoURI).into(viewHolder.itemView.findViewById<ImageView>(R.id.sentPhoto))
    }

    override fun getLayout(): Int {
        return R.layout.friend_chat_to_photo
    }
}

class FriendChatFromPhoto(val photoURI: String): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        Picasso.get().load(photoURI).into(viewHolder.itemView.findViewById<ImageView>(R.id.sentPhoto))
    }

    override fun getLayout(): Int {
        return R.layout.friend_chat_from_photo
    }
}


