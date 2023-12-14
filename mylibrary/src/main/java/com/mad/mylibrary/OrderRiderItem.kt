package com.mad.mylibrary

class OrderRiderItem {
    var keyRestaurant: String? = null
        private set
    var keyCustomer: String? = null
        private set
    var addrCustomer: String? = null
        private set
    var addrRestaurant: String? = null
        private set
    var totPrice: String? = null
        private set
    var time: Long? = null
        private set

    constructor()
    constructor(
        keyRestaurant: String?,
        keyCustomer: String?,
        addrCustomer: String?,
        addrRestaurant: String?,
        time: Long?,
        totPrice: String?
    ) {
        this.keyRestaurant = keyRestaurant
        this.keyCustomer = keyCustomer
        this.addrCustomer = addrCustomer
        this.addrRestaurant = addrRestaurant
        this.time = time
        this.totPrice = totPrice
    }
}