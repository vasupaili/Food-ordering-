package com.mad.appetit.OrderActivities

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.mad.appetit.OrderActivitiesimport.ListRiderFragment
import com.mad.appetit.OrderActivitiesimport.MapsFragment
import com.mad.appetit.R
import com.mad.mylibrary.OrderItem
import com.mad.mylibrary.OrderRiderItem
import com.mad.mylibrary.Restaurateur
import com.mad.mylibrary.SharedClass.ACCEPTED_ORDER_PATH
import com.mad.mylibrary.SharedClass.CUSTOMER_ID
import com.mad.mylibrary.SharedClass.CUSTOMER_PATH
import com.mad.mylibrary.SharedClass.ORDER_ID
import com.mad.mylibrary.SharedClass.RESERVATION_PATH
import com.mad.mylibrary.SharedClass.RESTAURATEUR_INFO
import com.mad.mylibrary.SharedClass.RIDERS_ORDER
import com.mad.mylibrary.SharedClass.RIDERS_PATH
import com.mad.mylibrary.SharedClass.ROOT_UID
import com.mad.mylibrary.SharedClass.STATUS_DELIVERING
import com.mad.mylibrary.Utilities.updateInfoDish
import java.util.Objects
import java.util.TreeMap


class MapsActivity : AppCompatActivity(), MapsFragment.OnFragmentInteractionListener,
    ListRiderFragment.OnFragmentInteractionListener {
    var ridersMap: HashMap<String?, String?>? = null
        private set
    var distanceMap: TreeMap<Double, String?>? = null
        private set
    private var mLocationPermissionGranted = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        locationPermission
        if (checkMapServices() && mLocationPermissionGranted) {
            supportFragmentManager.beginTransaction().replace(
                R.id.fragment_maps_container,
                MapsFragment()
            ).commit()
            mapsFragVisible = true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_maps, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.near_rider -> {
                chooseNearestRider()
                true
            }

            R.id.list_riders -> {
                if (mapsFragVisible) {
                    mapsFragVisible = false
                    item.setIcon(R.drawable.icon_map)
                    supportFragmentManager.beginTransaction().replace(
                        R.id.fragment_maps_container,
                        ListRiderFragment()
                    ).commit()
                } else {
                    mapsFragVisible = true
                    item.setIcon(R.drawable.showlist_map)
                    supportFragmentManager.beginTransaction().replace(
                        R.id.fragment_maps_container,
                        MapsFragment()
                    ).commit()
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkMapServices(): Boolean {
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
            return false
        }
        return true
    }

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
            .setCancelable(false)
            .setNegativeButton("No") { dialog: DialogInterface?, id: Int -> finish() }
            .setPositiveButton("Yes") { dialog: DialogInterface?, id: Int ->
                val enableGpsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS)
            }
        val alert = builder.create()
        alert.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PERMISSIONS_REQUEST_ENABLE_GPS -> {
                locationPermission
                if (checkMapServices() && mLocationPermissionGranted) {
                    supportFragmentManager.beginTransaction().replace(
                        R.id.fragment_maps_container,
                        MapsFragment()
                    ).commit()
                    mapsFragVisible = true
                } else finish()
            }

            else -> finish()
        }
    }

    private val locationPermission: Unit
        private get() {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                mLocationPermissionGranted = true
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                )
            }
        }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> if (grantResults.size > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                mLocationPermissionGranted = true
                if (checkMapServices()) {
                    supportFragmentManager.beginTransaction().replace(
                        R.id.fragment_maps_container,
                        MapsFragment()
                    ).commit()
                    mapsFragVisible = true
                }
            } else  // Request is cancelled, the result arrays are empty.
                finish()

            else -> finish()
        }
    }

    private fun chooseNearestRider() {
        val choiceDialog = AlertDialog.Builder(this).create()
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.reservation_dialog, null)
        view.findViewById<View>(R.id.button_confirm).setOnClickListener { e: View? ->
            choiceDialog.dismiss()
            selectRider(
                distanceMap!!.firstEntry().value,
                intent.getStringExtra(ORDER_ID),
                intent.getStringExtra(CUSTOMER_ID)
            )
        }
        view.findViewById<View>(R.id.button_cancel)
            .setOnClickListener { e: View? -> choiceDialog.dismiss() }
        choiceDialog.setView(view)
        choiceDialog.setTitle("Do you want to choose automatically the nearest rider?")
        choiceDialog.show()
    }

    fun selectRider(riderId: String?, orderId: String?, customerId: String?) {
        val database = FirebaseDatabase.getInstance()
        val queryDel: Query = database.reference.child(
            RESTAURATEUR_INFO + "/" + ROOT_UID
                    + "/" + RESERVATION_PATH
        ).child(orderId!!)
        queryDel.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val acceptOrder = database.getReference(
                        RESTAURATEUR_INFO + "/" + ROOT_UID
                                + "/" + ACCEPTED_ORDER_PATH
                    )
                    val orderMap: MutableMap<String?, Any?> = HashMap()
                    val orderItem = dataSnapshot.getValue(OrderItem::class.java)
                    updateInfoDish(orderItem!!.dishes)

                    //removing order from RESERVATION_PATH and storing it into ACCEPTED_ORDER_PATH
                    orderMap[Objects.requireNonNull(acceptOrder.push().key)] = orderItem
                    dataSnapshot.ref.removeValue()
                    acceptOrder.updateChildren(orderMap)

                    //choosing the selected rider (riderId)
                    val queryRider: Query = database.getReference(RIDERS_PATH)
                    queryRider.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                var keyRider = ""
                                var name = ""
                                for (d in dataSnapshot.children) {
                                    if (d.key == riderId) {
                                        keyRider = d.key.toString()
                                        name = d.child("rider_info").child("name").getValue(
                                            String::class.java
                                        ).toString()
                                        break
                                    }
                                }

                                //getting address of restaurant to fill OrderRiderItem class
                                val getAddrRestaurant =
                                    database.getReference(RESTAURATEUR_INFO + "/" + ROOT_UID + "/info")
                                val finalKeyRider = keyRider
                                val finalName = name
                                getAddrRestaurant.addListenerForSingleValueEvent(object :
                                    ValueEventListener {
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            val restaurateur = dataSnapshot.getValue(
                                                Restaurateur::class.java
                                            )
                                            orderMap.clear()
                                            orderMap[orderId] = OrderRiderItem(
                                                ROOT_UID,
                                                customerId,
                                                orderItem!!.addrCustomer,
                                                restaurateur!!.addr,
                                                orderItem.time,
                                                orderItem.totPrice
                                            )
                                            val addOrderToRider =
                                                database.getReference(RIDERS_PATH + "/" + finalKeyRider + RIDERS_ORDER)
                                            addOrderToRider.updateChildren(orderMap)

                                            //setting to 'false' the availability of that rider
                                            val setFalse =
                                                database.getReference(RIDERS_PATH + "/" + finalKeyRider + "/available")
                                            setFalse.setValue(false)

                                            //setting STATUS_DELIVERING of the order to customer
                                            val refCustomerOrder = FirebaseDatabase.getInstance()
                                                .reference.child(CUSTOMER_PATH + "/" + customerId)
                                                .child("orders").child(
                                                    orderId
                                                )
                                            val order = HashMap<String, Any>()
                                            order["status"] = STATUS_DELIVERING
                                            refCustomerOrder.updateChildren(order)
                                            Toast.makeText(
                                                applicationContext,
                                                "Order assigned to rider $finalName",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            finish()
                                        }
                                    }

                                    override fun onCancelled(databaseError: DatabaseError) {
                                        Log.w(
                                            "RESERVATION",
                                            "Failed to read value.",
                                            databaseError.toException()
                                        )
                                    }
                                })
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.w(
                                "RESERVATION",
                                "Failed to read value.",
                                databaseError.toException()
                            )
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("RESERVATION", "Failed to read value.", error.toException())
            }
        })
    }

    fun saveRidersList(ridersMap: HashMap<String?, String?>?) {
        this.ridersMap = ridersMap
    }

    fun saveDistanceMap(distanceMap: TreeMap<Double, String?>?) {
        this.distanceMap = distanceMap
    }

    override fun onFragmentInteraction(uri: Uri?) {}

    companion object {
        private var mapsFragVisible = false
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
        const val PERMISSIONS_REQUEST_ENABLE_GPS = 2
    }
}