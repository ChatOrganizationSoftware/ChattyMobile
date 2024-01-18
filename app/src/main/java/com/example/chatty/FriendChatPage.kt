package com.example.chatty

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import de.hdodenhof.circleimageview.CircleImageView
import com.google.firebase.Timestamp
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class FriendChatPage : AppCompatActivity() {
    private lateinit var enteredMessage: EditText
    private lateinit var recyclerChatLog: RecyclerView
    private lateinit var friendChatProfilePhoto: CircleImageView
    private lateinit var sendIcon: ImageView
    private lateinit var chatName: TextView
    private var groupAdapter = GroupAdapter<GroupieViewHolder>()
    private var databaseRef = FirebaseDatabase.getInstance()
    private var uid = FirebaseAuth.getInstance().uid
    private var friendId: String? = null
    private var messageCount = 0
    private var chatKey: SecretKey? = null

    private val readListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists() && snapshot.getValue(Boolean::class.java) == false)
                snapshot.ref.setValue(true)
        }

        override fun onCancelled(error: DatabaseError) {
            // Handle onCancelled if needed
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {

        val isDarkTheme = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            .getBoolean("DARK_THEME", false)

        if (isDarkTheme) {
            setTheme(R.style.Theme_Chatty_Dark)  // Önceden tanımlanmış karanlık tema
        } else {
            setTheme(R.style.Theme_Chatty_Light)  // Önceden tanımlanmış aydınlık tema
        }

        databaseRef.getReference("/users/$uid")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(!snapshot.exists()){
                        startActivity(Intent(this@FriendChatPage, LoginPage::class.java))

                        finishAffinity()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        super.onCreate(savedInstanceState)
        setContentView(R.layout.friend_chat_page)

        val chatId = intent.getStringExtra("CHAT_ID")!!
        friendId = intent.getStringExtra("FRIEND_ID")!!

        databaseRef.getReference("/users/$uid/chats/$friendId/read").addValueEventListener(readListener)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val actionBar = supportActionBar
        actionBar?.setDisplayShowTitleEnabled(false)

        sendIcon = findViewById(R.id.messageSendIcon)
        enteredMessage = findViewById(R.id.enteredMessage)
        friendChatProfilePhoto = findViewById(R.id.friendChatProfilePhoto)
        chatName = findViewById(R.id.friend_chat_name)

        recyclerChatLog = findViewById(R.id.recyclerViewChatLog)
        var llm = LinearLayoutManager(this)
        llm.stackFromEnd = true
        llm.reverseLayout = false
        recyclerChatLog.layoutManager = llm

        recyclerChatLog.adapter = groupAdapter

        databaseRef.getReference("/IndividualChats/$chatId")
            .addValueEventListener(object: ValueEventListener{
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(!snapshot.exists())
                        finish()
                    else
                        messageCount = snapshot.child("Messages").childrenCount.toInt()
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        databaseRef.getReference("IndividualChats/$chatId/key")
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    chatKey = getKeyFromString(snapshot.getValue(String::class.java).toString())
                    listenMessages(chatId)
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        databaseRef.getReference("/users/$friendId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()) {
                        chatName.text = snapshot.child("username").getValue(String::class.java)
                        val image = snapshot.child("profilePhoto").getValue(String::class.java)
                        if (image != "")
                            Picasso.get().load(image).into(friendChatProfilePhoto)
                    }
                    else
                        finish()
                }

                override fun onCancelled(error: DatabaseError) {
                // Handle any errors that occur while fetching data
                }
            })

        // Go to user's profile page
        friendChatProfilePhoto.setOnClickListener {
            enteredMessage.clearFocus() // Clear focus from EditText
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(enteredMessage.windowToken, 0)
            val intent = Intent(this@FriendChatPage, FriendProfilePage::class.java)
            intent.putExtra("USER_ID", friendId)
            intent.putExtra("CHAT_ID", chatId)
            startActivity(intent, null)
        }

        // Send message icon
        sendIcon.setOnClickListener{
            val text = enteredMessage.text.toString().trimEnd()
            if(text!="") {
                enteredMessage.setText("")
                val ref = databaseRef.getReference("/IndividualChats/${chatId}/Messages").push()

                val message = IndividualMessage(encrypt(text), uid!!)
                ref.setValue(message).addOnSuccessListener {
                    recyclerChatLog.scrollToPosition(groupAdapter.itemCount - 1)
                    val time = Timestamp.now().seconds
                    databaseRef.getReference("/users/$friendId/chats/$uid/read").setValue(false)
                    databaseRef.getReference("/users/$friendId/chats/$uid/time").setValue(time)
                    databaseRef.getReference("/users/$uid/chats/$friendId/time").setValue(time)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun encrypt(message: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16)  // Generate an initialization vector (IV)
        val ivSpec = IvParameterSpec(iv)
        val keySpec = SecretKeySpec(chatKey!!.encoded, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encrypted = cipher.doFinal(message.toByteArray())
        return Base64.getEncoder().encodeToString(encrypted)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun decrypt(encryptedMessage: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16)  // Retrieve the same IV used for encryption
        val ivSpec = IvParameterSpec(iv)
        val keySpec = SecretKeySpec(chatKey!!.encoded, "AES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val encryptedBytes = Base64.getDecoder().decode(encryptedMessage)
        val decrypted = cipher.doFinal(encryptedBytes)
        return decrypted.toString(Charsets.UTF_8)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getKeyFromString(encodedKeyString: String): SecretKey {
        val decodedKey = Base64.getDecoder().decode(encodedKeyString)
        return SecretKeySpec(decodedKey, "AES")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    // Gets the all previous messages in the chat from the firebase
    private fun listenMessages(chatId: String){
        var i = 0
        databaseRef.getReference("/IndividualChats/${chatId}/Messages").addChildEventListener(object: ChildEventListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if(snapshot.exists()) {
                    val message = snapshot.getValue(IndividualMessage::class.java)
                    if (uid == message!!.senderId) {
                        groupAdapter.add(FriendChatToItem(decrypt(message.message!!)))
                        if(i<0)
                            recyclerChatLog.scrollToPosition(groupAdapter.itemCount - 1)
                    }
                    else
                        groupAdapter.add(FriendChatFromItem(decrypt(message.message!!)))

                    i++
                    if(i==messageCount) {
                        recyclerChatLog.scrollToPosition(groupAdapter.itemCount - 1)
                        i = -99999999
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
            }

        })
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

    override fun onDestroy() {
        super.onDestroy()
        databaseRef.getReference("/users/$uid/chats/$friendId/read").removeEventListener(readListener)
    }
}

// Class to display messages get from the other user
class FriendChatFromItem(val text: String): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.textViewFromRow).text = text
    }

    override fun getLayout(): Int {
        return R.layout.friend_chat_from_row
    }
}

// Class to display messages the current user has sent
class FriendChatToItem(val text: String): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.textViewToRow).text = text
    }

    override fun getLayout(): Int {
        return R.layout.friend_chat_to_row
    }
}