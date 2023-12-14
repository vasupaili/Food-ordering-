package com.mad.customer.Itemsimport

class DishItem {
    var name: String
    var desc: String
    var price: Float
    var quantity: Int
    var photoUri: String?

    constructor() {
        name = ""
        desc = ""
        price = -1f
        quantity = -1
        photoUri = null
    }

    constructor(name: String, desc: String, price: Float, quantity: Int, photo: String?) {
        this.name = name
        this.desc = desc
        this.price = price
        this.quantity = quantity
        photoUri = photo
    }
}