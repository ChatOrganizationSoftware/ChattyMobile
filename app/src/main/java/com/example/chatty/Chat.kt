package com.example.chatty


class Chat(var id: String, var group: Boolean = false) {
    var read: Boolean = true
    var name: String = ""
    var photoURI: String? = ""
    var friendId: String? = null
    var time: Long? = null

    constructor(id: String, group: Boolean, time:Long): this(id, group){
        this.time = time
    }
}