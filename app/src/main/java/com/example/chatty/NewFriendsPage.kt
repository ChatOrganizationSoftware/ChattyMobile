package com.example.chatty

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class NewFriendsPage : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private var friends = mutableListOf<String>()
    private var blockedBy = mutableListOf<String>()
    private var block = mutableListOf<String>()
    private lateinit var searchBar: EditText
    private var databaseRef = FirebaseDatabase.getInstance()
    private var clicked = false
    private var groupAdapter = GroupAdapter<GroupieViewHolder>()

    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_friends_page)

        clicked = false

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Select User"

        searchBar = findViewById(R.id.searchBar)
        recyclerView = findViewById(R.id.recyclerViewNewUsers)
        recyclerView.adapter = groupAdapter

        returnFriends()

        // Listener to text changes. For each change fetch the users for the current text
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // This method is called before the text changes
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // This method is called when the text is changing
            }

            override fun afterTextChanged(s: Editable?) {
                // This method is called after the text has changed
                val typedText = s.toString().trim()
                searchJob?.cancel() // Cancel previous job if running

                if (typedText.isNotEmpty()) {
                    searchJob = CoroutineScope(Dispatchers.Main).launch {
                        delay(300) // Adjust the delay duration as needed (in milliseconds)
                        groupAdapter.clear()
                        fetchUsers(typedText)
                    }
                }
                else{
                    groupAdapter.clear()
                }
            }
        })

        groupAdapter.setOnItemClickListener { item, view ->
            if (!clicked) {
                clicked = true
                val userItem = item as NewUserItem
                if (block.contains(userItem.user.userId)) {
                    val intent = Intent(view.context, NonFriendProfilePage::class.java)
                    intent.putExtra(USER_KEY, userItem.user.userId)
                    startActivity(intent)
                    clicked = false
                } else {
                    val chatId = UUID.randomUUID().toString()
                    val chat = IndividualChat(
                        chatId,
                        FirebaseAuth.getInstance().uid!!,
                        userItem.user.userId
                    )
                    val chatRef = databaseRef.getReference("/IndividualChats/${chatId}")
                    chatRef.setValue(chat).addOnFailureListener {
                        showToast("Error: Couldn't create the chat")
                    }.addOnCompleteListener {
                        val time = Timestamp.now().seconds
                        databaseRef.getReference("/users/${chat.user1}/chats/${chat.id}/id")
                            .setValue(chat.id)
                        databaseRef.getReference("/users/${chat.user1}/chats/${chat.id}/time")
                            .setValue(time)
                        databaseRef.getReference("/users/${chat.user1}/friends/${chat.user2}")
                            .setValue(chat.id)

                        databaseRef.getReference("/users/${chat.user2}/chats/${chat.id}/id")
                            .setValue(chat.id)
                        databaseRef.getReference("/users/${chat.user2}/chats/${chat.id}/time")
                            .setValue(time)
                        databaseRef.getReference("/users/${chat.user2}/friends/${chat.user1}")
                            .setValue(chat.id)

                        val intent = Intent(view.context, FriendChatPage::class.java)
                        intent.putExtra("CHAT_ID", chat.id)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }

    companion object{
        const val USER_KEY = "USER_KEY"
    }

    // Fetch the users which are matching with the given text
    private fun fetchUsers(typedText: String){
        databaseRef.getReference("/users").orderByChild("username").startAt(typedText).endAt(typedText + "\uf8ff")
            .addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach{
                    val user = it.getValue(User::class.java)
                    if (user != null) {
                        if (user.visibility =="Public" && it.key != FirebaseAuth.getInstance().currentUser?.uid && !friends.contains(user.userId) && !blockedBy.contains(user.userId) && user.active == true){
                            groupAdapter.add(NewUserItem(user))
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // onBackPressed() // Go back when the Up button is clicked
                // return true
                finish()
            }
            // Handle other menu items if needed
        }
        return super.onOptionsItemSelected(item)
    }

    // Returns the users we are already messaging so we don't display them again.
    private fun returnFriends() {
        val ref = databaseRef.getReference("/users/${FirebaseAuth.getInstance().uid}/friends")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                friends = mutableListOf()
                for (snapshot in dataSnapshot.children) {
                    snapshot.key?.let { friends.add(it) }
                }


            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors if any
                println("Error: ${databaseError.message}")
            }
        })

        databaseRef.getReference("/users/${FirebaseAuth.getInstance().uid}/blockedBy")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    blockedBy = mutableListOf()
                    for (snapshot in dataSnapshot.children) {
                        val chat = snapshot.getValue(String::class.java)
                        chat?.let { blockedBy.add(it) }
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        databaseRef.getReference("/users/${FirebaseAuth.getInstance().uid}/block")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    block = mutableListOf()
                    for (snapshot in dataSnapshot.children) {
                        val chat = snapshot.getValue(String::class.java)
                        chat?.let { block.add(it) }
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

// Class to display the new users
class NewUserItem(val user: User): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.username_newfriend_row).text = user.username
        viewHolder.itemView.findViewById<TextView>(R.id.visibility_newfriend_row).text = user.visibility
        if(user.profilePhoto!="")
            Picasso.get().load(user.profilePhoto).into(viewHolder.itemView.findViewById<CircleImageView>(R.id.image_newfriend_row))
    }

    override fun getLayout(): Int {
        return R.layout.new_friends_row
    }
}