package com.example.chatty


class Chat(var id: String, var group: Boolean) {
    var read: Boolean = true
    var name: String? = null
    var photoURI: String? = null
    var friendId: String? = null
    var time: Long? = null

    constructor(id: String, group: Boolean, time:Long): this(id, group){
        this.time = time
    }
}