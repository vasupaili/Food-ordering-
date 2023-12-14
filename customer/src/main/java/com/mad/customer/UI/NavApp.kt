package com.mad.customer.UI

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hsalf.smilerating.SmileRating
import com.mad.customer.R
import com.mad.customer.UIimport.MainActivity

import com.mad.mylibrary.ReviewItem
import com.mad.mylibrary.SharedClass
import com.mad.mylibrary.SharedClass.CUSTOMER_PATH
import com.mad.mylibrary.SharedClass.RESTAURATEUR_INFO
import com.mad.mylibrary.SharedClass.ROOT_UID
import com.mad.mylibrary.SharedClass.STATUS_DELIVERED
import com.mad.mylibrary.SharedClass.STATUS_DELIVERING
import com.mad.mylibrary.SharedClass.STATUS_DISCARDED
import com.mad.mylibrary.SharedClass.orderToTrack
import com.mad.mylibrary.SharedClass.user
import com.mad.mylibrary.StarItem
import com.mad.mylibrary.User


class NavApp : AppCompatActivity(), Restaurant.OnFragmentInteractionListener,
    Profile.OnFragmentInteractionListener, Order.OnFragmentInteractionListener {
    private lateinit var order_to_listen: SharedPreferences
    private val mOnNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    //onRefuseOrder();
                    if (supportFragmentManager.findFragmentById(R.id.fragment_container) !is Restaurant) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, Restaurant()).commit()
                    }
                    return@OnNavigationItemSelectedListener true
                }

                R.id.navigation_profile -> {
                    //onRefuseOrder();
                    if (supportFragmentManager.findFragmentById(R.id.fragment_container) !is Profile) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, Profile()).commit()
                    }
                    return@OnNavigationItemSelectedListener true
                }

                R.id.navigation_reservation -> {
                    //onRefuseOrder();
                    if (supportFragmentManager.findFragmentById(R.id.fragment_container) !is Order) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, Order()).commit()
                    }
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav_app)
        val navigation = findViewById<View>(R.id.navigation) as BottomNavigationView
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
       // val toolbar =findViewById<Toolbar>(R.id.toolbar)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(
                R.id.fragment_container,
                Restaurant()
            ).commit()
        }

        //Get the hashMap from sharedPreferences
        order_to_listen = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE)
        val gson = Gson()
        val storedHashMapString = order_to_listen.getString("HashMap", null)
        val type = object : TypeToken<HashMap<String?, Int?>?>() {}.type

        //orderToTrack = gson.fromJson(storedHashMapString, type)
        orderToTrack = gson.fromJson(storedHashMapString, type) ?: HashMap()

        userInfo
    }

    val userInfo: Unit
        get() {
            val myRef = FirebaseDatabase.getInstance().getReference(CUSTOMER_PATH)
                .child(ROOT_UID)
            myRef.addListenerForSingleValueEvent(object : ValueEventListener {
                @SuppressLint("SuspiciousIndentation")
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if(dataSnapshot.exists()) {
                        user = dataSnapshot.child("customer_info").getValue(
                            User::class.java
                        )
                        Log.d("user", "" + user)
                    }else{
                        val auth: FirebaseAuth = FirebaseAuth.getInstance()
                        Toast.makeText(this@NavApp, "Please Login Again", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@NavApp, MainActivity::class.java)
                        startActivity(intent)
                        auth.signOut()
                        ROOT_UID=""
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Failed to read value
                    Log.w("MAIN", "Failed to read value.", error.toException())
                }
            })
        }

    private fun onRefuseOrder() {
        if (orderToTrack != null) {
            for (entry in orderToTrack.entries) {
                val query: Query =
                    FirebaseDatabase.getInstance().getReference(CUSTOMER_PATH)
                        .child(ROOT_UID).child("orders").child(entry.key)
                query.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            val changed_statusi = dataSnapshot.child("status").value as Long?
                            val changed_status = changed_statusi!!.toInt()
                            if (changed_status != entry.value) {
                                if (changed_status == STATUS_DISCARDED) {
                                    entry.setValue(changed_status)
                                    orderToTrack.replace(entry.key, changed_status)
                                    showAlertDialogDiscarded(
                                        dataSnapshot.child("key").value as String?,
                                        dataSnapshot.key
                                    )
                                } else if (changed_status == STATUS_DELIVERING) {
                                    entry.setValue(changed_status)
                                    orderToTrack.replace(entry.key, changed_status)
                                } else if (changed_status ==STATUS_DELIVERED) {
                                    entry.setValue(changed_status)
                                    orderToTrack.replace(entry.key, changed_status)
                                    setRated(dataSnapshot.key, false)
                                    showAlertDialogDelivered(
                                        dataSnapshot.child("key").value as String?,
                                        dataSnapshot.key
                                    )
                                }
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
            }
        }
    }

    private fun showAlertDialogDiscarded(resKey: String?, orderKey: String?) {
        val query: Query =
            FirebaseDatabase.getInstance().getReference(RESTAURATEUR_INFO).child(
                resKey!!
            ).child("info")
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val alertDialog = AlertDialog.Builder(this@NavApp).create()
                val factory = LayoutInflater.from(this@NavApp)
                val view = factory.inflate(R.layout.discarded_dialog, null)
                alertDialog.setView(view)
                if (dataSnapshot.child("photoUri").exists()) {
                    Glide.with(view).load(dataSnapshot.child("photoUri").value)
                        .into((view.findViewById<View>(R.id.dialog_discarded_rating_icon) as ImageView))
                }
                (view.findViewById<View>(R.id.discarded_res_name) as TextView).text =
                    dataSnapshot.child("name").value as String?
                alertDialog.show()
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun setRated(orderKey: String?, bool: Boolean) {
        val myRef2 = FirebaseDatabase.getInstance().getReference(CUSTOMER_PATH)
            .child(ROOT_UID).child("orders").child(
            orderKey!!
        )
        val rated = HashMap<String, Any>()
        rated["rated"] = bool
        myRef2.updateChildren(rated)
    }

    private fun showAlertDialogDelivered(resKey: String?, orderKey: String?) {
        val query: Query =
            FirebaseDatabase.getInstance().getReference(RESTAURATEUR_INFO).child(
                resKey!!
            ).child("info")
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val alertDialog = AlertDialog.Builder(this@NavApp).create()
                val factory = LayoutInflater.from(this@NavApp)
                val view = factory.inflate(R.layout.rating_dialog, null)
                alertDialog.setView(view)
                if (dataSnapshot.child("photoUri").exists()) {
                    Glide.with(view).load(dataSnapshot.child("photoUri").value)
                        .into((view.findViewById<View>(R.id.dialog_rating_icon) as ImageView))
                }
                val smileRating =
                    view.findViewById<View>(R.id.dialog_rating_rating_bar) as SmileRating
                //Button confirm pressed
                view.findViewById<View>(R.id.dialog_rating_button_positive)
                    .setOnClickListener { a: View? ->
                        if (smileRating.rating != 0) {
                            val myRef = FirebaseDatabase.getInstance()
                                .getReference(RESTAURATEUR_INFO + "/" + resKey)
                                .child("review")
                            val review = HashMap<String?, Any>()
                            val comment =
                                (view.findViewById<View>(R.id.dialog_rating_feedback) as EditText).text.toString()
                            val rate_key = myRef.push().key
                            updateRestaurantStars(resKey, smileRating.rating)
                            if (!comment.isEmpty()) {
                                setRated(orderKey, true)
                                review[rate_key] = ReviewItem(
                                    smileRating.rating,
                                    comment,
                                    ROOT_UID,
                                   user!!.photoPath,
                                   user!!.name
                                )
                                myRef.updateChildren(review)
                            } else {
                                setRated(orderKey, true)
                                review[rate_key] = ReviewItem(
                                    smileRating.rating,
                                    null,
                                    ROOT_UID,
                                    user!!.photoPath,
                                   user!!.name
                                )
                                myRef.updateChildren(review)
                            }
                            Toast.makeText(
                                applicationContext,
                                "Thanks for your review!",
                                Toast.LENGTH_LONG
                            ).show()
                            alertDialog.dismiss()
                        } else {
                            Toast.makeText(
                                applicationContext,
                                "You forgot to rate!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                view.findViewById<View>(R.id.dialog_rating_button_negative)
                    .setOnClickListener { b: View? -> alertDialog.dismiss() }
                alertDialog.show()
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun updateRestaurantStars(resKey: String?, stars: Int) {
        val query: Query =
            FirebaseDatabase.getInstance().getReference(RESTAURATEUR_INFO).child(
                resKey!!
            ).child("stars")
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val star = HashMap<String, Any>()
                val myRef = FirebaseDatabase.getInstance()
                    .getReference(RESTAURATEUR_INFO + "/" + resKey)
                if (dataSnapshot.exists()) {
                    val s = (dataSnapshot.child("tot_stars").value as Long?)!!.toInt()
                    val p = (dataSnapshot.child("tot_review").value as Long?)!!.toInt()
                    star["stars"] = StarItem(s + stars, p + 1, -s - stars)
                    myRef.updateChildren(star)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    override fun onFragmentInteraction(uri: Uri?) {}
    override fun onRestart() {
        super.onRestart()
    }

    override fun onResume() {
        super.onResume()
        onRefuseOrder()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        val gson = Gson()
        order_to_listen = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE)
        val mapString = gson.toJson(orderToTrack)
        order_to_listen.edit().putString("HashMap", mapString).apply()
        super.onStop()
    }

    companion object {
        const val PREFERENCE_NAME = "ORDER_LIST"
    }
}