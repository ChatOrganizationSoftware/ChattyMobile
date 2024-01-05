package com.example.chatty

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Class to store user information
@Parcelize
class User(val userId: String, var username: String, var profilePhoto:String = "",
           var about: String = "Hello there", var visibility: String ="Public" ): Parcelable {
    var active: Boolean? = null
    constructor() : this("", "", "", "", "")
}