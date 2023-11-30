package com.example.chatty

// Properties stored in database for a user
class User {
    public var userId: String=""
    public var userName: String=""
    public var profileImage: String=""
    public var chats: ArrayList<String> = ArrayList<String>()
    public var about: String = ""

    constructor(userId:String, userName: String){
        this.userId = userId
        this.userName = userName
    }
}