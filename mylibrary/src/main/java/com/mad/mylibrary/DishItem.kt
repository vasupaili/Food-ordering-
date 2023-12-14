package com.mad.mylibrary

class DishItem {
    @JvmField
    var name: String
    @JvmField
    var desc: String
    @JvmField
    var price: Float
    @JvmField
    var quantity: Int
    @JvmField
    var photo: String?
    @JvmField
    var frequency: Int

    constructor() {
        name = ""
        desc = ""
        price = 0f
        quantity = 0
        photo = null
        frequency = 0
    }

    constructor(
        name: String,
        desc: String,
        price: Float,
        quantity: Int,
        photo: String?,
        frequency: Int
    ) {
        this.name = name
        this.desc = desc
        this.price = price
        this.quantity = quantity
        this.photo = photo
        this.frequency = frequency
    }
}