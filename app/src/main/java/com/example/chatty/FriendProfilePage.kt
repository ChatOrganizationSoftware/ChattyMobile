package com.example.chatty

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

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
    private var deleted = false

    override fun onCreate(savedInstanceState: Bundle?) {

        val isDarkTheme = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            .getBoolean("DARK_THEME", false)

        if (isDarkTheme) {
            setTheme(R.style.Theme_Chatty_Dark)  // Önceden tanımlanmış karanlık tema
        } else {
            setTheme(R.style.Theme_Chatty_Light)  // Önceden tanımlanmış aydınlık tema
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.friend_profile_page)

        val userId = intent.getStringExtra(FriendChatPage.USER_KEY)
        chatId = intent.getStringExtra("CHAT_ID")

        databaseRef.getReference("/IndividualChats/$chatId")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.child("deleted").exists() && !deleted){
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

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
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentUser = snapshot.getValue(String::class.java)
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        databaseRef.getReference("/users/${userId}")
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    user = snapshot.getValue(User::class.java)
                    nameField.text = user!!.username
                    aboutField.text = user!!.about
                    visibility.text = user!!.visibility
                    if (user!!.profilePhoto != "") {
                        Picasso.get().load(user!!.profilePhoto)
                            .into(findViewById<CircleImageView>(R.id.friend_profile_photo))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        removeChatButton.setOnClickListener {
            deleted = true

            if (user != null) {
                databaseRef.getReference("/users/${uid}/friends/${user!!.userId}").removeValue()
                databaseRef.getReference("/users/${user!!.userId}/friends/${uid}").removeValue()
                databaseRef.getReference("/users/${user!!.userId}/chats/${chatId}").removeValue()
                databaseRef.getReference("/users/${user!!.userId}/notifications").push().setValue("{$currentUser} has removed your chat.")

                databaseRef.getReference("/IndividualChats/$chatId/deleted").setValue(true)
                    .addOnCompleteListener {
                        databaseRef.getReference("/users/${uid}/chats/${chatId}")
                            .removeValue().addOnCompleteListener {
                                finish()
                            }
                    }
            }
        }

        blockButton.setOnClickListener {
            deleted = true
            if (user != null) {

                databaseRef.getReference("/users/${uid}/block/${user!!.userId}").setValue(user!!.userId)
                databaseRef.getReference("/users/${user!!.userId}/blockedBy/${uid}").setValue(uid)

                databaseRef.getReference("/users/${uid}/friends/${user!!.userId}").removeValue()
                databaseRef.getReference("/users/${user!!.userId}/friends/${uid}").removeValue()
                databaseRef.getReference("/users/${user!!.userId}/chats/${chatId}").removeValue()
                databaseRef.getReference("/users/${user!!.userId}/notifications").push().setValue("{$currentUser} has blocked you.")
                databaseRef.getReference("/IndividualChats/$chatId/deleted").setValue(true)
                    .addOnCompleteListener {
                        databaseRef.getReference("/users/${uid}/chats/${chatId}")
                            .removeValue().addOnCompleteListener {
                                finish()
                            }
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
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}