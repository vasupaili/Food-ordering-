package com.mad.mylibrary

object SharedClass {
    /**
     * Key for onSaveInstanceState() and onRestoreInstanceState()
     */
    const val Name = "keyName"
    const val Password = "keyPassword"
    const val Description = "keyDescription"
    const val Address = "keyAddress"
    const val Mail = "keyMail"
    const val Price = "keyEuroPrice"
    const val Photo = "keyPhoto"
    const val Phone = "keyPhone"
    const val Time = "keyTime"
    const val Quantity = "keyQuantity"
    const val CameraOpen = "keyCameraDialog"
    const val PriceOpen = "keyPriceDialog"
    const val QuantOpen = "keyQuantityDialog"
    const val TimeClose = "keyTimeClose"
    const val TimeOpen = "keyTimeOpen"

    /**
     * Status of an order
     */
    const val STATUS_UNKNOWN = 1000
    const val STATUS_DELIVERING = 1001
    const val STATUS_DELIVERED = 1002
    const val STATUS_DISCARDED = 1003

    /**
     * Useful values key to retrieve data from activity (Intent)
     */
    const val EDIT_EXISTING_DISH = "DISH_NAME"
    const val ORDER_ID = "ORDER_ID"
    const val CUSTOMER_ID = "CUSTOMER_ID"

    /**
     * Permission values
     */
    const val PERMISSION_GALLERY_REQUEST = 1
    const val GOOGLE_SIGIN = 101
    const val SIGNUP = 102

    /**
     * Firebase paths
     */
    @JvmField
    var ROOT_UID = ""
    var user: User? = null
    const val RESTAURATEUR_INFO = "/restaurants"
    const val DISHES_PATH = "/dishes"
    const val RESERVATION_PATH = "/reservation"
    const val ACCEPTED_ORDER_PATH = "/order"
    const val RESTAURATEUR_REVIEW = "/reviews"
    const val RIDERS_PATH = "/riders"
    const val RIDERS_ORDER = "/pending"
    const val CUSTOMER_PATH = "/customers"
    const val CUSTOMER_FAVOURITE_RESTAURANT_PATH = "/favourites"

    /**
     * List of orders for a customer
     */
  //  var orderToTrack = HashMap<String, Int>()
    var orderToTrack: HashMap<String, Int> = HashMap()

}