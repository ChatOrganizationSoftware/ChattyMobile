package com.example.chatty

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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
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
    private lateinit var chatName: TextView
    private lateinit var group: Group
    private lateinit var memberTable: HashMap<String, User>
    private lateinit var databaseRef: DatabaseReference
    private var groupAdapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.group_chat_page)

        val groupId = intent.getStringExtra(NewFriendsPage.USER_KEY)!!

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
        FirebaseDatabase.getInstance().getReference("/GroupChats/${groupId}").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Parse the user data from snapshot and update the UI
                group = snapshot.getValue(Group::class.java)!!
                chatName.text = group.name
                val image = group.groupPhoto
                if(image != "") {
                    Picasso.get().load(image).into(friendChatProfilePhoto)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors that occur while fetching data
            }
        })

        databaseRef = FirebaseDatabase.getInstance().getReference("/GroupChats/${groupId}/Messages")

        memberTable = HashMap<String, User>()
        listenMessages()

        friendChatProfilePhoto.setOnClickListener {
            val intent = Intent(this, GroupProfilePage::class.java)
            intent.putExtra("Group", group)
            val bundle = Bundle()
            bundle.putSerializable("memberTable", memberTable)
            intent.putExtra("Members", bundle)
            startActivity(intent)
        }

        sendIcon.setOnClickListener{
            val text = enteredMessage.text.toString().trimEnd()
            if(text!="") {
                val ref = databaseRef.push()

                val time = Timestamp.now()
                val message = IndividualMessage( ref.key!!, text, FirebaseAuth.getInstance().uid!!, time)
                ref.setValue(message).addOnSuccessListener {
                    for(member in group.members){
                        FirebaseDatabase.getInstance().getReference("/users/${member}/chats/${group.groupId}/time").setValue(time)
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
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val messagesList = mutableListOf<String>()
                val sendersList = mutableListOf<String>()

                var size = 0
                for (snapshot in dataSnapshot.children) {
                    snapshot.child("message").getValue<String>()?.let { messagesList.add(it) }
                    snapshot.child("senderId").getValue<String>()?.let { sendersList.add(it) }
                    size += 1
                }

                var count = 0
                for(member in group.members){
                    FirebaseDatabase.getInstance().getReference("/users/${member}")
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                val user = dataSnapshot.getValue(User::class.java) as User
                                memberTable.put(user.userId, user)
                                count += 1
                                if(count==group.members.size){
                                    for(i in 0..<size){
                                        if (FirebaseAuth.getInstance().uid == sendersList[(i)])
                                            groupAdapter.add(FriendChatToItem(messagesList[i]))
                                        else {
                                            val sender = memberTable[sendersList[i]]
                                            groupAdapter.add( GroupChatFromItem( messagesList[i], sender!!.username, sender.profilePhoto ))
                                        }
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

// Class to display the messages coming from other users
class GroupChatFromItem(val text: String, val username:String, val photoURI:String): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.textViewFromRow).text = text
        viewHolder.itemView.findViewById<TextView>(R.id.chat_row_username).text = username
        if(photoURI!=""){
            Picasso.get().load(photoURI).into(viewHolder.itemView.findViewById<CircleImageView>(R.id.group_chat_row_image))
        }
    }

    override fun getLayout(): Int {
        return R.layout.group_chat_from_row
    }
}