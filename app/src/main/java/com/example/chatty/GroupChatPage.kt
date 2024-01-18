package com.example.chatty

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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class GroupChatPage : AppCompatActivity() {
    private lateinit var enteredMessage: EditText
    private lateinit var recyclerChatLog: RecyclerView
    private lateinit var friendChatProfilePhoto: CircleImageView
    private lateinit var sendIcon: ImageView
    private lateinit var chatName: TextView
    private var members : List<String>? = null
    private var memberNames : HashMap<String, String>? = null
    private lateinit var groupId: String
    private var groupAdapter = GroupAdapter<GroupieViewHolder>()
    private var databaseRef = FirebaseDatabase.getInstance()
    private var uid = FirebaseAuth.getInstance().uid
    private var listened = false
    private lateinit var admin : String
    private val readListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists() && snapshot.getValue(Boolean::class.java) == false)
                snapshot.ref.setValue(true)
        }

        override fun onCancelled(error: DatabaseError) {
            // Handle onCancelled if needed
        }
    }
    private var messageCount = 0
    private var chatKey:SecretKey? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        val isDarkTheme = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
            .getBoolean("DARK_THEME", false)

        if (isDarkTheme) {
            setTheme(R.style.Theme_Chatty_Dark)  // Önceden tanımlanmış karanlık tema
        } else {
            setTheme(R.style.Theme_Chatty_Light)  // Önceden tanımlanmış aydınlık tema
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.group_chat_page)

        groupId = intent.getStringExtra("GROUP_ID")!!

        databaseRef.getReference("/users/$uid/chats/$groupId/read").addValueEventListener(readListener)

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

        databaseRef.getReference("/GroupChats/$groupId")
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

        databaseRef.getReference("/GroupChats/$groupId/key")
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    chatKey = getKeyFromString(snapshot.getValue(String::class.java).toString())
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        // Gets the group information from the firebase
        databaseRef.getReference("/GroupChats/${groupId}/name")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        chatName.text = snapshot.getValue(String::class.java)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        // Gets the group information from the firebase
        databaseRef.getReference("/GroupChats/${groupId}/adminId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        admin = snapshot.getValue(String::class.java).toString()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        databaseRef.getReference("/GroupChats/${groupId}/groupPhoto")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val image = snapshot.getValue(String::class.java)
                        if(image != "")
                            Picasso.get().load(image).into(friendChatProfilePhoto)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })


        databaseRef.getReference("/GroupChats/$groupId/members")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()) {
                        val genericType = object : GenericTypeIndicator<HashMap< String,String>>() {}
                        members = snapshot.getValue(genericType)?.values?.toMutableList()
                        if (members != null) {
                            memberNames = hashMapOf()
                            if (!members!!.contains(uid))
                                finish()

                            databaseRef.getReference("/GroupChats/$groupId/prevMembers")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
                                            val genericType = object :
                                                GenericTypeIndicator<HashMap<String, String>>() {}
                                            memberNames = snapshot.getValue(genericType)
                                        }

                                        var i = 0
                                        for (mem in members!!) {
                                            databaseRef.getReference("/users/$mem/username")
                                                .addListenerForSingleValueEvent(object :
                                                    ValueEventListener {
                                                    override fun onDataChange(snapshot: DataSnapshot) {
                                                        if (snapshot.exists()) {
                                                            memberNames?.put(
                                                                mem,
                                                                snapshot.getValue(String::class.java)!!
                                                            )
                                                        }
                                                        i++
                                                        if (i == members!!.size) {
                                                            if (!listened) {
                                                                listened = true
                                                                listenMessages()
                                                            }
                                                        }
                                                    }

                                                    override fun onCancelled(error: DatabaseError) {
                                                    }
                                                })
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                    }
                                })
                        }
                    }
                    else{
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        friendChatProfilePhoto.setOnClickListener {
            if (admin == uid) {
                enteredMessage.clearFocus() // Clear focus from EditText
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(enteredMessage.windowToken, 0)
                val intent = Intent(this, GroupAdminProfilePage::class.java)
                intent.putExtra("GROUP_ID", groupId)
                startActivity(intent)
            }
            else {
                enteredMessage.setText("")
                enteredMessage.clearFocus() // Clear focus from EditText
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(enteredMessage.windowToken, 0)

                val intent = Intent(this, GroupProfilePage::class.java)
                intent.putExtra("GROUP_ID", groupId)
                startActivity(intent)
            }
        }

        sendIcon.setOnClickListener{
            val text = enteredMessage.text.toString().trimEnd()
            if(text!="") {
                enteredMessage.setText("")
                val ref = databaseRef.getReference("/GroupChats/${groupId}/Messages").push()
                val message = IndividualMessage(encrypt(text), uid!!)
                ref.setValue(message).addOnSuccessListener {
                    recyclerChatLog.scrollToPosition(groupAdapter.itemCount - 1)
                    val time = Timestamp.now().seconds
                    for(member in members!!){
                        databaseRef.getReference("/users/$member/chats/$groupId/time").setValue(time)
                        databaseRef.getReference("/users/$member/chats/$groupId/read").setValue(false)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getKeyFromString(encodedKeyString: String): SecretKey {
        val decodedKey = Base64.getDecoder().decode(encodedKeyString)
        return SecretKeySpec(decodedKey, "AES")
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Gets the all messages in the group rom the firebase
    private fun listenMessages(){
        var i = 0
        databaseRef.getReference("/GroupChats/${groupId}/Messages")
            .addChildEventListener(object: ChildEventListener{
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if(snapshot.exists()) {
                        val message = snapshot.getValue(IndividualMessage::class.java)

                        if (message != null){
                            if (uid == message.senderId) {
                                groupAdapter.add(FriendChatToItem(decrypt(message.message!!)))
                                if(i<0)
                                    recyclerChatLog.scrollToPosition(groupAdapter.itemCount - 1)
                            }
                            else {
                                val sender = memberNames?.get(message.senderId)
                                groupAdapter.add(GroupChatFromItem(decrypt(message.message!!), sender!!))
                            }
                            i++
                            if(i==messageCount) {
                                recyclerChatLog.scrollToPosition(groupAdapter.itemCount - 1)
                                i = -99999999
                            }
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
        databaseRef.getReference("/users/$uid/chats/$groupId/read").removeEventListener(readListener)
    }
}

// Class to display the messages coming from other users
class GroupChatFromItem(val text: String, val username:String): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.findViewById<TextView>(R.id.textViewFromRow).text = text
        viewHolder.itemView.findViewById<TextView>(R.id.chat_row_username).text = username
    }

    override fun getLayout(): Int {
        return R.layout.group_chat_from_row
    }
}