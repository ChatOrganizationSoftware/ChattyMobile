package com.example.chatty

// Class to store the message information
class IndividualMessage(var id: String, var message: String?, var photoURI: String?, var senderId: String) {
    constructor() : this("", "", null, "")
}