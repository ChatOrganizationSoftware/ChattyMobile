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
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class SettingsPage: AppCompatActivity() {

    private lateinit var themeSettings: ConstraintLayout
    private lateinit var changePasswordSettings: ConstraintLayout
    private lateinit var deleteAccountSettings: ConstraintLayout
    private val databaseRef = FirebaseDatabase.getInstance()
    private lateinit var user: User
    private var chats = mutableListOf<Chat>()

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

        databaseRef.getReference("/users/${FirebaseAuth.getInstance().uid}")
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    user = snapshot.getValue(User::class.java)!!
                    for(data in snapshot.child("chats").children){
                        val chat = Chat(data.child("id").getValue(String::class.java)!!, data.child("group").exists(), 0)
                        chats.add(chat)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        deleteAccountSettings.setOnClickListener{
            if(chats.size != 0) {
                for (chat in chats) {
                    if (!chat.group) {
                        databaseRef.getReference("/IndividualChats/${chat.id}")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val indChat = snapshot.getValue(IndividualChat::class.java)
                                    if (indChat != null) {
                                        val folderRef = FirebaseStorage.getInstance()
                                            .getReference("/${indChat.id}")
                                        folderRef.listAll().addOnSuccessListener { list ->
                                            list.items.forEach {
                                                it.delete()
                                            }
                                        }
                                        var friend = indChat.user1
                                        if (user.userId == indChat.user1)
                                            friend = indChat.user2

                                        databaseRef.getReference("/users/${friend}/friends/${user.userId}")
                                            .removeValue()
                                        databaseRef.getReference("/users/${friend}/chats/${indChat.id}")
                                            .removeValue()
                                        databaseRef.getReference("/users/${friend}/notifications")
                                            .push()
                                            .setValue("{${user.username}} has closed his/her account.")
                                        databaseRef.getReference("/IndividualChats/${indChat.id}/deleted")
                                            .setValue(true)
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    TODO("Not yet implemented")
                                }
                            })
                    } else {
                        databaseRef.getReference("/GroupChats/${chat.id}")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val group = snapshot.getValue(Group::class.java)
                                    if (group != null) {
                                        group.members.remove(user.userId)
                                        if (user.userId != group.adminId) {
                                            if (group.members.keys.size > 1) {
                                                databaseRef.getReference("/GroupChats/${group.groupId}/members/${user.userId}")
                                                    .removeValue()
                                                databaseRef.getReference("/GroupChats/${group.groupId}/prevMembers/${user.userId}")
                                                    .setValue(user.username)
                                            } else {
                                                FirebaseStorage.getInstance()
                                                    .getReference("/Profile Photos/${group.groupId}")
                                                    .delete()
                                                for (mem in group.members.keys) {
                                                    databaseRef.getReference("/users/$mem/chats/${group.groupId}")
                                                        .removeValue()
                                                        .addOnSuccessListener {
                                                            databaseRef.getReference("/users/$mem/notifications")
                                                                .push()
                                                                .setValue("{${group.name}} is closed.")
                                                        }
                                                }
                                                databaseRef.getReference("/GroupChats/${group.groupId}/deleted")
                                                    .setValue(true)
                                            }
                                        } else {
                                            FirebaseStorage.getInstance()
                                                .getReference("/Profile Photos/${group.groupId}")
                                                .delete()
                                            for (mem in group.members.keys) {
                                                databaseRef.getReference("/users/$mem/chats/${group.groupId}")
                                                    .removeValue()
                                                    .addOnSuccessListener {
                                                        databaseRef.getReference("/users/$mem/notifications")
                                                            .push()
                                                            .setValue("{${group.name}} is closed.")
                                                    }
                                            }
                                            databaseRef.getReference("/GroupChats/${group.groupId}/deleted")
                                                .setValue(true)
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    TODO("Not yet implemented")
                                }
                            })
                    }
                }
            }
            FirebaseDatabase.getInstance().getReference("/users/${user.userId}/active").removeValue()
            FirebaseAuth.getInstance().currentUser?.delete()
                ?.addOnCompleteListener {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this@SettingsPage, LoginPage::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
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