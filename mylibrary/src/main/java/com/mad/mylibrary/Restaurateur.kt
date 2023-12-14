package com.mad.mylibrary

import java.io.Serializable

class Restaurateur : Serializable {
    var mail: String
        private set
    var name: String
        private set
    var addr: String
        private set
    var cuisine: String
        private set
    var openingTime: String? = null
        private set
    var phone: String
        private set
    var photoUri: String?
        private set

    constructor() {
        mail = ""
        name = ""
        addr = ""
        cuisine = ""
        phone = ""
        photoUri = null
    }

    constructor(
        mail: String,
        name: String,
        addr: String,
        cuisine: String,
        openingTime: String?,
        phone: String,
        photoUri: String?
    ) {
        this.mail = mail
        this.name = name
        this.addr = addr
        this.cuisine = cuisine
        this.openingTime = openingTime
        this.phone = phone
        this.photoUri = photoUri
    }
}