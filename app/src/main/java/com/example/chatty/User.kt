package com.example.chatty

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class User(val userId: String, var username: String, var profilePhoto:String = "",
           var about: String = "Hello there", var visibility: String ="Public" ): Parcelable {
    constructor() : this("", "", "", "", "")
}