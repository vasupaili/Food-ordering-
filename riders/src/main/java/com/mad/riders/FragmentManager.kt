package com.mad.riders

import android.Manifest
import android.app.ActivityManager
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.mad.mylibrary.SharedClass.RIDERS_PATH
import com.mad.mylibrary.SharedClass.ROOT_UID
import com.mad.mylibrary.User
import com.mad.riders.NavigationFragments.Home
import com.mad.riders.NavigationFragments.Orders
import com.mad.riders.NavigationFragments.Profile
import com.mad.riders.Services.LocationService
import java.util.Objects

class FragmentManager constructor() : AppCompatActivity(), Orders.OnFragmentInteractionListener,
    Home.OnFragmentInteractionListener, Profile.OnFragmentInteractionListener {
    var value: Boolean = false
    private var mLocationPermissionGranted: Boolean = false
    private var navigation: BottomNavigationView? = null
    private val mOnNavigationItemSelectedListener: BottomNavigationView.OnNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item: MenuItem ->
            when (item.getItemId()) {
                R.id.navigation_home -> {
                    if (!(getSupportFragmentManager().findFragmentById(R.id.fragment_container) is Home)) {
                        checkBadge()
                        getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, Home()).commit()
                    }
                    return@OnNavigationItemSelectedListener true
                }

                R.id.navigation_profile -> {
                    if (!(getSupportFragmentManager().findFragmentById(R.id.fragment_container) is Profile)) {
                        val bundle: Bundle = Bundle()
                        bundle.putString("UID", ROOT_UID)
                        val profile: Profile = Profile()
                        profile.setArguments(bundle)
                        checkBadge()
                        getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, profile).commit()
                    }
                    return@OnNavigationItemSelectedListener true
                }

                R.id.navigation_reservation -> {
                    if (!(getSupportFragmentManager().findFragmentById(R.id.fragment_container) is Orders)) {
                        val bundle2: Bundle = Bundle()
                        bundle2.putString("UID", ROOT_UID)
                        if (value) bundle2.putString(
                            "STATUS",
                            "true"
                        ) else bundle2.putString("STATUS", "false")
                        val orders: Orders = Orders()
                        orders.setArguments(bundle2)
                        getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, orders).commit()
                        hideBadgeView()
                    }
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        }
    private var notificationBadge: View? = null
    private var serviceIntent: Intent? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app)
        navigation = findViewById<View>(R.id.navigation) as BottomNavigationView?
        navigation!!.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        val databases: FirebaseDatabase = FirebaseDatabase.getInstance()
        val myRef: DatabaseReference = databases.getReference(RIDERS_PATH).child(ROOT_UID)
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            public override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                if (dataSnapshot.exists()) {
                    val user: User?
                    user = dataSnapshot.child("rider_info").getValue(User::class.java)
                    if (user == null) {
                        val auth: FirebaseAuth = FirebaseAuth.getInstance()
                        auth.signOut()
                        ROOT_UID=""
                        Toast.makeText(
                            this@FragmentManager,
                            "Please Login Again",
                            Toast.LENGTH_SHORT
                        ).show()
                        val intent = Intent(this@FragmentManager, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }

            public override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w("MAIN", "Failed to read value.", error.toException())
            }
        })

        //TODO: MAP PERMISSION
        if (checkMapServices()) {
            if (mLocationPermissionGranted) {
                // TODO:
            } else {
                locationPermission
            }
        }
        startLocationService()
        value = true
        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        val status: DatabaseReference =
            database.getReference(RIDERS_PATH + "/" + ROOT_UID + "/available/")
        status.addValueEventListener(object : ValueEventListener {
            public override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    value = dataSnapshot.getValue() as Boolean
                }
            }

            public override fun onCancelled(databaseError: DatabaseError) {}
        })
        checkBadge()
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(
                R.id.fragment_container,
                Home()
            ).commit()

            addBadgeView()
            hideBadgeView()
        }
    }

    public override fun onFragmentInteraction(uri: Uri?) {}
    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
        stopService(serviceIntent)
    }

    private fun checkBadge() {
        val query: Query =
            FirebaseDatabase.getInstance().getReference(RIDERS_PATH + "/" + ROOT_UID + "/pending/")
        query.addValueEventListener(object : ValueEventListener {
            public override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val count: Long = dataSnapshot.getChildrenCount()
                    (notificationBadge!!.findViewById<View>(R.id.count_badge) as TextView).setText(
                        java.lang.Long.toString(count)
                    )
                    refreshBadgeView()
                } else {
                    hideBadgeView()
                }
            }

            public override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun addBadgeView() {
        val menuView: BottomNavigationMenuView =
            navigation!!.getChildAt(0) as BottomNavigationMenuView
        val itemView: BottomNavigationItemView = menuView.getChildAt(2) as BottomNavigationItemView
        notificationBadge =
            LayoutInflater.from(this).inflate(R.layout.notification_badge, menuView, false)
        itemView.addView(notificationBadge)
    }

    private fun refreshBadgeView() {
        notificationBadge!!.setVisibility(View.VISIBLE)
    }

    private fun hideBadgeView() {
        notificationBadge!!.setVisibility(View.INVISIBLE)
    }

    //Functions for permissions check
    private fun checkMapServices(): Boolean {
        if (isServicesOK) {
            if (isMapsEnabled) {
                return true
            }
        }
        return false
    }

    val isServicesOK: Boolean
        get() {
            Log.d("TAG", "isServicesOK: checking google services version")
            val available: Int = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(this@FragmentManager)
            if (available == ConnectionResult.SUCCESS) {
                //everything is fine and the user can make map requests
                Log.d("TAG", "isServicesOK: Google Play Services is working")
                return true
            } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
                //an error occured but we can resolve it
                Log.d("TAG", "isServicesOK: an error occured but we can fix it")
                val dialog: Dialog? = GoogleApiAvailability.getInstance()
                    .getErrorDialog(this@FragmentManager, available, ERROR_DIALOG_REQUEST)
                dialog!!.show()
            } else {
                Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show()
            }
            return false
        }
    private val locationPermission: Unit
        private get() {
            /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
            if ((ContextCompat.checkSelfPermission(
                    getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                        == PackageManager.PERMISSION_GRANTED)
            ) {
                mLocationPermissionGranted = true
                //getChatrooms();
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                )
            }
        }
    val isMapsEnabled: Boolean
        get() {
            val manager: LocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                buildAlertMessageNoGps()
                return false
            }
            return true
        }

    private fun buildAlertMessageNoGps() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes", object : DialogInterface.OnClickListener {
                public override fun onClick(
                    @Suppress("unused") dialog: DialogInterface,
                    @Suppress("unused") id: Int
                ) {
                    val enableGpsIntent: Intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS)
                }
            })
        val alert: AlertDialog = builder.create()
        alert.show()
    }

    private fun startLocationService() {
        if (!isLocationServiceRunning) {
            serviceIntent = Intent(this, LocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    private val isLocationServiceRunning: Boolean
        private get() {
            val manager: ActivityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            for (service: ActivityManager.RunningServiceInfo in manager.getRunningServices(Int.MAX_VALUE)) {
                if (("com.example.rider_map.services.LocationService" == service.service.getClassName())) {
                    Log.d("DEBUG", "isLocationServiceRunning: location service is already running.")
                    return true
                }
            }
            Log.d("DEBUG", "isLocationServiceRunning: location service is not running.")
            return false
        }

    companion object {
        val ERROR_DIALOG_REQUEST: Int = 9001
        val PERMISSIONS_REQUEST_ENABLE_GPS: Int = 9002
        val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: Int = 9003
    }
}