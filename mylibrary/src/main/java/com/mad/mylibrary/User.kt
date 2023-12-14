package com.mad.mylibrary

class User {
    var username: String
    @JvmField
    var name: String
    @JvmField
    var surname: String
    @JvmField
    var email: String
    @JvmField
    var phone: String
    var addr: String
    @JvmField
    var photoPath: String?

    constructor() {
        username = ""
        name = ""
        surname = ""
        email = ""
        phone = ""
        addr = ""
        photoPath = null
    }

    constructor(
        username: String,
        name: String,
        surname: String,
        email: String,
        phone: String,
        addr: String,
        photoPath: String?
    ) {
        this.username = username
        this.name = name
        this.surname = surname
        this.email = email
        this.phone = phone
        this.addr = addr
        this.photoPath = photoPath
    }
}