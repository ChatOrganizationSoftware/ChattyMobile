package com.example.chatty

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Class to store group informations
@Parcelize
class Group(val groupId: String, var adminId: String, var name: String, var groupPhoto:String = "",
             var about: String = "Hello there" ,var members: MutableList<String> = mutableListOf()
): Parcelable {
    var latestMessage: String? = null
    constructor() : this("", "", "", "", "", mutableListOf())
}