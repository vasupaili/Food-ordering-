package com.mad.mylibrary

class Position {
    var latitude: Double? = null
    var longitude: Double? = null

    constructor()
    constructor(latitude: Double?, longitude: Double?) {
        this.latitude = latitude
        this.longitude = longitude
    }
}