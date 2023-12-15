package com.example.chatty

import com.google.firebase.Timestamp


class IndividualMessage(val id: String, val message: String, val senderId: String, val time: Timestamp?) {
    constructor() : this("", "", "", null)
}