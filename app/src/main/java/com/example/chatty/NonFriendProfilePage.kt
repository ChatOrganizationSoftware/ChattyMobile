package com.example.chatty

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class NonFriendProfilePage : AppCompatActivity() {
    private lateinit var nameField: TextView
    private lateinit var aboutField: TextView
    private lateinit var visibility: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nonfriend_profile_page)

        val user = intent.getParcelableExtra<User>(ChatPage.USER_KEY)

        val toolbar = findViewById<Toolbar>(R.id.toolbar2)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        nameField = findViewById(R.id.nameField)
        aboutField = findViewById(R.id.aboutField)
        visibility = findViewById(R.id.visibilityText)

        val database = FirebaseDatabase.getInstance().getReference("users/${user?.userId}")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Parse the user data from snapshot and update the UI
                nameField.text = snapshot.child("username").getValue(String::class.java)
                aboutField.text = snapshot.child("about").getValue(String::class.java)
                visibility.text = snapshot.child("visibility").getValue(String::class.java)

                val image = snapshot.child("profilePhoto").getValue(String::class.java)
                if(image != "") {
                    Picasso.get().load(image).into(findViewById<CircleImageView>(R.id.nonfriend_profile_photo))
                }

            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors that occur while fetching data
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.friend_profile_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> {
                val caller = intent.getStringExtra("callerActivity")
                if (caller == "NewFriendsPage") {
                    val launchIntent = Intent(this, NewFriendsPage::class.java)
                    startActivity(launchIntent)
                    finish()
                }

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}