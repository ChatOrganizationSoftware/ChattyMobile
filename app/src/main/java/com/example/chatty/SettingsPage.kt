package com.example.chatty

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class SettingsPage: AppCompatActivity() {

    private lateinit var themeSettings: ConstraintLayout
    private lateinit var changePasswordSettings: ConstraintLayout
    private lateinit var deleteAccountSettings: ConstraintLayout
    private val databaseRef = FirebaseDatabase.getInstance()
    private lateinit var username: String
    private var chats = mutableListOf<Chat>()
    private val uid = FirebaseAuth.getInstance().uid


    override fun onCreate(savedInstanceState: Bundle?) {

        val isDarkTheme = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            .getBoolean("DARK_THEME", false)

        if (isDarkTheme) {
            setTheme(R.style.Theme_Chatty_Dark)  // Önceden tanımlanmış karanlık tema
        } else {
            setTheme(R.style.Theme_Chatty_Light)  // Önceden tanımlanmış aydınlık tema
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_page)

        val toolbar = findViewById<Toolbar>(R.id.toolbar3)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        themeSettings = findViewById(R.id.themeSettings)
        changePasswordSettings = findViewById(R.id.changePasswordSettings)
        deleteAccountSettings = findViewById(R.id.deleteAccountSettings)

        themeSettings.setOnClickListener {
            toggleTheme()
        }

        databaseRef.getReference("/users/$uid")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(!snapshot.exists()){
                        startActivity(Intent(this@SettingsPage, LoginPage::class.java))

                        finishAffinity()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        databaseRef.getReference("/users/$uid/username")
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    username = snapshot.getValue(String::class.java)!!
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        databaseRef.getReference("/users/$uid/chats")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    chats = mutableListOf()
                    for (data in snapshot.children) {
                        val chat = Chat(
                            data.child("id").getValue(String::class.java)!!,
                            data.child("group").exists(),
                            0
                        )
                        chats.add(chat)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        deleteAccountSettings.setOnClickListener{
            if(chats.size != 0) {
                for (chat in chats) {
                    if (!chat.group) {
                        databaseRef.getReference("/IndividualChats/${chat.id}")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if(snapshot.exists()) {
                                        val indChat = snapshot.getValue(IndividualChat::class.java)
                                        var friend = indChat!!.user1
                                        if (uid == indChat.user1)
                                            friend = indChat.user2

                                        databaseRef.getReference("/users/${friend}/chats/$uid")
                                            .removeValue()
                                        databaseRef.getReference("/users/${friend}/notifications")
                                            .push()
                                            .setValue("{$username} has closed his/her account.")
                                        databaseRef.getReference("/IndividualChats/${indChat.id}").removeValue()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                }
                            })
                    }
                    else {
                        databaseRef.getReference("/GroupChats/${chat.id}")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if(snapshot.exists()) {
                                        val name = snapshot.child("name").getValue(String::class.java)
                                        val genericType = object : GenericTypeIndicator<HashMap<String, String>>() {}
                                        val members = snapshot.child("/members").getValue(genericType)?.values?.toMutableList()
                                        val admin = snapshot.child("/adminId").getValue(String::class.java)
                                        if (members != null) {
                                            members.remove(uid)
                                            if (uid != admin) {
                                                if (members.size > 1) {
                                                    databaseRef.getReference("/GroupChats/${chat.id}/members/$uid")
                                                        .removeValue()
                                                    databaseRef.getReference("/GroupChats/${chat.id}/prevMembers/$uid")
                                                        .setValue(username)
                                                }
                                                else {
                                                    FirebaseStorage.getInstance()
                                                        .getReference("/Profile Photos/${chat.id}")
                                                        .delete()
                                                    for (mem in members) {
                                                        databaseRef.getReference("/users/$mem/chats/${chat.id}")
                                                            .removeValue()
                                                            .addOnSuccessListener {
                                                                databaseRef.getReference("/users/$mem/notifications")
                                                                    .push()
                                                                    .setValue("{$name} is closed.")
                                                            }
                                                    }
                                                    databaseRef.getReference("/GroupChats/${chat.id}").removeValue()
                                                }
                                            }
                                            else {
                                                FirebaseStorage.getInstance()
                                                    .getReference("/Profile Photos/${chat.id}")
                                                    .delete()
                                                for (mem in members) {
                                                    databaseRef.getReference("/users/$mem/chats/${chat.id}")
                                                        .removeValue()
                                                        .addOnSuccessListener {
                                                            databaseRef.getReference("/users/$mem/notifications")
                                                                .push()
                                                                .setValue("{$name} is closed.")
                                                        }
                                                }
                                                databaseRef.getReference("/GroupChats/${chat.id}").removeValue()
                                            }
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                }
                            })
                    }
                }
            }

            FirebaseDatabase.getInstance().getReference("/users/$uid").removeValue()
            FirebaseAuth.getInstance().currentUser?.delete()
                ?.addOnCompleteListener {
                    FirebaseAuth.getInstance().signOut()

                    val intent = Intent(this@SettingsPage, LoginPage::class.java)
                    startActivity(intent)
                    finishAffinity()
                }
        }
    }

    private fun toggleTheme() {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val isDarkTheme = sharedPreferences.getBoolean("DARK_THEME", false)

        sharedPreferences.edit().putBoolean("DARK_THEME", !isDarkTheme).apply()

        recreate()  // Aktiviteyi yeniden başlatarak temayı uygula
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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