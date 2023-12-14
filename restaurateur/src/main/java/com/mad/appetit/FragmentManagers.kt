package com.mad.appetitimport

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.mad.appetit.DishesActivitiesimport.DailyOffer
import com.mad.appetit.HomeActivities.Home
import com.mad.appetit.HomeActivities.HomeStats
import com.mad.appetit.OrderActivities.Order
import com.mad.appetit.OrderActivities.Reservation
import com.mad.appetit.OrderActivitiesimport.PagerAdapterOrder
import com.mad.appetit.ProfileActivitiesimport.PagerAdapterProfile
import com.mad.appetit.ProfileActivitiesimport.Profile
import com.mad.appetit.ProfileActivitiesimport.Rating

import com.mad.appetit.R
import com.mad.appetit.Startup.MainActivity
import com.mad.mylibrary.Restaurateur
import com.mad.mylibrary.SharedClass
import com.mad.mylibrary.SharedClass.RESERVATION_PATH
import com.mad.mylibrary.SharedClass.RESTAURATEUR_INFO
import com.mad.mylibrary.SharedClass.ROOT_UID
import com.mad.mylibrary.User

class FragmentManagers : AppCompatActivity(), DailyOffer.OnFragmentInteractionListener,
    Reservation.OnFragmentInteractionListener, Order.OnFragmentInteractionListener,
    Home.OnFragmentInteractionListener, Profile.OnFragmentInteractionListener,
    HomeStats.OnFragmentInteractionListener, PagerAdapterOrder.OnFragmentInteractionListener,
    PagerAdapterProfile.OnFragmentInteractionListener, Rating.OnFragmentInteractionListener {
    private var notificationBadge: View? = null
    private lateinit var navigation: BottomNavigationView
    private val mOnNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    if (supportFragmentManager.findFragmentById(R.id.fragment_container) !is Home) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, Home()).commit()
                    }
                    return@OnNavigationItemSelectedListener true
                }

                R.id.navigation_profile -> {
                    if (supportFragmentManager.findFragmentById(R.id.fragment_container) !is PagerAdapterProfile) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, PagerAdapterProfile()).commit()
                    }
                    return@OnNavigationItemSelectedListener true
                }

                R.id.navigation_dailyoffer -> {
                    if (supportFragmentManager.findFragmentById(R.id.fragment_container) !is DailyOffer) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, DailyOffer()).commit()
                    }
                    return@OnNavigationItemSelectedListener true
                }

                R.id.navigation_reservation -> {
                    if (supportFragmentManager.findFragmentById(R.id.fragment_container) !is PagerAdapterOrder) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, PagerAdapterOrder()).commit()
                    }
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_manager)
        navigation = findViewById(R.id.navigation)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(
                R.id.fragment_container,
                Home()
            ).commit()
        }
        val myRef = FirebaseDatabase.getInstance().reference
        val query: Query = myRef.child(RESTAURATEUR_INFO + "/" + ROOT_UID).child("info")
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val restaurateur = dataSnapshot.getValue(
                        Restaurateur::class.java
                    )

                }else{
                    val auth: FirebaseAuth = FirebaseAuth.getInstance()
                    Toast.makeText(this@FragmentManagers, "Please Login Again", Toast.LENGTH_SHORT).show()

                    auth.signOut()
                    ROOT_UID=""
                    val intent = Intent(this@FragmentManagers, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("DAILY OFFER", "Failed to read value.", error.toException())
            }
        })
        addBadgeView()
        hideBadgeView()
    }

    override fun onResume() {
        super.onResume()
        checkBadge()
    }

    private fun checkBadge() {
        val query: Query = FirebaseDatabase.getInstance().getReference(
            RESTAURATEUR_INFO + "/" + ROOT_UID
                    + "/" + RESERVATION_PATH
        )
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val count = dataSnapshot.childrenCount
                    (notificationBadge!!.findViewById<View>(R.id.count_badge) as TextView).text =
                        java.lang.Long.toString(count)
                    refreshBadgeView()
                } else {
                    hideBadgeView()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun addBadgeView() {
        val menuView = navigation!!.getChildAt(0) as BottomNavigationMenuView
        val itemView = menuView.getChildAt(3) as BottomNavigationItemView
        notificationBadge =
            LayoutInflater.from(this).inflate(R.layout.notification_badge, menuView, false)
        itemView.addView(notificationBadge)
    }

    private fun refreshBadgeView() {
        notificationBadge!!.visibility = View.VISIBLE
    }

    private fun hideBadgeView() {
        notificationBadge!!.visibility = View.INVISIBLE
    }

    override fun onFragmentInteraction(uri: Uri?) {}
}