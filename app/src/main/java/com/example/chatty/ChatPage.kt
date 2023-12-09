package com.example.chatty

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import de.hdodenhof.circleimageview.CircleImageView

class ChatPage : AppCompatActivity() {
    private lateinit var sendMessageIcon: ImageView
    private lateinit var enteredMessage: EditText
    private lateinit var recyclerChatLog: RecyclerView
    private lateinit var friendChatProfilePhoto: CircleImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_page)

        val user = intent.getParcelableExtra<User>(NewFriendsPage.USER_KEY)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val actionBar = supportActionBar
        actionBar?.setDisplayShowTitleEnabled(false)

        findViewById<TextView>(R.id.chat_name).text = user?.username

        sendMessageIcon = findViewById(R.id.messageSendIcon)
        enteredMessage = findViewById(R.id.enteredMessage)
        recyclerChatLog = findViewById(R.id.recyclerViewChatLog)
        friendChatProfilePhoto = findViewById(R.id.friendChatProfilePhoto)

        if(user?.profilePhoto!="") {
            Picasso.get().load(user?.profilePhoto).into(friendChatProfilePhoto)
        }

        val adapter = GroupAdapter<GroupieViewHolder>()
        adapter.add(FriendChatFromItem())
        adapter.add(FriendChatToItem())
        adapter.add(FriendChatFromItem())
        adapter.add(FriendChatToItem())
        adapter.add(FriendChatFromItem())
        adapter.add(FriendChatToItem())
        adapter.add(FriendChatFromItem())
        adapter.add(FriendChatToItem())
        adapter.add(FriendChatFromItem())
        adapter.add(FriendChatToItem())
        adapter.add(FriendChatFromItem())
        adapter.add(FriendChatToItem())
        recyclerChatLog.adapter = adapter
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

class FriendChatFromItem: Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

    }

    override fun getLayout(): Int {
        return R.layout.friend_chat_from_row
    }
}
class FriendChatToItem: Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {

    }

    override fun getLayout(): Int {
        return R.layout.friend_chat_to_row
    }
}
