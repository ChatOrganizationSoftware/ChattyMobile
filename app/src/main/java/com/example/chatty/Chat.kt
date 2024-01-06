package com.example.chatty

import com.google.firebase.Timestamp

class Chat(var id: String, var group: Boolean, var time: Long) {
    var name: String? = null
    var photoURI: String? = null
}