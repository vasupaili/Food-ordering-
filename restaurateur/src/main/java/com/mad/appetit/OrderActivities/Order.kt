package com.mad.appetit.OrderActivities

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.mad.mylibrary.SharedClass.ACCEPTED_ORDER_PATH
import com.mad.mylibrary.SharedClass.RESTAURATEUR_INFO
import com.mad.mylibrary.SharedClass.ROOT_UID


class Order : Fragment() {
    private var mListener: OnFragmentInteractionListener? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var mAdapter_ordered: RecyclerAdapterOrdered? = null
    private lateinit var recyclerView_accepted: RecyclerView
    private lateinit var recyclerView_ordered: RecyclerView
    private lateinit var mAdapter_accepted: FirebaseRecyclerAdapter<OrderItem, ViewHolderReservation>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_order, container, false)
        recyclerView_accepted = view.findViewById(R.id.reservation_list_accepted)
        mAdapter_accepted = object : FirebaseRecyclerAdapter<OrderItem, ViewHolderReservation>(
            options
        ) {
            override fun onBindViewHolder(
                holder: ViewHolderReservation,
                position: Int,
                model: OrderItem
            ) {
                holder.setData(model, position)
                holder.view.findViewById<View>(R.id.confirm_reservation).visibility = View.INVISIBLE
                holder.view.findViewById<View>(R.id.delete_reservation).visibility = View.INVISIBLE
                holder.view.findViewById<View>(R.id.open_reservation)
                    .setOnClickListener { k: View? -> viewOrder(getRef(position).key) }
            }

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): ViewHolderReservation {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.reservation_listview, parent, false)
                return ViewHolderReservation(view)
            }
        }
        layoutManager = LinearLayoutManager(context)
        recyclerView_accepted.setAdapter(mAdapter_accepted)
        recyclerView_accepted.setLayoutManager(layoutManager)
        return view
    }

    fun viewOrder(id: String?) {
        val reservationDialog = AlertDialog.Builder(
            this.requireContext()
        ).create()
        val inflater = LayoutInflater.from(this.context)
        val view = inflater.inflate(R.layout.dishes_list_dialog, null)
        val database = FirebaseDatabase.getInstance()
        val query: Query = database.reference.child(
            RESTAURATEUR_INFO + "/" + ROOT_UID
                    + "/" + ACCEPTED_ORDER_PATH
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

    override fun onStart() {
        super.onStart()
        mAdapter_accepted!!.startListening()
    }

    override fun onStop() {
        super.onStop()
        mAdapter_accepted!!.stopListening()
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri?) {
        if (mListener != null) {
            mListener!!.onFragmentInteraction(uri)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is OnFragmentInteractionListener) {
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
        private val options = FirebaseRecyclerOptions.Builder<OrderItem>()
            .setQuery(
                FirebaseDatabase.getInstance().getReference(
                    RESTAURATEUR_INFO + "/" + ROOT_UID
                            + "/" + ACCEPTED_ORDER_PATH
                ),
                OrderItem::class.java
            ).build()
    }
}