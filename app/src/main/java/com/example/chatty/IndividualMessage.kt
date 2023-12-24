package com.example.chatty

import com.google.firebase.Timestamp

// Class to store the message information
class IndividualMessage(val id: String, val message: String, val senderId: String, val time: Timestamp?) {
    constructor() : this("", "", "", null)
}