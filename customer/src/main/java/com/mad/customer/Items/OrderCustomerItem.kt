package com.mad.customer.Itemsimport

import java.io.Serializable



class OrderCustomerItem : Serializable {
    var key: String? = null
    var addrCustomer: String? = null
    var totPrice: String? = null
    var dishes: HashMap<String, Int>? = null //key = dish name, value = quantity
    var time: Long? = null
    var sort: Long? = null
    var status: Int? = null
    var isRated: Boolean = false

    constructor()
    constructor(
        key: String?,
        addrCustomer: String?,
        totPrice: String?,
        dishes: HashMap<String, Int>?,
        time: Long?,
        sort: Long?,
        status: Int?,
        rated: Boolean
    ) {
        this.key = key
        this.addrCustomer = addrCustomer
        this.totPrice = totPrice
        this.dishes = dishes
        this.time = time
        this.sort = sort
        this.status = status
        isRated = rated
    }
}