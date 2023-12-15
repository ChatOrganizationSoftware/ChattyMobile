package com.example.chatty

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class IndividualChat(val id: String, val user1: String, val user2: String) : Parcelable {
    constructor(): this("", "", "")
}