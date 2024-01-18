package com.example.chatty

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Class to store chat information
@Parcelize
class IndividualChat(val id: String, val user1: String, val user2: String) : Parcelable {
    lateinit var key: String
    constructor(): this("", "", "")
}