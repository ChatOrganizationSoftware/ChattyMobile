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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
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
    private lateinit var sendImageIcon: ImageView
    private lateinit var chatName: TextView
    private var group = Group()
    private lateinit var groupId: String
    private var memberTable= HashMap<String, User>()
    private var groupAdapter = GroupAdapter<GroupieViewHolder>()
    private var selectedPhoto: Uri? = null
    private var databaseRef = FirebaseDatabase.getInstance()
    private var uid = FirebaseAuth.getInstance().uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.group_chat_page)

        groupId = intent.getStringExtra(NewFriendsPage.USER_KEY)!!

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

        listenMessages()

        // Gets the group information from the firebase
        databaseRef.getReference("/GroupChats/${groupId}").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(!snapshot.exists()){
                    val intent = Intent(this@GroupChatPage, MainPage::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    finish()
                }

                // Parse the user data from snapshot and update the UI
                group.name = snapshot.child("name").getValue(String::class.java).toString()
                group.groupId = groupId
                group.groupPhoto =snapshot.child("groupPhoto").getValue(String::class.java).toString()
                if(group.groupPhoto != "")
                    Picasso.get().load(group.groupPhoto).into(friendChatProfilePhoto)
                group.about = snapshot.child("about").getValue(String::class.java).toString()
                chatName.text = group.name

                group.members = mutableListOf()
                for(mem in snapshot.child("members").children)
                    mem.getValue(String::class.java)?.let { group.members.add(it) }
                group.adminId = snapshot.child("adminId").getValue(String::class.java)
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
                finish()
            }
            else {
                enteredMessage.clearFocus() // Clear focus from EditText
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(enteredMessage.windowToken, 0)
                val intent = Intent(this, GroupProfilePage::class.java)
                intent.putExtra("GROUP_ID", group.groupId)
                startActivity(intent)
                finish()
            }
        }

        sendIcon.setOnClickListener{
            val text = enteredMessage.text.toString().trimEnd()
            if(text!="") {
                val ref = databaseRef.getReference("/GroupChats/${groupId}/Messages").push()

                val message = IndividualMessage( ref.key!!, text, null, uid!!)
                ref.setValue(message).addOnSuccessListener {
                    val time = Timestamp.now()
                    for(member in group.members){
                        databaseRef.getReference("/users/${member}/chats/${group.groupId}/time").setValue(time)
                    }
                    enteredMessage.setText("")
                }
            }
        }

        sendImageIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode==0 && resultCode== Activity.RESULT_OK && data!=null){
            selectedPhoto = data.data

            val intent = Intent(this, DisplayImageGroupPage::class.java)
            intent.putExtra("PHOTO_SELECTED", selectedPhoto.toString())
            intent.putExtra("GROUP", group)
            startActivity(intent)
        }
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private  fun fetchMembers(){
        for(mem in group.members){
            databaseRef.getReference("/users/${mem}")
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                val user = dataSnapshot.getValue(User::class.java) as User
                                memberTable.put(user.userId, user)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                TODO("Not yet implemented")
                            }
                        })
        }
        groupAdapter.clear()
        listenMessages()
    }

    // Gets the all messages in the group rom the firebase
    private fun listenMessages(){
        databaseRef.getReference("/GroupChats/${groupId}/Messages")
            .addChildEventListener(object: ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = IndividualMessage()
                message.message = snapshot.child("message").getValue(String::class.java)
                message.senderId = snapshot.child("senderId").getValue(String::class.java)!!
                message.photoURI = snapshot.child("photoURI").getValue(String::class.java)
                message.id = snapshot.key.toString()

                if(message.photoURI == null) {
                    if (uid == message.senderId) {
                        groupAdapter.add(FriendChatToItem(message.message!!))
                        recyclerChatLog.scrollToPosition(groupAdapter.itemCount - 1)
                    }
                    else {
                        if(memberTable.containsKey(message.senderId)){
                            val sender = memberTable[message.senderId]
                            groupAdapter.add(GroupChatFromItem(message.message!!, sender!!.username))
                        }
                        else{
                            databaseRef.getReference("/users/${}")
                        }
                    }
                }
                else{
                    if (uid == message.senderId) {
                        groupAdapter.add(FriendChatToPhoto(message.photoURI!!))
                        recyclerChatLog.scrollToPosition(groupAdapter.itemCount - 1)
                    }
                    else {
                        val sender = memberTable[message.senderId]
                        groupAdapter.add(GroupChatFromPhoto(sender!!.username, message.photoURI!!))
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

class GroupChatFromPhoto(val username: String, val photoURI: String): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.chat_row_username).text = username
        Picasso.get().load(photoURI).into(viewHolder.itemView.findViewById<ImageView>(R.id.sentPhoto))
    }

    override fun getLayout(): Int {
        return R.layout.group_chat_from_photo
    }
}