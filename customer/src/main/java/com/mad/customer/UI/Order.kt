package com.mad.customer.UI

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.FirebaseDatabase
import com.mad.customer.Itemsimport.OrderCustomerItem
import com.mad.customer.R
import com.mad.customer.UIimport.OrderDetails
import com.mad.customer.ViewHoldersimport.OrderViewHolder
import com.mad.mylibrary.SharedClass


class Order : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private var mAdapter: FirebaseRecyclerAdapter<OrderCustomerItem, OrderViewHolder>? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    private val recyclerView_ordered: RecyclerView? = null
    private var mListener: OnFragmentInteractionListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_order, container, false)
        recyclerView = view.findViewById(R.id.ordered_list)
        mAdapter = object : FirebaseRecyclerAdapter<OrderCustomerItem, OrderViewHolder>(options) {
            override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): OrderViewHolder {
                val view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.order_item, viewGroup, false)
                return OrderViewHolder(view)
            }

            override fun onBindViewHolder(
                holder: OrderViewHolder,
                position: Int,
                model: OrderCustomerItem
            ) {
                val orderkey = getRef(position).key
                if (orderkey != null) {
                    holder.setData(model, position, orderkey)
                }
                holder.view.findViewById<View>(R.id.order_details_button)
                    .setOnClickListener { a: View? ->
                        val intent = Intent(context, OrderDetails::class.java)
                        intent.putExtra("order_item", model)
                        startActivity(intent)
                    }
            }
        }
        layoutManager = LinearLayoutManager(context)
        recyclerView.setAdapter(mAdapter)
        recyclerView.setLayoutManager(layoutManager)
        return view
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

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri?)
    }

    companion object {
        private val options = FirebaseRecyclerOptions.Builder<OrderCustomerItem>()
            .setQuery(
                FirebaseDatabase.getInstance().getReference(SharedClass.CUSTOMER_PATH)
                    .child(SharedClass.ROOT_UID).child("orders").orderByChild("sort"),
                OrderCustomerItem::class.java
            ).build()
    }
}