package com.mad.appetit.HomeActivities

import android.content.Context
import android.net.Uri
import android.os.Bundle
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
import com.google.firebase.database.FirebaseDatabase
import com.mad.appetit.R
import com.mad.mylibrary.DishItem
import com.mad.mylibrary.SharedClass.DISHES_PATH
import com.mad.mylibrary.SharedClass.RESTAURATEUR_INFO
import com.mad.mylibrary.SharedClass.ROOT_UID


private  class ViewHolderDailyOfferMostFavourite(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
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

class Home : Fragment() {
    private var mListener: OnFragmentInteractionListener? = null
    private lateinit var recyclerView: RecyclerView
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var mAdapter: FirebaseRecyclerAdapter<DishItem, ViewHolderDailyOfferMostFavourite>? =
        null
    private val query = FirebaseDatabase.getInstance().reference.child(
        RESTAURATEUR_INFO + "/" +
                ROOT_UID + "/" + DISHES_PATH
    ).orderByChild("frequency")
    private val options = FirebaseRecyclerOptions.Builder<DishItem>()
        .setQuery(query, DishItem::class.java).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        recyclerView = view.findViewById(R.id.dish_list)
        layoutManager = LinearLayoutManager(context)
        recyclerView.setLayoutManager(layoutManager)
        mAdapter = object :
            FirebaseRecyclerAdapter<DishItem, ViewHolderDailyOfferMostFavourite>(options) {
            override fun onBindViewHolder(
                holder: ViewHolderDailyOfferMostFavourite,
                position: Int,
                model: DishItem
            ) {
                holder.setData(model, position)
            }

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): ViewHolderDailyOfferMostFavourite {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.dailyoffer_listview, parent, false)
                view.findViewById<View>(R.id.delete_offer).visibility = View.GONE
                view.findViewById<View>(R.id.edit_offer).visibility = View.GONE
                return ViewHolderDailyOfferMostFavourite(view)
            }
        }
        recyclerView.setAdapter(mAdapter)
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return when (id) {
            R.id.favourite_dishes -> {
                if (!this@Home.isVisible) requireActivity().supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        Home()
                    ).commit()
                true
            }

            R.id.advanced_stats -> {
                if (this@Home.isVisible) requireActivity().supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        HomeStats()
                    ).commit()
                true
            }

            else -> super.onOptionsItemSelected(item)
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
}