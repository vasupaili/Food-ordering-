package com.mad.appetit.OrderActivitiesimport

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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



private class RiderInfo(val name: String?, val key: String?, val dist: Double?)
private class ListRiderAdapter(
    context: Context?,
    private val mDataset: ArrayList<RiderInfo>,
    private val listRiderFragment: ListRiderFragment
) : RecyclerView.Adapter<ListRiderAdapter.MyViewHolder?>() {
    private val mInflater: LayoutInflater

    init {
        mInflater = LayoutInflater.from(context)
    }

    internal inner class MyViewHolder(var view_item: View) : RecyclerView.ViewHolder(
        view_item
    ) {
        var nameRider: TextView
        var distanceValue: TextView

        init {
            nameRider = itemView.findViewById(R.id.name_rider)
            distanceValue = itemView.findViewById(R.id.distance)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ListRiderAdapter.MyViewHolder {
        val view = mInflater.inflate(R.layout.list_rider_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListRiderAdapter.MyViewHolder, position: Int) {
        val mCurrent = mDataset[position]
        holder.nameRider.text = mCurrent.name
        val df = DecimalFormat("#.##")
        holder.distanceValue.text = df.format(mCurrent.dist) + " km"
        holder.itemView.findViewById<View>(R.id.confirm_rider)
            .setOnClickListener { e: View? -> mCurrent.key?.let { listRiderFragment.selectRider(it) } }
    }

    override fun getItemCount(): Int {
        return mDataset.size
    }
}

class ListRiderFragment : Fragment() {
    private var mListener: ListRiderFragment.OnFragmentInteractionListener? = null
    private var distanceMap: TreeMap<Double?, String?>? = null
    private var ridersMap: HashMap<String?, String?>? = null
    private var ridersList: ArrayList<RiderInfo>? = null
    private lateinit var recyclerView: RecyclerView
    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_list_rider, container, false)
        ridersMap = (activity as MapsActivity?)?.ridersMap
      //  distanceMap = (activity as MapsActivity?)?.distanceMap
        distanceMap = (activity as MapsActivity?)?.distanceMap as TreeMap<Double?, String?>?

        treeMapToList(ridersMap, distanceMap)
        recyclerView = view.findViewById(R.id.list_rider_recyclerview)
        recyclerView.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(this.context)
        recyclerView.setLayoutManager(layoutManager)
        mAdapter = ridersList?.let { ListRiderAdapter(context, it, this) }
        recyclerView.setAdapter(mAdapter)
        return view
    }

    fun treeMapToList(
        ridersMap: HashMap<String?, String?>?,
        distanceMap: TreeMap<Double?, String?>?
    ) {
        ridersList = ArrayList()
        for ((key, value) in distanceMap!!) {
            ridersList!!.add(RiderInfo(ridersMap!![value], value, key))
        }
    }

    fun selectRider(riderId: String) {
        val reservationDialog = AlertDialog.Builder(
            this.requireContext()
        ).create()
        val inflater = LayoutInflater.from(this.context)
        val view = inflater.inflate(R.layout.reservation_dialog, null)
        val orderId = requireActivity().intent.getStringExtra(ORDER_ID)
        val customerId = requireActivity().intent.getStringExtra(CUSTOMER_ID)
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

                        // choosing the first available rider which assign the order
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

                                                //setting to 'false' boolean variable of rider
                                                val setFalse =
                                                    database.getReference(RIDERS_PATH + "/" + finalKeyRider + "/available")
                                                setFalse.setValue(false)

                                                //setting STATUS_DELIVERING of the order to customer
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
                                                "RESERVATION",
                                                "Failed to read value.",
                                                databaseError.toException()
                                            )
                                        }
                                    })
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {}
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("RESERVATION", "Failed to read value.", error.toException())
                }
            })
        }
        view.findViewById<View>(R.id.button_cancel)
            .setOnClickListener { e: View? -> reservationDialog.dismiss() }
        reservationDialog.setView(view)
        reservationDialog.setTitle("Are you sure to select this rider?\n")
        reservationDialog.show()
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri?) {
        if (mListener != null) {
            mListener!!.onFragmentInteraction(uri)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is ListRiderFragment.OnFragmentInteractionListener) {
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
}