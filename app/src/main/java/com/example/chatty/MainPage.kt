package com.example.chatty

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder

class MainPage : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_page)

        verifyUserIsLoggedIn()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.recyclerviewChats)

        fetchChats()
    }

    private fun fetchChats(){
        /*val ref = FirebaseDatabase.getInstance().getReference("/users").orderByChild("username")
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupAdapter = GroupAdapter<GroupieViewHolder>()
                snapshot.children.forEach{
                    val user = it.getValue(User::class.java)
                    if (user != null && it.key != FirebaseAuth.getInstance().currentUser?.uid){
                        groupAdapter.add(UserItem(user))
                    }
                }

                groupAdapter.setOnItemClickListener { item, view ->
                    val userItem = item as UserItem

                    val intent = Intent(view.context, FriendProfilePage::class.java)
                    intent.putExtra(NewFriendsPage.USER_KEY, item.user)
                    startActivity(intent)

                    finish()
                }

                recyclerView.adapter = groupAdapter
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })*/
        val ref = FirebaseDatabase.getInstance().getReference("/users")
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupAdapter = GroupAdapter<GroupieViewHolder>()
                snapshot.children.forEach{
                    val user = it.getValue(User::class.java)
                    if (user != null && it.key == "Fza4118CEAdx0xoPZyXkOg6b4rW2"){
                        groupAdapter.add(UserItem(user))
                    }
                }

                groupAdapter.setOnItemClickListener { item, view ->
                    val userItem = item as UserItem

                    val intent = Intent(view.context, ChatPage::class.java)
                    intent.putExtra(NewFriendsPage.USER_KEY, userItem.user)
                    startActivity(intent)
                }

                recyclerView.adapter = groupAdapter
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun verifyUserIsLoggedIn(){
        if(FirebaseAuth.getInstance().uid == null){
            val intent = Intent(this, LoginPage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item?.itemId){
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