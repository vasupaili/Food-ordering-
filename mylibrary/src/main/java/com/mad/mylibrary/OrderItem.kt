package com.mad.mylibrary

import java.io.Serializable

class OrderItem : Serializable {
    @JvmField
    var key: String? = null
    @JvmField
    var addrCustomer: String? = null
    @JvmField
    var totPrice: String? = null
    @JvmField
    var dishes: HashMap<String, Int>? = null //key = dish name, value = quantity
    @JvmField
    var time: Long? = null
    var status: Int? = null

    constructor()
    constructor(
        key: String?,
        addrCustomer: String?,
        totPrice: String?,
        status: Int?,
        dishes: HashMap<String, Int>?,
        time: Long?
    ) {
        this.key = key
        this.addrCustomer = addrCustomer
        this.totPrice = totPrice
        this.status = status
        this.dishes = dishes
        this.time = time
    }
}