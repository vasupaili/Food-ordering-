package com.mad.riders.Services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.FirebaseDatabase
import com.mad.mylibrary.SharedClass.RIDERS_PATH
import com.mad.mylibrary.SharedClass.ROOT_UID

class LocationService : Service() {
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private val database = FirebaseDatabase.getInstance()
    private val mDatabaseRef = database.reference
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        //TODO add service icon
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "my_channel_01"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "My Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("").build()
            startForeground(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: called.")
        location
        return START_NOT_STICKY
    }

    private val location: Unit
        private get() {

            // ---------------------------------- LocationRequest ------------------------------------
            // Create the location request to start receiving updates
            val mLocationRequestHighAccuracy = LocationRequest()
            mLocationRequestHighAccuracy.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            mLocationRequestHighAccuracy.interval = UPDATE_INTERVAL
            mLocationRequestHighAccuracy.fastestInterval = FASTEST_INTERVAL


            // new Google API SDK v11 uses getFusedLocationProviderClient(this)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "getLocation: stopping the location service.")
                stopSelf()
                return
            }
            Log.d(TAG, "getLocation: getting location information.")
            mFusedLocationClient!!.requestLocationUpdates(
                mLocationRequestHighAccuracy, object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        Log.d(TAG, "onLocationResult: got location result.")
                        val location = locationResult.lastLocation
                        if (location != null) {
                            val pos = LatLng(location.latitude, location.longitude)
                            updateLocationDatabase(location.latitude, location.longitude)
                            Log.d(
                                TAG,
                                "saved location " + location.latitude + "  " + location.longitude
                            )
                        }
                    }
                },
                Looper.myLooper()
            ) // Looper.myLooper tells this to repeat forever until thread is destroyed
        }

    private fun updateLocationDatabase(latitude: Double, longitude: Double) {
        mDatabaseRef.child(RIDERS_PATH).child(ROOT_UID).child("rider_pos").child("latitude")
            .setValue(latitude)
        mDatabaseRef.child(RIDERS_PATH).child(ROOT_UID).child("rider_pos").child("longitude")
            .setValue(longitude)
    }

    companion object {
        private const val TAG = "LocationService"
        private const val UPDATE_INTERVAL = (4 * 1000 /* 4 secs */).toLong()
        private const val FASTEST_INTERVAL: Long = 2000 /* 2 sec */
    }
}