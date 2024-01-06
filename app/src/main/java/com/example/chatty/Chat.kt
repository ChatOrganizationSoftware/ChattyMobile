package com.example.chatty


class Chat(var id: String, var group: Boolean, var time: Long) {
    var read: Boolean = true
    var name: String? = null
}