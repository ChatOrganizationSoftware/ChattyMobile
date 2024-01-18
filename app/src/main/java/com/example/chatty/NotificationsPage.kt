package com.example.chatty

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item

class NotificationsPage : AppCompatActivity() {
    private lateinit var uid: String
    private lateinit var notificationView: RecyclerView
    private var groupAdapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {

        val isDarkTheme = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            .getBoolean("DARK_THEME", false)

        if (isDarkTheme) {
            setTheme(R.style.Theme_Chatty_Dark)  // Önceden tanımlanmış karanlık tema
        } else {
            setTheme(R.style.Theme_Chatty_Light)  // Önceden tanımlanmış aydınlık tema
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.notifications_page)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Notifications"

        uid = FirebaseAuth.getInstance().uid.toString()

        notificationView = findViewById(R.id.notificationView)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        layoutManager.reverseLayout = true
        notificationView.layoutManager = layoutManager

        notificationView.adapter = groupAdapter

        listenNotifications()
    }

    private fun listenNotifications(){
        FirebaseDatabase.getInstance().getReference("/users/$uid/notifications")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    groupAdapter.clear()
                    for(not in snapshot.children){
                        groupAdapter.add(NotificationItem(not.getValue(String::class.java)!!, not.ref))
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
}

class NotificationItem(val notification: String, val notRef: DatabaseReference): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.notificationText).text = notification
        viewHolder.itemView.findViewById<ImageView>(R.id.removeMember).setOnClickListener {
            notRef.removeValue()
        }
    }

    override fun getLayout(): Int {
        return R.layout.notification_item
    }
}