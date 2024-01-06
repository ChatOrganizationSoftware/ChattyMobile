package com.example.chatty

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainPage : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var createGroupIcon: ImageView
    private var groupAdapter = GroupAdapter<GroupieViewHolder>()
    private var databaseRef = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_page)

        databaseRef.getReference("/users/${FirebaseAuth.getInstance().uid}/active").setValue(true)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.recyclerviewChats)
        recyclerView.adapter = groupAdapter

        displayChats()

        createGroupIcon = findViewById(R.id.createGroupIcon)
        createGroupIcon.setOnClickListener{
            val intent = Intent(this, CreateGroupPage::class.java)
            startActivity(intent)
        }


        groupAdapter.setOnItemClickListener { item, view ->

            if (item is ChatItem) {
                val chatItem = item as ChatItem
                val chat = chatItem.chatId

                val intent = Intent(view.context, FriendChatPage::class.java)
                intent.putExtra("CHAT_ID", chat)
                startActivity(intent)
            } else {
                val groupItem = item as GroupItem
                val group = groupItem.groupId

                val intent = Intent(view.context, GroupChatPage::class.java)
                intent.putExtra(NewFriendsPage.USER_KEY, group)
                startActivity(intent)
            }
        }

    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun displayChats(){
        databaseRef.getReference("/users/${FirebaseAuth.getInstance().uid}/chats")
            .addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val inputChats = mutableListOf<Chat>()
                groupAdapter.clear()
                for(data in snapshot.children){
                    val chat = Chat(data.child("id").getValue(String::class.java)!!, data.child("group").exists(), data.child("time").getValue(Long::class.java)!!)
                    inputChats.add(chat)
                }

                val sortedChats = inputChats.sortedByDescending { it.time }
                    CoroutineScope(Dispatchers.IO).launch {
                        for (chat in sortedChats) {
                            if (!chat.group) {
                                val user: User = getFriendForIndividualChat(chat.id)
                                launch(Dispatchers.Main) {
                                    groupAdapter.add(
                                        ChatItem(
                                            chat.id,
                                            user.username,
                                            user.profilePhoto
                                        )
                                    )
                                }.join() // Wait for UI update to complete
                            } else {
                                val group: Group = getGroup(chat.id)
                                launch(Dispatchers.Main) {
                                    groupAdapter.add(
                                        GroupItem(
                                            chat.id,
                                            group.name,
                                            group.groupPhoto
                                        )
                                    )
                                }.join() // Wait for UI update to complete
                            }
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private suspend fun getFriendForIndividualChat(chatId: String): User {
        return withContext(Dispatchers.IO) {
            val chatRef = databaseRef.getReference("/IndividualChats/$chatId")
            val snapshot = chatRef.get().await()
            val chat = snapshot.getValue(IndividualChat::class.java)
            val friend = if (FirebaseAuth.getInstance().uid == chat?.user1) {
                chat?.user2
            } else {
                chat?.user1
            }
            val snap = FirebaseDatabase.getInstance().getReference("/users/$friend").get().await()
            val user = snap.getValue(User::class.java)
            return@withContext user!! // Ensure friend is not null
        }
    }

    private suspend fun getGroup(chatId: String): Group {
        return withContext(Dispatchers.IO) {
            val snapshot = FirebaseDatabase.getInstance().getReference("/GroupChats/$chatId").get().await()
            val group = snapshot.getValue(Group::class.java)
            return@withContext group!! // Ensure friend is not null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item?.itemId){
            R.id.main_notifications -> {
                val intent = Intent(this, NotificationsPage::class.java)
                startActivity(intent)
            }
            R.id.main_new_friends ->{
                val intent = Intent(this, NewFriendsPage::class.java)
                startActivity(intent)
            }
            R.id.main_profile ->{
                val intent = Intent(this, ProfilePage::class.java)
                startActivity(intent)
            }
            R.id.main_settings ->{
                val intent = Intent(this, SettingsPage::class.java)
                startActivity(intent)
            }
            R.id.main_signout ->{
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginPage::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

// Class to display the chats
class ChatItem(val chatId:String, val username: String, val profilePhoto: String): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.username_newfriend_row).text = username
        if(profilePhoto!="")
            Picasso.get().load(profilePhoto).into(viewHolder.itemView.findViewById<CircleImageView>(R.id.image_newfriend_row))
    }

    override fun getLayout(): Int {
        return R.layout.chat_row
    }
}

// Class to display the groups
class GroupItem(val groupId: String, val name: String, val groupPhoto: String): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.username_newfriend_row).text = name
        if(groupPhoto!="")
            Picasso.get().load(groupPhoto).into(viewHolder.itemView.findViewById<CircleImageView>(R.id.image_newfriend_row))
    }

    override fun getLayout(): Int {
        return R.layout.chat_row
    }
}