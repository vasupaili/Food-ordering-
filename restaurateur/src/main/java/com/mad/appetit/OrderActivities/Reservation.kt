package com.mad.appetit.OrderActivities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.mad.appetit.OrderActivitiesimport.RecyclerAdapterOrdered
import com.mad.appetit.R
import com.mad.mylibrary.OrderItem
import com.mad.mylibrary.SharedClass.CUSTOMER_ID
import com.mad.mylibrary.SharedClass.CUSTOMER_PATH
import com.mad.mylibrary.SharedClass.ORDER_ID
import com.mad.mylibrary.SharedClass.RESERVATION_PATH
import com.mad.mylibrary.SharedClass.RESTAURATEUR_INFO
import com.mad.mylibrary.SharedClass.ROOT_UID
import com.mad.mylibrary.SharedClass.STATUS_DISCARDED
import com.mad.mylibrary.Utilities.getDateFromTimestamp


class ViewHolderReservation(val view: View) : RecyclerView.ViewHolder(
    view
) {
    private val name: TextView
    private val addr: TextView
    private val cell: TextView
    private val time: TextView
    private val price: TextView
    private var position = 0

    init {
        name = itemView.findViewById(R.id.listview_name)
        addr = itemView.findViewById(R.id.listview_address)
        cell = itemView.findViewById(R.id.listview_cellphone)
        time = itemView.findViewById(R.id.textView_time)
        price = itemView.findViewById(R.id.listview_price)
    }

    fun setData(current: OrderItem, pos: Int) {
        val query: Query = FirebaseDatabase.getInstance().getReference(CUSTOMER_PATH).child(
            current.key!!
        ).child("customer_info")
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    name.text = dataSnapshot.child("name").getValue(String::class.java)
                    addr.text = current.addrCustomer
                    cell.text = dataSnapshot.child("phone").getValue(String::class.java)
                    time.text = getDateFromTimestamp(current.time)
                    price.text = current.totPrice
                    position = pos
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }
}

class Reservation : Fragment() {
    private lateinit var mAdapter: FirebaseRecyclerAdapter<OrderItem, ViewHolderReservation>
    private var mAdapter_ordered: RecyclerAdapterOrdered? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var mListener: OnFragmentInteractionListener? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerView_ordered: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reservation, container, false)
        recyclerView = view.findViewById(R.id.ordered_list)
        mAdapter = object : FirebaseRecyclerAdapter<OrderItem, ViewHolderReservation>(options) {
            override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolderReservation {
                val view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.reservation_listview, viewGroup, false)
                return ViewHolderReservation(view)
            }

            override fun onBindViewHolder(
                holder: ViewHolderReservation,
                position: Int,
                model: OrderItem
            ) {
                holder.setData(model, position)
                val keyOrder = getRef(position).key
                holder.view.findViewById<View>(R.id.confirm_reservation)
                    .setOnClickListener { e: View? ->
                        val mapsIntent = Intent(context, MapsActivity::class.java)
                        mapsIntent.putExtra(ORDER_ID, keyOrder)
                        mapsIntent.putExtra(CUSTOMER_ID, model.key)
                        startActivity(mapsIntent)
                    }
                holder.view.findViewById<View>(R.id.delete_reservation)
                    .setOnClickListener { h: View? -> removeOrder(keyOrder, model.key) }
                holder.view.findViewById<View>(R.id.open_reservation)
                    .setOnClickListener { k: View? -> viewOrder(keyOrder) }
            }
        }
        layoutManager = LinearLayoutManager(context)
        recyclerView.setAdapter(mAdapter)
        recyclerView.setLayoutManager(layoutManager)
        return view
    }

    fun removeOrder(keyOrder: String?, keyCustomer: String?) {
        val reservationDialog = AlertDialog.Builder(this.context).create()
        val inflater = LayoutInflater.from(this.context)
        val view = inflater.inflate(R.layout.reservation_dialog, null)
        view.findViewById<View>(R.id.button_confirm).setOnClickListener { e: View? ->
            val database = FirebaseDatabase.getInstance()
            val queryDel: Query = database.reference.child(
                RESTAURATEUR_INFO + "/" + ROOT_UID
                        + "/" + RESERVATION_PATH
            ).child(keyOrder!!)
            queryDel.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (d in dataSnapshot.children) d.ref.removeValue()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("RESERVATION", "Failed to read value.", error.toException())
                }
            })
            mAdapter!!.notifyDataSetChanged()

            //setting status canceled of the order to customer
            val refCustomerOrder = FirebaseDatabase.getInstance()
                .reference.child(CUSTOMER_PATH + "/" + keyCustomer).child("orders").child(keyOrder)
            val order = HashMap<String, Any>()
            order["status"] = STATUS_DISCARDED
            refCustomerOrder.updateChildren(order)
            reservationDialog.dismiss()
        }
        view.findViewById<View>(R.id.button_cancel)
            .setOnClickListener { e: View? -> reservationDialog.dismiss() }
        reservationDialog.setView(view)
        reservationDialog.setTitle("Delete Reservation?")
        reservationDialog.show()
    }

    fun viewOrder(id: String?) {
        val reservationDialog = AlertDialog.Builder(this.context).create()
        val inflater = LayoutInflater.from(this.context)
        val view = inflater.inflate(R.layout.dishes_list_dialog, null)
        val database = FirebaseDatabase.getInstance()
        val query: Query = database.reference.child(
            RESTAURATEUR_INFO + "/" + ROOT_UID
                    + "/" + RESERVATION_PATH
        ).child(id!!).child("dishes")
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val dishes = ArrayList<String>()
                    for (d in dataSnapshot.children) {
                        dishes.add(d.key + " - Quantity: " + d.getValue(Int::class.java))
                    }
                    recyclerView_ordered = view.findViewById(R.id.ordered_list)
                    mAdapter_ordered = RecyclerAdapterOrdered(reservationDialog.context, dishes)
                    layoutManager = LinearLayoutManager(reservationDialog.context)
                    recyclerView_ordered.setAdapter(mAdapter_ordered)
                    recyclerView_ordered.setLayoutManager(layoutManager)
                    view.findViewById<View>(R.id.back)
                        .setOnClickListener { e: View? -> reservationDialog.dismiss() }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("RESERVATION", "Failed to read value.", error.toException())
            }
        })
        reservationDialog.setView(view)
        reservationDialog.setTitle("Order")
        reservationDialog.show()
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri?) {
        if (mListener != null) {
            mListener!!.onFragmentInteraction(uri)
        }
    }

    override fun onStart() {
        super.onStart()
        mAdapter!!.startListening()
    }

    override fun onStop() {
        super.onStop()
        mAdapter!!.stopListening()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener =
            if (context is OnFragmentInteractionListener) {
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
        fun onFragmentInteraction(uri: Uri?)
    }

    companion object {
        private val options = FirebaseRecyclerOptions.Builder<OrderItem>()
            .setQuery(
                FirebaseDatabase.getInstance().getReference(
                    RESTAURATEUR_INFO + "/" + ROOT_UID
                            + "/" + RESERVATION_PATH
                ),
                OrderItem::class.java
            ).build()
    }
}