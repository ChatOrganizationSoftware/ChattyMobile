package com.example.chatty

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Group(val groupId: String, var name: String, var groupPhoto:String = "",
             var about: String = "Hello there", var visibility: String ="Public" , var members: MutableList<User> = mutableListOf()
): Parcelable {
    constructor() : this("", "", "", "", "", mutableListOf())
}