package com.mad.appetit.OrderActivitiesimport


import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.PlaceDetectionClient
import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.mad.appetit.OrderActivities.MapsActivity
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
import java.text.DecimalFormat
import java.util.Objects
import java.util.TreeMap


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [MapsFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [MapsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
private class Position {
    var latitude: Double? = null
    var longitude: Double? = null

    constructor()
    constructor(latitude: Double?, longitude: Double?) {
        this.latitude = latitude
        this.longitude = longitude
    }
}

private object Haversine {
    private const val EARTH_RADIUS = 6371 // Approx Earth radius in KM
    fun distance(
        startLat: Double, startLong: Double,
        endLat: Double, endLong: Double
    ): Double {
        var startLat = startLat
        var endLat = endLat
        val dLat = Math.toRadians(endLat - startLat)
        val dLong = Math.toRadians(endLong - startLong)
        startLat = Math.toRadians(startLat)
        endLat = Math.toRadians(endLat)
        val a =
            Haversine.haversineFormula(dLat) + Math.cos(startLat) * Math.cos(endLat) * Haversine.haversineFormula(
                dLong
            )
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return Haversine.EARTH_RADIUS * c // <-- d
    }

    fun haversineFormula(`val`: Double): Double {
        return Math.pow(Math.sin(`val` / 2), 2.0)
    }
}

class MapsFragment : Fragment(), OnMapReadyCallback {
    private lateinit var mapView: MapView
    private var mMap: GoogleMap? = null
    private val mLocationPermissionGranted = true
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private var mLastKnownLocation: Location? = null
    private var mPlaceDetectionClient: PlaceDetectionClient? = null
    private val mDefaultLocation = LatLng(-33.8523341, 151.2106085) //default location
    private var longitude = 0.0
    private var latitude = 0.0
    private lateinit var queryRiderPos: Query
    private var riderPosListener: ValueEventListener? = null
    private var restaurantName: String? = null
    private var posMap: HashMap<String?, Position?>? = null
    private var riderName: HashMap<String?, String?>? = null
    private var distanceMap: TreeMap<Double, String?>? = null
    private val markerMap = HashMap<String?, Marker?>()
    private val riderKey = HashSet<String?>()
    private var mListener: MapsFragment.OnFragmentInteractionListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(
            this.requireContext(), null
        )

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this.requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.map)
        mapView.onCreate(savedInstanceState)
        mapView.onResume()
        mapView.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        updateLocationUI()
        try {
            if (mLocationPermissionGranted) {

                val locationResult: Task<*> = mFusedLocationProviderClient!!.lastLocation


                locationResult.addOnCompleteListener { task: Task<*> ->
                        if (task.isSuccessful) {
                            // Set the map's camera position to the current location of the device.
                            val location = task.result as? Location
                            if (location != null) {
                                mLastKnownLocation = location

                                // Rest of your code
                            }
                           // mLastKnownLocation = task.result as? Location
                            val getRestaurantInfo: Query = FirebaseDatabase.getInstance().reference
                                .child(RESTAURATEUR_INFO + "/" + ROOT_UID)
                            getRestaurantInfo.addListenerForSingleValueEvent(object :
                                ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        val position = dataSnapshot.child("info_pos").getValue(
                                            Position::class.java
                                        )
                                        restaurantName =
                                            dataSnapshot.child("info").child("name").getValue(
                                                String::class.java
                                            )
                                        if (position != null) {
                                            latitude = position.latitude!!
                                        }
                                        if (position != null) {
                                            longitude = position.longitude!!
                                        }
                                        mMap!!.moveCamera(
                                            CameraUpdateFactory.newLatLngZoom(
                                                LatLng(latitude, longitude),
                                                MapsFragment.Companion.DEFAULT_ZOOM.toFloat()
                                            )
                                        )
                                        mMap!!.addCircle(
                                            CircleOptions()
                                                .center(LatLng(latitude, longitude))
                                                .radius(10000.0)
                                                .strokeColor(-0x438c9e)
                                                .fillColor(0x32FFC8C8)
                                        )
                                        setRidersOnMaps()
                                    }
                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                    Log.w(
                                        "MAPS FRAGMENT",
                                        "Failed to read value.",
                                        databaseError.toException()
                                    )
                                }
                            })
                        } else {
                            Log.d("TAG", "Current location is null. Using defaults.")
                            Log.e("TAG", "Exception: %s", task.exception)
                            mMap!!.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    mDefaultLocation,
                                    MapsFragment.Companion.DEFAULT_ZOOM.toFloat()
                                )
                            )
                            mMap!!.uiSettings.isMyLocationButtonEnabled = false
                        }
                    }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message!!)
        }
    }

    private fun setRidersOnMaps() {
        queryRiderPos = FirebaseDatabase.getInstance().getReference(RIDERS_PATH)
        queryRiderPos.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    distanceMap = TreeMap()
                    posMap = HashMap()
                    riderName = HashMap()
                    for (d in dataSnapshot.children) {
                        val availableValue = d.child("available").value
                        //if (d.child("available").value as Boolean== true) {
                            if (availableValue is Boolean && availableValue) {
                            riderName!![d.key] = d.child("rider_info").child("name").getValue(
                                String::class.java
                            )
                            val riderPos = d.child("rider_pos").getValue(Position::class.java)

//                            posMap!![d.key] = d.child("rider_pos").getValue(
//                                Position::class.java
//                            )
                            if (riderPos?.latitude != null && riderPos.longitude != null) {
                                posMap!![d.key] = riderPos
                                distanceMap!![posMap!![d.key]!!.latitude?.let {
                                    posMap!![d.key]!!.longitude?.let { it1 ->
                                        Haversine.distance(
                                            latitude,
                                            longitude,
                                            it,
                                            it1
                                        )
                                    }
                                }!!] = d.key}
                            }


                    }
                    if (distanceMap!!.isEmpty()) {
                        val builder = AlertDialog.Builder(
                            context!!
                        )
                        builder.setMessage("No riders available. Retry later!")
                            .setCancelable(false)
                            .setNeutralButton("Ok") { dialog: DialogInterface?, id: Int -> activity!!.finish() }
                        val alert = builder.create()
                        alert.show()
                    } else {
                        var first = true
                        for ((key, value) in distanceMap!!) {
                            if (!riderKey.contains(value)) {
                                riderKey.add(value)
                                if (first) {
                                    first = false
                                    val m = posMap!![value]!!.latitude?.let {
                                        posMap!![value]!!.longitude?.let { it1 ->
                                            LatLng(
                                                it,
                                                it1
                                            )
                                        }
                                    }?.let {
                                        MarkerOptions().position(
                                            it
                                        )
                                            .title(riderName!![value])
                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.nearest_icon))
                                            .snippet(DecimalFormat("#.##").format(key) + " km")
                                    }?.let {
                                        mMap!!.addMarker(
                                            it
                                        )
                                    }
                                    m!!.tag = value
                                    markerMap[value] = m
                                } else {
                                    val m = posMap!![value]!!.latitude?.let {
                                        posMap!![value]!!.longitude?.let { it1 ->
                                            LatLng(
                                                it,
                                                it1
                                            )
                                        }
                                    }?.let {
                                        MarkerOptions().position(
                                            it
                                        )
                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_rider))
                                            .title(riderName!![value])
                                            .snippet(DecimalFormat("#.##").format(key) + " km")
                                    }?.let {
                                        mMap!!.addMarker(
                                            it
                                        )
                                    }
                                    m!!.tag = value
                                    markerMap[value] = m
                                }
                            } else {
                                if (first) {
                                    first = false
                                    markerMap[value]!!.position = posMap!![value]!!.latitude?.let {
                                        posMap!![value]!!.longitude?.let { it1 ->
                                            LatLng(
                                                it, it1
                                            )
                                        }
                                    }!!
                                    markerMap[value]!!.snippet = DecimalFormat("#.##").format(
                                        key
                                    ) + " km"
                                    markerMap[value]!!.setIcon(
                                        BitmapDescriptorFactory.fromResource(
                                            R.drawable.nearest_icon
                                        )
                                    )
                                } else {
                                    markerMap[value]!!.position = posMap!![value]!!.latitude?.let {
                                        posMap!![value]!!.longitude?.let { it1 ->
                                            LatLng(
                                                it, it1
                                            )
                                        }
                                    }!!
                                    markerMap[value]!!.snippet = DecimalFormat("#.##").format(
                                        key
                                    ) + " km"
                                    markerMap[value]!!.setIcon(
                                        BitmapDescriptorFactory.fromResource(
                                            R.drawable.icon_rider
                                        )
                                    )
                                }
                            }
                        }
                        mMap!!.addMarker(
                            MarkerOptions().position(LatLng(latitude, longitude))
                                .title(restaurantName)
                        )
                        mMap!!.setOnInfoWindowClickListener { marker: Marker ->
                            selectRider(
                                marker.tag.toString(),
                                activity!!.intent.getStringExtra(ORDER_ID),
                                activity!!.intent.getStringExtra(CUSTOMER_ID)
                            )
                        }
                        (activity as MapsActivity?)!!.saveDistanceMap(distanceMap)
                        (activity as MapsActivity?)!!.saveRidersList(riderName)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("MAPS FRAGMENT", "Failed to read value.", databaseError.toException())
            }
        }.also { riderPosListener = it })
    }

    private fun selectRider(riderId: String, orderId: String?, customerId: String?) {
        val reservationDialog = AlertDialog.Builder(
            this.requireContext()
        ).create()
        val inflater = LayoutInflater.from(this.context)
        val view = inflater.inflate(R.layout.reservation_dialog, null)
        view.findViewById<View>(R.id.button_confirm).setOnClickListener { e: View? ->
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

                        // choosing the selected rider (riderId)
                        val queryRider: Query = database.getReference(RIDERS_PATH)
                        queryRider.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    var keyRider = ""
                                    var name = ""
                                    for (d in dataSnapshot.children) {
                                        if (d.key == riderId) {
                                            keyRider = d.key!!
                                            name = d.child("rider_info").child("name").getValue(
                                                String::class.java
                                            ).toString()
                                            break
                                        }
                                    }

                                    //getting address of restaurant to fill OrderRiderItem class
                                    val getAddrRestaurant = database.getReference(
                                        RESTAURATEUR_INFO + "/" + ROOT_UID
                                                + "/info"
                                    )
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

                                                //setting to 'false' boolean variable of rider
                                                val setFalse =
                                                    database.getReference(RIDERS_PATH + "/" + finalKeyRider + "/available")
                                                setFalse.setValue(false)

                                                //setting status delivering of the order to customer
                                                val refCustomerOrder =
                                                    FirebaseDatabase.getInstance()
                                                        .reference.child(CUSTOMER_PATH + "/" + customerId)
                                                        .child("orders").child(
                                                            orderId
                                                        )
                                                val order = HashMap<String, Any>()
                                                order["status"] = STATUS_DELIVERING
                                                refCustomerOrder.updateChildren(order)
                                                reservationDialog.dismiss()
                                                Toast.makeText(
                                                    context,
                                                    "Order assigned to rider $finalName",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                activity!!.finish()
                                            }
                                        }

                                        override fun onCancelled(databaseError: DatabaseError) {
                                            Log.w(
                                                "MAPS FRAGMENT",
                                                "Failed to read value.",
                                                databaseError.toException()
                                            )
                                        }
                                    })
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                Log.w(
                                    "MAPS FRAGMENT",
                                    "Failed to read value.",
                                    databaseError.toException()
                                )
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("MAPS FRAGMENT", "Failed to read value.", error.toException())
                }
            })
        }
        view.findViewById<View>(R.id.button_cancel)
            .setOnClickListener { e: View? -> reservationDialog.dismiss() }
        reservationDialog.setView(view)
        reservationDialog.setTitle("Are you sure to select this rider?\n")
        reservationDialog.show()
    }

    private fun updateLocationUI() {
        if (mMap == null) {
            return
        }
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        mMap!!.isMyLocationEnabled = true
        mMap!!.uiSettings.isMyLocationButtonEnabled = true
    }

    override fun onPause() {
        queryRiderPos!!.removeEventListener(riderPosListener!!)
        super.onPause()
    }

    override fun onStop() {
        queryRiderPos!!.removeEventListener(riderPosListener!!)
        super.onStop()
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri?) {
        if (mListener != null) {
            mListener!!.onFragmentInteraction(uri)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is MapsFragment.OnFragmentInteractionListener) {
            context
        } else {
            throw RuntimeException(
                context.toString()
                        + " must implement OnFragmentInteractionListener"
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri?)
    }

    companion object {
        private const val DEFAULT_ZOOM = 15
        fun newInstance(): MapsFragment {
            val fragment = MapsFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}