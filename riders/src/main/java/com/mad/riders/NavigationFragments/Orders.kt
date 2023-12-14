package com.mad.riders.NavigationFragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.maps.DirectionsApiRequest
import com.google.maps.GeoApiContext
import com.google.maps.PendingResult
import com.google.maps.internal.PolylineEncoding
import com.google.maps.model.DirectionsResult
import com.google.maps.model.DirectionsRoute
import com.mad.mylibrary.OrderRiderItem
import com.mad.mylibrary.Restaurateur
import com.mad.mylibrary.SharedClass.CUSTOMER_PATH
import com.mad.mylibrary.SharedClass.RIDERS_PATH
import com.mad.mylibrary.SharedClass.ROOT_UID
import com.mad.mylibrary.SharedClass.STATUS_DELIVERED
import com.mad.mylibrary.Utilities.getDateFromTimestamp
import com.mad.riders.R
import java.io.IOException
import java.util.Calendar
import java.util.Date
import java.util.UUID

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [Orders.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [Orders.newInstance] factory method to
 * create an instance of this fragment.
 */
//TODO: Fix MapView
//TODO: Add dynamic button
class Orders constructor() : Fragment(), OnMapReadyCallback {
    private var available: Boolean = false
    private var restaurantReached: Boolean = false
    private var order: OrderRiderItem? = null
    private var query1: DatabaseReference? = null
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mGeoApiContext: GeoApiContext? = null
    private var mMap: GoogleMap? = null
    var col: Int = 0
    private val mScrollView: ScrollView? = null
    private var query: DatabaseReference? = null
    lateinit var listenerQuery: ValueEventListener
    private var database: FirebaseDatabase? = null
    private val latLng_restaurant: LatLng? = null
    private val restaurateur: Restaurateur? = null
    private var pos_restaurant: LatLng? = null
    private var distance: Long? = null
    private var orderKey: String? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        distance = 0L
    }

    public override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_orders, container, false)
        val mapView: MapView = view.findViewById(R.id.map)
        mapView.onCreate(savedInstanceState)
        mapView.onResume()
        mapView.getMapAsync(this)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient((getContext())!!)
        if (mGeoApiContext == null) {
            mGeoApiContext = GeoApiContext.Builder().apiKey(getString(R.string.google_maps_key))
                .build()
        }
        restaurantReached = false
        query1 = FirebaseDatabase.getInstance().getReference(RIDERS_PATH + "/" + ROOT_UID)
            .child("available")
        query1!!.addValueEventListener(object : ValueEventListener {
            public override fun onDataChange(dataSnapshot: DataSnapshot) {
                //TODO: STATUS
                if (dataSnapshot.exists()) {
                    available = dataSnapshot.getValue() as Boolean
                    if (!available) {
                        val btn: Button = view.findViewById(R.id.accept_button)
                        btn.setText("Restaurant Reached")
                        val text: TextView = view.findViewById(R.id.status)
                        text.setText("Delivering..")
                    } else {
                        val btn: Button = view.findViewById(R.id.accept_button)
                        btn.setText("No pending order")
                        val text: TextView = view.findViewById(R.id.status)
                        text.setText("Available")
                        cancelOrderView(view)
                    }
                }
            }

            public override fun onCancelled(databaseError: DatabaseError) {}
        })

        // BUTTON ACCEPTED ORDER
        val b: Button = view.findViewById(R.id.accept_button)
        b.setOnClickListener(View.OnClickListener({ e: View? ->
            if (available) acceptOrder() else {
                if (!restaurantReached) {
                    restaurantReachedByRider(b)
                } else {
                    deliveredOrder()
                }
            }
        }))
        database = FirebaseDatabase.getInstance()
        query = database!!
            .getReference(RIDERS_PATH + "/" + ROOT_UID + "/pending/")
        listenerQuery = object : ValueEventListener {
            public override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (d: DataSnapshot in dataSnapshot.getChildren()) {
                    orderKey = d.getKey()
                    order = d.getValue(OrderRiderItem::class.java)
                    setOrderView(view, order)
                    val restaurantAddress: String = order!!.addrRestaurant + ",Torino"
                    val customerAddress: String? = order!!.addrCustomer
                    Log.d("QUERY", (customerAddress)!!)
                    pos_restaurant = getLocationFromAddress(restaurantAddress)
                    getLastKnownLocation(pos_restaurant)
                    b.setEnabled(true)
                }
            }

            public override fun onCancelled(databaseError: DatabaseError) {
                cancelOrderView(view)
                b.setText("No order pending")
                b.setEnabled(false)
            }
        }
        query!!.addListenerForSingleValueEvent(listenerQuery)
        return view
    }

    public override fun onPause() {
        super.onPause()
        query1!!.removeEventListener((listenerQuery)!!)
    }

    private fun restaurantReachedByRider(b: Button) {
        val reservationDialog: AlertDialog = AlertDialog.Builder(
            (getContext())!!
        ).create()
        val inflater: LayoutInflater = LayoutInflater.from(getContext())
        val view: View = inflater.inflate(R.layout.reservation_dialog, null)
        view.findViewById<View>(R.id.button_confirm)
            .setOnClickListener(View.OnClickListener { e: View? ->
                restaurantReached = true
                b.setText("Order delivered")
                val customerAddr: String? = order!!.addrCustomer
                mMap!!.clear()
                getLastKnownLocation(getLocationFromAddress(customerAddr))
                reservationDialog.dismiss()
            })
        view.findViewById<View>(R.id.button_cancel)
            .setOnClickListener(View.OnClickListener({ e: View? -> reservationDialog.dismiss() }))
        reservationDialog.setView(view)
        reservationDialog.setTitle("Restaurant Reached?")
        reservationDialog.show()
    }

    private fun setOrderView(view: View, order: OrderRiderItem?) {
        val r_addr: TextView = view.findViewById(R.id.restaurant_text)
        val c_addr: TextView = view.findViewById(R.id.customer_text)
        val time_text: TextView = view.findViewById(R.id.time_text)
        val cash_text: TextView = view.findViewById(R.id.cash_text)
        r_addr.setText(order!!.addrRestaurant)
        c_addr.setText(order.addrCustomer)
        time_text.setText(getDateFromTimestamp(order.time))
        cash_text.setText(order.totPrice + " €")
    }

    private fun cancelOrderView(view: View) {
        val r_addr: TextView = view.findViewById(R.id.restaurant_text)
        val c_addr: TextView = view.findViewById(R.id.customer_text)
        val time_text: TextView = view.findViewById(R.id.time_text)
        val cash_text: TextView = view.findViewById(R.id.cash_text)
        r_addr.setText("")
        c_addr.setText("")
        time_text.setText("")
        cash_text.setText("")
    }

    fun acceptOrder() {
        val reservationDialog: AlertDialog = AlertDialog.Builder(
            (getContext())!!
        ).create()
        val inflater: LayoutInflater = LayoutInflater.from(getContext())
        val view: View = inflater.inflate(R.layout.reservation_dialog, null)
        view.findViewById<View>(R.id.button_confirm)
            .setOnClickListener(View.OnClickListener { e: View? ->
                if (!available) {
                    Toast.makeText(
                        getContext(),
                        "You have alredy accepted this order!", Toast.LENGTH_LONG
                    ).show()
                } else {
                    val query: DatabaseReference = FirebaseDatabase.getInstance()
                        .getReference(RIDERS_PATH + "/" + ROOT_UID)
                    val status: MutableMap<String, Any> = HashMap()
                    status.put("available", false)
                    query.updateChildren(status)
                }
                reservationDialog.dismiss()
            })
        view.findViewById<View>(R.id.button_cancel)
            .setOnClickListener(View.OnClickListener { e: View? ->
                val query: DatabaseReference = FirebaseDatabase
                    .getInstance().getReference(RIDERS_PATH + "/" + ROOT_UID + "/pending/")
                query.removeValue()
                reservationDialog.dismiss()
            })
        reservationDialog.setView(view)
        reservationDialog.setTitle("Confirm Orders?")
        reservationDialog.show()
    }

    fun deliveredOrder() {
        val reservationDialog: AlertDialog = AlertDialog.Builder(
            (getContext())!!
        ).create()
        val inflater: LayoutInflater = LayoutInflater.from(getContext())
        val view: View = inflater.inflate(R.layout.reservation_dialog, null)
        view.findViewById<View>(R.id.button_confirm)
            .setOnClickListener(View.OnClickListener { e: View? ->
                val query: DatabaseReference = FirebaseDatabase.getInstance()
                    .getReference(RIDERS_PATH + "/" + ROOT_UID + "/pending/")
                query.removeValue()
                val query2: DatabaseReference = FirebaseDatabase.getInstance()
                    .getReference(RIDERS_PATH + "/" + ROOT_UID)
                val status: MutableMap<String, Any> = HashMap()
                status.put("available", true)
                query2.updateChildren(status)

                //SET STATUS TO CUSTOMER ORDER
                val refCustomerOrder: DatabaseReference = FirebaseDatabase.getInstance()
                    .getReference().child(CUSTOMER_PATH).child((order!!.keyCustomer)!!)
                    .child("orders").child((orderKey)!!)
                val order_status: HashMap<String, Any> = HashMap()
                order_status.put("status", STATUS_DELIVERED)
                refCustomerOrder.updateChildren(order_status)
                mMap!!.clear()
                val query3: DatabaseReference = FirebaseDatabase.getInstance()
                    .getReference(RIDERS_PATH + "/" + ROOT_UID).child("delivered")
                val delivered: MutableMap<String, Any?> = HashMap()
                delivered.put(UUID.randomUUID().toString(), distance)
                query3.updateChildren(delivered)
                distance = 0L
                reservationDialog.dismiss()
            })
        view.findViewById<View>(R.id.button_cancel)
            .setOnClickListener(View.OnClickListener({ e: View? -> reservationDialog.dismiss() }))
        reservationDialog.setView(view)
        reservationDialog.setTitle("Order restaurantReached?")
        reservationDialog.show()
    }

    public override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    public override fun onDetach() {
        super.onDetach()
    }

    public override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if ((ActivityCompat.checkSelfPermission(
                (getContext())!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(
                (getContext())!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            return
        }
        Log.d("MAP_DEBUG", "Sto visualizzando la posizione")
        mMap!!.setMyLocationEnabled(true)
    }

    public interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri?)
    }

    private fun getLastKnownLocation(restaurantPos: LatLng?) {
        Log.d("DEBUG MAP", "getLastKnownLocation: called.")
        if (ActivityCompat.checkSelfPermission(
                (getContext())!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        mFusedLocationClient!!.getLastLocation()
            .addOnCompleteListener(object : OnCompleteListener<Location> {
                public override fun onComplete(task: Task<Location>) {
                    if (task.isSuccessful()) {
                        val mUserLocation: Location = task.getResult()
                        val mUserPosition: LatLng =
                            LatLng(mUserLocation.getLatitude(), mUserLocation.getLongitude())
                        mMap!!.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                mUserPosition,
                                16.0f
                            )
                        )
                        calculateDirections(mUserPosition, restaurantPos)
                    }
                }
            })
    }

    private fun calculateDirections(start: LatLng, end: LatLng?) {
        Log.d("MAP DEBUG", "calculateDirections: calculating directions.")

        if (end == null) {
            Log.e("MAP DEBUG", "calculateDirections: destination is null.")
            return
        }
        val destination: com.google.maps.model.LatLng = com.google.maps.model.LatLng(
            end.latitude,
            end.longitude
        )
        val directions: DirectionsApiRequest = DirectionsApiRequest(mGeoApiContext)
        directions.alternatives(true)
        directions.origin(
            com.google.maps.model.LatLng(
                start.latitude,
                start.longitude
            )
        )
        Log.d("DEBUG", "calculateDirections: destination: " + destination.toString())
        directions.destination(destination)
            .setCallback(object : PendingResult.Callback<DirectionsResult> {
                public override fun onResult(result: DirectionsResult) {
                    Log.d(
                        "DEBUG",
                        "calculateDirections: routes: " + result.routes.get(0).toString()
                    )
                    Log.d(
                        "DEBUG",
                        "calculateDirections: duration: " + result.routes.get(0).legs.get(0).duration
                    )
                    Log.d(
                        "DEBUG",
                        "calculateDirections: distance: " + result.routes.get(0).legs.get(0).distance
                    )
                    Log.d(
                        "DEBUG",
                        "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints.get(0)
                            .toString()
                    )
                    addPolylinesToMap(result, end)
                }

                public override fun onFailure(e: Throwable) {
                    Log.e("DEBuG", "calculateDirections: Failed to get directions: " + e.message)
                }
            })
    }

    private fun addPolylinesToMap(result: DirectionsResult, finalPos: LatLng?) {
        Handler(Looper.getMainLooper()).post(object : Runnable {
            public override fun run() {
                Log.d("DEBUG", "run: result routes: " + result.routes.size)
                val route: DirectionsRoute = result.routes.get(0)
                Log.d("DEBUG", "run: leg: " + route.legs.get(0).toString())
                val decodedPath: List<com.google.maps.model.LatLng> =
                    PolylineEncoding.decode(route.overviewPolyline.getEncodedPath())
                val newDecodedPath: MutableList<LatLng> = ArrayList()

                // This loops through all the LatLng coordinates of ONE polyline.
                for (latLng: com.google.maps.model.LatLng in decodedPath) {

//                        Log.d(TAG, "run: latlng: " + latLng.toString());
                    newDecodedPath.add(
                        LatLng(
                            latLng.lat,
                            latLng.lng
                        )
                    )
                }
                val polyline: Polyline =
                    mMap!!.addPolyline(PolylineOptions().addAll(newDecodedPath))
                if (col == 0) {
                    polyline.setColor(
                        ContextCompat.getColor(
                            (getContext())!!,
                            R.color.colorPrimary
                        )
                    )
                    col++
                } else {
                    polyline.setColor(
                        ContextCompat.getColor(
                            (getContext())!!,
                            R.color.colorPrimary
                        )
                    )
                    col--
                }
                polyline.setClickable(true)
                val finalMarker: Marker? = mMap!!.addMarker(
                    MarkerOptions()
                        .position((finalPos)!!) //TODO: FIX NAME
                        .title("NAME")
                        .snippet(
                            "Duration: " + route.legs.get(0).duration
                        )
                )
                distance = distance!! + route.legs.get(0).distance.inMeters
            }
        })
    }

    private fun getDateFromTimestamp(timestamp: Long): String {
        val d: Date = Date(timestamp)
        val c: Calendar = Calendar.getInstance()
        c.setTime(d)
        val hourValue: Int = c.get(Calendar.HOUR)
        val minValue: Int = c.get(Calendar.MINUTE)
        var hourString: String = Integer.toString(hourValue)
        var minString: String = Integer.toString(minValue)
        if (hourValue < 10) hourString = "0" + hourValue
        if (minValue < 10) minString = "0" + minValue
        return hourString + ":" + minString
    }

    //Other functions
    fun getLocationFromAddress(strAddress: String?): LatLng? {
        val coder: Geocoder = Geocoder((getContext())!!)
        val address: List<Address>?
        var p1: LatLng? = null
        try {
            address = coder.getFromLocationName((strAddress)!!, 5)
            if (address == null) {
                return null
            }
            val location: Address = address.get(0)
            location.getLatitude()
            location.getLongitude()
            p1 = LatLng(
                (location.getLatitude()),
                (location.getLongitude())
            )
            return p1
        } catch (ex: IOException) {
            ex.printStackTrace()
            Toast.makeText(getContext(), ex.message, Toast.LENGTH_LONG).show()
        }
        return null
    }

    companion object {
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private val ARG_PARAM1: String = "param1"
        private val ARG_PARAM2: String = "param2"

        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String?, param2: String?): Orders {
            val fragment: Orders = Orders()
            val args: Bundle = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.setArguments(args)
            return fragment
        }
    }
}