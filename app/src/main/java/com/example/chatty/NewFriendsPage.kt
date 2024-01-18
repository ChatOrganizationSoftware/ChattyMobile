package com.example.chatty

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import java.util.Base64
import java.util.UUID
import javax.crypto.KeyGenerator

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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        val isDarkTheme = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            .getBoolean("DARK_THEME", false)

        if (isDarkTheme) {
            setTheme(R.style.Theme_Chatty_Dark)  // Önceden tanımlanmış karanlık tema
        } else {
            setTheme(R.style.Theme_Chatty_Light)  // Önceden tanımlanmış aydınlık tema
        }

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
                if (block.contains(userItem.chat.id)) {
                    val intent = Intent(view.context, NonFriendProfilePage::class.java)
                    intent.putExtra("USER_ID", userItem.chat.id)
                    startActivity(intent)
                    clicked = false
                } else {
                    val chatId = UUID.randomUUID().toString()
                    val chat = IndividualChat(
                        chatId,
                        FirebaseAuth.getInstance().uid!!,
                        userItem.chat.id
                    )
                    chat.key = generateKey()
                    val chatRef = databaseRef.getReference("/IndividualChats/${chatId}")
                    chatRef.setValue(chat).addOnFailureListener {
                        showToast("Error: Couldn't create the chat")
                    }.addOnCompleteListener {
                        val time = Timestamp.now().seconds
                        databaseRef.getReference("/users/${chat.user1}/chats/${chat.user2}/id")
                            .setValue(chat.id)
                        databaseRef.getReference("/users/${chat.user1}/chats/${chat.user2}/time")
                            .setValue(time)

                        databaseRef.getReference("/users/${chat.user2}/chats/${chat.user1}/id")
                            .setValue(chat.id)
                        databaseRef.getReference("/users/${chat.user2}/chats/${chat.user1}/time")
                            .setValue(time)
                        databaseRef.getReference("/users/${chat.user2}/chats/${chat.user1}/read")
                            .setValue(false)

                        val intent = Intent(view.context, FriendChatPage::class.java)
                        intent.putExtra("CHAT_ID", chat.id)
                        intent.putExtra("FRIEND_ID", chat.user2)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateKey(): String {
        val key = KeyGenerator.getInstance("AES").generateKey()
        return Base64.getEncoder().encodeToString(key.encoded)
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
                    if (it.child("visibility").getValue(String::class.java) =="Public" && it.key != FirebaseAuth.getInstance().uid && !friends.contains(it.key) && !blockedBy.contains(it.key) && it.child("active").getValue(Boolean::class.java) == true){
                        val chat = Chat(it.key!!, false)
                        chat.name = it.child("username").getValue(String::class.java).toString()
                        chat.photoURI = it.child("profilePhoto").getValue(String::class.java)
                        groupAdapter.add(NewUserItem(chat))
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
        val ref = databaseRef.getReference("/users/${FirebaseAuth.getInstance().uid}/chats")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if( dataSnapshot.exists() && !dataSnapshot.child("group").exists()) {
                    friends = mutableListOf()
                    for (snapshot in dataSnapshot.children)
                        snapshot.key?.let { friends.add(it) }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
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
                }
            })
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

// Class to display the new users
class NewUserItem(val chat: Chat): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.username_newfriend_row).text = chat.name
        if(chat.photoURI !="")
            Picasso.get().load(chat.photoURI).into(viewHolder.itemView.findViewById<CircleImageView>(R.id.image_newfriend_row))
    }

    override fun getLayout(): Int {
        return R.layout.chat_row
    }
}