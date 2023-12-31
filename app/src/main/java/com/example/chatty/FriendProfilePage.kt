package com.example.chatty

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FriendProfilePage : AppCompatActivity() {
    private lateinit var nameField: TextView
    private lateinit var aboutField: TextView
    private lateinit var visibility: TextView
    private lateinit var blockButton: Button
    private lateinit var removeChatButton: Button
    private var currentUser: String? = null
    var chatId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.friend_profile_page)

        val user = intent.getParcelableExtra<User>(FriendChatPage.USER_KEY)

        val toolbar = findViewById<Toolbar>(R.id.toolbar2)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        nameField = findViewById(R.id.nameField)
        aboutField = findViewById(R.id.aboutField)
        visibility = findViewById(R.id.visibilityText)
        blockButton = findViewById(R.id.blockButton)
        removeChatButton = findViewById(R.id.deleteChat)

        FirebaseDatabase.getInstance().getReference("/users/${FirebaseAuth.getInstance().uid}/username")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentUser = snapshot.getValue(String::class.java)
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        // Get the information about the user from the firebase
        val database = FirebaseDatabase.getInstance().getReference("users/${user?.userId}")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Parse the user data from snapshot and update the UI
                nameField.text = snapshot.child("username").getValue(String::class.java)
                aboutField.text = snapshot.child("about").getValue(String::class.java)
                visibility.text = snapshot.child("visibility").getValue(String::class.java)

                val image = snapshot.child("profilePhoto").getValue(String::class.java)
                if(image != "") {
                    Picasso.get().load(image).into(findViewById<CircleImageView>(R.id.friend_profile_photo))
                }

            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors that occur while fetching data
            }
        })

        if (user != null) {
            FirebaseDatabase.getInstance().getReference("/users/${FirebaseAuth.getInstance().uid}/friends/${user.userId}")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        chatId = snapshot.getValue(String::class.java)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })
        }

        removeChatButton.setOnClickListener {
            if (user != null) {
                val folderRef = FirebaseStorage.getInstance().getReference("/$chatId")
                folderRef.listAll().addOnSuccessListener { list ->
                    list.items.forEach {
                        it.delete()
                    }
                }

                FirebaseDatabase.getInstance()
                    .getReference("/users/${FirebaseAuth.getInstance().uid}/friends/${user.userId}")
                    .removeValue()
                FirebaseDatabase.getInstance()
                    .getReference("/users/${user.userId}/friends/${FirebaseAuth.getInstance().uid}")
                    .removeValue()
                FirebaseDatabase.getInstance().getReference("/users/${user.userId}/chats/${chatId}")
                    .removeValue()
                FirebaseDatabase.getInstance().getReference("/users/${user.userId}/notifications")
                    .push().setValue("$currentUser has removed your chat.")
                FirebaseDatabase.getInstance().getReference("/IndividualChats/${chatId}")
                    .removeValue()
                FirebaseDatabase.getInstance()
                    .getReference("/users/${FirebaseAuth.getInstance().uid}/chats/${chatId}")
                    .removeValue().addOnSuccessListener {
                        showToast("ACTIVITY")
                        val intent = Intent(this, MainPage::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
            }
        }

        blockButton.setOnClickListener {
            if (user != null) {
                FirebaseDatabase.getInstance().getReference("/users/${FirebaseAuth.getInstance().uid}/block/${user.userId}").setValue(user.userId)
                FirebaseDatabase.getInstance().getReference("/users/${user.userId}/blockedBy/${FirebaseAuth.getInstance().uid}").setValue(FirebaseAuth.getInstance().uid)

                val folderRef = FirebaseStorage.getInstance().getReference("/$chatId")
                folderRef.listAll().addOnSuccessListener { list ->
                    list.items.forEach {
                        it.delete()
                    }
                }

                FirebaseDatabase.getInstance()
                    .getReference("/users/${FirebaseAuth.getInstance().uid}/friends/${user.userId}")
                    .removeValue()
                FirebaseDatabase.getInstance()
                    .getReference("/users/${user.userId}/friends/${FirebaseAuth.getInstance().uid}")
                    .removeValue()
                FirebaseDatabase.getInstance().getReference("/users/${user.userId}/chats/${chatId}")
                    .removeValue()
                FirebaseDatabase.getInstance().getReference("/users/${user.userId}/notifications")
                    .push().setValue("$currentUser has blocked you.")
                FirebaseDatabase.getInstance().getReference("/IndividualChats/${chatId}")
                    .removeValue()
                FirebaseDatabase.getInstance()
                    .getReference("/users/${FirebaseAuth.getInstance().uid}/chats/${chatId}")
                    .removeValue().addOnSuccessListener {
                        val intent = Intent(this, MainPage::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.friend_profile_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> {
                FirebaseDatabase.getInstance().getReference("/IndividualChats/$chatId")
                    .addValueEventListener(object: ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val chat = snapshot.getValue(IndividualChat::class.java)
                            val intent = Intent(this@FriendProfilePage, FriendChatPage::class.java)
                            intent.putExtra(NewFriendsPage.USER_KEY, chat)
                            startActivity(intent)
                            finish()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }
                    })
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}