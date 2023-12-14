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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.mad.appetit.R
import com.mad.mylibrary.DishItem
import com.mad.mylibrary.OrderItem
import com.mad.mylibrary.SharedClass.ACCEPTED_ORDER_PATH
import com.mad.mylibrary.SharedClass.DISHES_PATH
import com.mad.mylibrary.SharedClass.RESTAURATEUR_INFO
import com.mad.mylibrary.SharedClass.ROOT_UID
import com.mad.mylibrary.Utilities.getDateFromTimestamp
import java.util.Locale
import java.util.TreeMap


private class ViewHolderDailyOfferTiming(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

class HomeStats : Fragment() {
    private var mListener: OnFragmentInteractionListener? = null
    private lateinit var recyclerView: RecyclerView
    private var layoutManager: RecyclerView.LayoutManager? = null
    private lateinit var mAdapter: FirebaseRecyclerAdapter<DishItem?, ViewHolderDailyOfferTiming?>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_stats, container, false)
        recyclerView = view.findViewById(R.id.dish_list)
        layoutManager = LinearLayoutManager(context)
        recyclerView.setLayoutManager(layoutManager)
        val checkBestTime: Query = FirebaseDatabase.getInstance().reference.child(
            RESTAURATEUR_INFO + "/" +
                    ROOT_UID + "/" + ACCEPTED_ORDER_PATH
        )
        checkBestTime.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val timeOrderMap =
                        TreeMap<String, Int>() //key = hour, value = number of order at that hour
                    val timeDishesMap =
                        HashMap<String, ArrayList<String>>() //key = hour, value = dishes name bought at that hour
                    for (d in dataSnapshot.children) {
                        val orderItem = d.getValue(OrderItem::class.java)
                        val orderTime = getDateFromTimestamp(orderItem!!.time)
                        val hour = orderTime.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0]
                        var value = timeOrderMap[hour]
                        if (value != null) {
                            timeOrderMap[hour] = ++value
                            val dishes: ArrayList<String> = timeDishesMap[hour]!!
                            for (key in orderItem.dishes!!.keys) if (!dishes.contains(key)) dishes.add(
                                key
                            )
                            timeDishesMap[hour] = dishes
                        } else {
                            timeOrderMap[hour] = 0
                            val dishes: ArrayList<String> = ArrayList<String>()
                            for (key in orderItem.dishes!!.keys) if (!dishes.contains(key)) dishes.add(
                                key
                            )
                            timeDishesMap[hour] = dishes
                        }
                    }
                    val bestTime = timeOrderMap.firstEntry().key
                    (view.findViewById<View>(R.id.analytics_text) as TextView).text =
                        getAnalyticsText(bestTime)
                    val mostTimeDishes = timeDishesMap[bestTime]!!
                    val getMostTimeDishes: Query = FirebaseDatabase.getInstance().reference.child(
                        RESTAURATEUR_INFO + "/" +
                                ROOT_UID + "/" + DISHES_PATH
                    )
                    options = FirebaseRecyclerOptions.Builder<DishItem>()
                        .setQuery(getMostTimeDishes) { snapshot: DataSnapshot ->
                            val dishItem = snapshot.getValue(
                                DishItem::class.java
                            )
                            val dishName = dishItem!!.name
                            for (name in mostTimeDishes) {
                                if (name.lowercase(Locale.getDefault()) == dishName.lowercase(Locale.getDefault())) {
                                    return@setQuery dishItem
                                }
                            }
                            DishItem()
                        }.build()
                    mAdapter =
                        object : FirebaseRecyclerAdapter<DishItem?, ViewHolderDailyOfferTiming?>(
                            options
                        ) {
                            override fun onBindViewHolder(
                                holder: ViewHolderDailyOfferTiming,
                                position: Int,
                                model: DishItem
                            ) {
                                if (model.name == "") holder.itemView.visibility =
                                    View.GONE else holder.setData(model, position)
                            }

                            override fun onCreateViewHolder(
                                parent: ViewGroup,
                                viewType: Int
                            ): ViewHolderDailyOfferTiming {
                                val view = LayoutInflater.from(parent.context)
                                    .inflate(R.layout.dailyoffer_listview, parent, false)
                                view.findViewById<View>(R.id.delete_offer).visibility = View.GONE
                                view.findViewById<View>(R.id.edit_offer).visibility = View.GONE
                                return ViewHolderDailyOfferTiming(view)
                            }
                        }
                    recyclerView.setAdapter(mAdapter)
                    mAdapter.startListening()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
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
                if (this@HomeStats.isVisible) requireActivity().supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        Home()
                    ).commit()
                true
            }

            R.id.advanced_stats -> {
                if (!this@HomeStats.isVisible) requireActivity().supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        HomeStats()
                    ).commit()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getAnalyticsText(bestTime: String): String {
        val nexTime = bestTime.toInt() + 1
        val nextTimeString: String
        nextTimeString = if (nexTime < 9) "0$nexTime" else nexTime.toString()
        return "Time slot with more orders is: $bestTime:00 - $nextTimeString:00"
    }

    override fun onResume() {
        super.onResume()
        if (mAdapter != null) mAdapter!!.startListening()
    }

    override fun onStart() {
        super.onStart()
        //mAdapter.startListening();
    }

    override fun onStop() {
        super.onStop()
        if (mAdapter != null) mAdapter!!.stopListening()
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri?) {
        if (mListener != null) {
            mListener!!.onFragmentInteraction(uri)
        }
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
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri?)
    }

    companion object {
        private lateinit var options: FirebaseRecyclerOptions<DishItem?>
    }
}