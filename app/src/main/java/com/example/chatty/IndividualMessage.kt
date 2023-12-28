package com.example.chatty

import com.google.firebase.Timestamp

// Class to store the message information
class IndividualMessage(var id: String, var message: String?, var photoURI: String?, var senderId: String, var time: Timestamp?) {
    constructor() : this("", "", null, "", null)
}