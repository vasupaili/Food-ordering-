package com.mad.appetit.DishesActivitiesimport

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mad.appetit.R

import com.mad.mylibrary.DishItem
import com.mad.mylibrary.SharedClass.DISHES_PATH
import com.mad.mylibrary.SharedClass.EDIT_EXISTING_DISH
import com.mad.mylibrary.SharedClass.RESTAURATEUR_INFO
import com.mad.mylibrary.SharedClass.ROOT_UID

 private class ViewHolderDailyOffer(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val dishPhoto: ImageView
    private val dishName: TextView
    private val dishDesc: TextView
    private val dishPrice: TextView
    private val dishQuantity: TextView
    private var current: DishItem? = null
    private var position = 0

    init {
        dishName = itemView.findViewById(R.id.dish_name)
        dishDesc = itemView.findViewById(R.id.dish_desc)
        dishPrice = itemView.findViewById(R.id.dish_price)
        dishQuantity = itemView.findViewById(R.id.dish_quant)
        dishPhoto = itemView.findViewById(R.id.dish_image)
    }

    fun setData(current: DishItem, position: Int) {
        dishName.text = current.name
        dishDesc.text = current.desc
        dishPrice.text = current.price.toString() + " €"
        dishQuantity.text = current.quantity.toString()
        if (current.photo != null) Glide.with(itemView.context).load(current.photo)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(dishPhoto) else Glide.with(itemView.context).load(R.drawable.hamburger)
            .into(dishPhoto)
        this.position = position
        this.current = current
    }
}

class DailyOffer : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private var layoutManager: RecyclerView.LayoutManager? = null
    private lateinit var mAdapter: FirebaseRecyclerAdapter<DishItem, ViewHolderDailyOffer>
    private var mListener: DailyOffer.OnFragmentInteractionListener? = null
    private val options = FirebaseRecyclerOptions.Builder<DishItem>()
        .setQuery(
            FirebaseDatabase.getInstance().getReference(
                RESTAURATEUR_INFO + "/" +
                        ROOT_UID + "/" + DISHES_PATH
            ),
            DishItem::class.java
        ).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dailyoffer, container, false)
        recyclerView = view.findViewById(R.id.dish_list)
        layoutManager = LinearLayoutManager(context)
        recyclerView.setLayoutManager(layoutManager)
        mAdapter = object : FirebaseRecyclerAdapter<DishItem, ViewHolderDailyOffer>(options) {
            override fun onBindViewHolder(
                holder: ViewHolderDailyOffer,
                position: Int,
                model: DishItem
            ) {
                holder.setData(model, position)
            }

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): ViewHolderDailyOffer {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.dailyoffer_listview, parent, false)
                view.findViewById<View>(R.id.delete_offer)
                    .setOnClickListener { e: View? -> deleteDish((view.findViewById<View>(R.id.dish_name) as TextView).text.toString()) }
                view.findViewById<View>(R.id.edit_offer)
                    .setOnClickListener { h: View? -> editDailyOffer((view.findViewById<View>(R.id.dish_name) as TextView).text.toString()) }
                return ViewHolderDailyOffer(view)
            }
        }
        recyclerView.setAdapter(mAdapter)
        return view
    }

    fun editDailyOffer(dishName: String?) {
        val editOffer = Intent(context, EditOffer::class.java)
        editOffer.putExtra(EDIT_EXISTING_DISH, dishName)
        startActivity(editOffer)
    }

    fun deleteDish(dishName: String?) {
        val reservationDialog = AlertDialog.Builder(
            this.requireContext()
        ).create()
        val inflater = LayoutInflater.from(this.context)
        val view = inflater.inflate(R.layout.reservation_dialog, null)
        view.findViewById<View>(R.id.button_confirm).setOnClickListener { e: View? ->
            val database = FirebaseDatabase.getInstance()
            val myRef = database.reference
            val query = myRef.child(RESTAURATEUR_INFO + "/" + ROOT_UID + "/" + DISHES_PATH)
                .orderByChild("name").equalTo(dishName)
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (d in dataSnapshot.children) {
                            d.ref.removeValue()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("DAILY OFFER", "Failed to read value.", error.toException())
                }
            })
            mAdapter!!.notifyDataSetChanged()
            reservationDialog.dismiss()
        }
        view.findViewById<View>(R.id.button_cancel)
            .setOnClickListener { e: View? -> reservationDialog.dismiss() }
        reservationDialog.setView(view)
        reservationDialog.setTitle("Delete Dish?")
        reservationDialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_daily, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return when (id) {
            R.id.add -> {
                val edit_profile = Intent(context, EditOffer::class.java)
                startActivityForResult(edit_profile, 0)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
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
        mListener = if (context is DailyOffer.OnFragmentInteractionListener) {
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