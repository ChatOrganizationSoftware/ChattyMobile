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
    private var chatId: String? = null
    private var user: User? = null
    private var databaseRef = FirebaseDatabase.getInstance()
    private var uid = FirebaseAuth.getInstance().uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.friend_profile_page)

        val userId = intent.getStringExtra(FriendChatPage.USER_KEY)
        chatId = intent.getStringExtra("CHAT_ID")

        val toolbar = findViewById<Toolbar>(R.id.toolbar2)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        nameField = findViewById(R.id.nameField)
        aboutField = findViewById(R.id.aboutField)
        visibility = findViewById(R.id.visibilityText)
        blockButton = findViewById(R.id.blockButton)
        removeChatButton = findViewById(R.id.deleteChat)

        databaseRef.getReference("/users/${uid}/username")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentUser = snapshot.getValue(String::class.java)
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        databaseRef.getReference("/users/${userId}")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    user = snapshot.getValue(User::class.java)
                    nameField.text = user!!.username
                    aboutField.text = user!!.about
                    visibility.text = user!!.visibility
                    if(user!!.profilePhoto != "") {
                        Picasso.get().load(user!!.profilePhoto).into(findViewById<CircleImageView>(R.id.friend_profile_photo))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        removeChatButton.setOnClickListener {
            if (user != null) {
                val folderRef = FirebaseStorage.getInstance().getReference("/$chatId")
                folderRef.listAll().addOnSuccessListener { list ->
                    list.items.forEach {
                        it.delete()
                    }
                }

                databaseRef.getReference("/users/${uid}/friends/${user!!.userId}")
                    .removeValue()
                databaseRef.getReference("/users/${user!!.userId}/friends/${uid}")
                    .removeValue()
                databaseRef.getReference("/users/${user!!.userId}/chats/${chatId}")
                    .removeValue()
                databaseRef.getReference("/users/${user!!.userId}/notifications")
                    .push().setValue("$currentUser has removed your chat.")
                databaseRef.getReference("/IndividualChats/${chatId}")
                    .removeValue()
                databaseRef.getReference("/users/${uid}/chats/${chatId}")
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
                databaseRef.getReference("/users/${uid}/block/${user!!.userId}").setValue(user!!.userId)
                databaseRef.getReference("/users/${user!!.userId}/blockedBy/${uid}").setValue(uid)

                val folderRef = FirebaseStorage.getInstance().getReference("/$chatId")
                folderRef.listAll().addOnSuccessListener { list ->
                    list.items.forEach {
                        it.delete()
                    }
                }

                databaseRef.getReference("/users/${uid}/friends/${user!!.userId}")
                    .removeValue()
                databaseRef.getReference("/users/${user!!.userId}/friends/${uid}")
                    .removeValue()
                databaseRef.getReference("/users/${user!!.userId}/chats/${chatId}")
                    .removeValue()
                databaseRef.getReference("/users/${user!!.userId}/notifications")
                    .push().setValue("$currentUser has blocked you.")
                databaseRef.getReference("/IndividualChats/${chatId}")
                    .removeValue()
                databaseRef.getReference("/users/${uid}/chats/${chatId}")
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
                databaseRef.getReference("/IndividualChats/$chatId")
                    .addValueEventListener(object: ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val chat = snapshot.getValue(IndividualChat::class.java)
                            val intent = Intent(this@FriendProfilePage, FriendChatPage::class.java)
                            intent.putExtra(NewFriendsPage.USER_KEY, chat!!.id)
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