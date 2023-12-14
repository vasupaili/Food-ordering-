package com.mad.mylibrary

class ReviewItem {
    @JvmField
    var stars = 0
    @JvmField
    var comment: String? = null
    var user_key: String? = null
    @JvmField
    var img: String? = null
    @JvmField
    var name: String? = null

    constructor()
    constructor(stars: Int, comment: String?, user_key: String?, img: String?, name: String?) {
        this.stars = stars
        this.comment = comment
        this.user_key = user_key
        this.img = img
        this.name = name
    }
}