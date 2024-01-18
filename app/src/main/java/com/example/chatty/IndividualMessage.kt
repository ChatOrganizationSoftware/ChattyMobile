package com.example.chatty

// Class to store the message information
class IndividualMessage(var message: String?, var senderId: String) {
    constructor() : this("", "")
}