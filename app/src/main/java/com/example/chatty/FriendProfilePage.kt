package com.example.chatty

import android.content.Intent
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
    private var databaseRef = FirebaseDatabase.getInstance()
    private var uid = FirebaseAuth.getInstance().uid
    private var clicked = false

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

        val userId = intent.getStringExtra("USER_ID")
        chatId = intent.getStringExtra("CHAT_ID")

        databaseRef.getReference("/IndividualChats/$chatId")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(!snapshot.exists()){
                        startActivity(Intent(this@FriendProfilePage, MainPage::class.java))

                        finishAffinity()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
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

        databaseRef.getReference("/users/$uid")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(!snapshot.exists()){
                        startActivity(Intent(this@FriendProfilePage, LoginPage::class.java))

                        finishAffinity()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        databaseRef.getReference("/users/${uid}/username")
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentUser = snapshot.getValue(String::class.java)
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        databaseRef.getReference("/users/${userId}")
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()) {
                        nameField.text = snapshot.child("username").getValue(String::class.java)
                        aboutField.text = snapshot.child("about").getValue(String::class.java)
                        visibility.text = snapshot.child("visibility").getValue(String::class.java)
                        val image = snapshot.child("profilePhoto").getValue(String::class.java)
                        if (image != "") {
                            Picasso.get().load(image).into(findViewById<CircleImageView>(R.id.friend_profile_photo))
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        removeChatButton.setOnClickListener {
            if(!clicked) {
                clicked = true
                databaseRef.getReference("/users/$userId/chats/$uid").removeValue()
                databaseRef.getReference("/users/$userId/notifications").push()
                    .setValue("{$currentUser} has removed your chat.")


                databaseRef.getReference("/users/$uid/chats/$userId")
                    .removeValue().addOnCompleteListener {
                        databaseRef.getReference("/IndividualChats/$chatId").removeValue()
                        startActivity(Intent(this, MainPage::class.java))

                        finishAffinity()
                    }
            }
        }

        blockButton.setOnClickListener {
            if(!clicked) {
                clicked = true
                databaseRef.getReference("/users/$uid/block/$userId").setValue(userId)
                databaseRef.getReference("/users/$userId/blockedBy/$uid").setValue(uid)

                databaseRef.getReference("/users/$userId/chats/$uid").removeValue()
                databaseRef.getReference("/users/$userId/notifications").push()
                    .setValue("{$currentUser} has blocked you.")

                databaseRef.getReference("/users/$uid/chats/$userId")
                    .removeValue().addOnCompleteListener {
                        databaseRef.getReference("/IndividualChats/$chatId").removeValue()
                        startActivity(Intent(this, MainPage::class.java))

                        finishAffinity()
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
                if(!clicked)
                    finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}