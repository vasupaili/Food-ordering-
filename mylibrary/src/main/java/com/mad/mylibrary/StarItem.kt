package com.mad.mylibrary

class StarItem {
    var tot_stars = 0
    var tot_review = 0
    var sort = 0

    constructor()
    constructor(tot_stars: Int, tot_review: Int, sort: Int) {
        this.tot_stars = tot_stars
        this.tot_review = tot_review
        this.sort = sort
    }
}