package com.mad.customer.UI

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.mad.customer.R
import com.mad.customer.ViewHoldersimport.RestaurantViewHolder

import com.mad.mylibrary.Restaurateur
import com.mad.mylibrary.SharedClass
import java.util.LinkedList
import java.util.Locale

class Restaurant : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var mAdapter: FirebaseRecyclerAdapter<Restaurateur, RestaurantViewHolder>
    private var layoutManager: RecyclerView.LayoutManager? = null
    private val cuisineType = HashSet<String>()
    private val chips = HashSet<Chip>()
    private lateinit var entryChipGroup: ChipGroup
    private var icon_pop = false
    private var menu: Menu? = null
    private val flag = true
    private var favourites_selected = false
    var keys_favorite_restaurant: LinkedList<String>? = null
    private var options = FirebaseRecyclerOptions.Builder<Restaurateur>()
        .setQuery(
            FirebaseDatabase.getInstance().getReference(SharedClass.RESTAURATEUR_INFO)
        ) { snapshot ->
            val searchRest: Restaurateur
            if (snapshot.child("info").child("photoUri").value == null) {
                searchRest = Restaurateur(
                    snapshot.child("info").child("mail").value.toString(),
                    snapshot.child("info").child("name").value.toString(),
                    snapshot.child("info").child("addr").value.toString(),
                    snapshot.child("info").child("cuisine").value.toString(),
                    snapshot.child("info").child("openingTime").value.toString(),
                    snapshot.child("info").child("phone").value.toString(),
                    "null"
                )
            } else {
                searchRest = Restaurateur(
                    snapshot.child("info").child("mail").value.toString(),
                    snapshot.child("info").child("name").value.toString(),
                    snapshot.child("info").child("addr").value.toString(),
                    snapshot.child("info").child("cuisine").value.toString(),
                    snapshot.child("info").child("openingTime").value.toString(),
                    snapshot.child("info").child("phone").value.toString(),
                    snapshot.child("info").child("photoUri").value.toString()
                )
            }
            searchRest
        }.build()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view : View = inflater.inflate(R.layout.fragment_restaurant, container, false)
        setHasOptionsMenu(true)
        entryChipGroup = view.findViewById(R.id.chip_group)
        recyclerView = view.findViewById(R.id.restaurant_recyclerview)
        //recyclerView.setHasFixedSize(true);
        layoutManager = LinearLayoutManager(context)
        recyclerView.setLayoutManager(layoutManager)
        mAdapter =
            object : FirebaseRecyclerAdapter<Restaurateur, RestaurantViewHolder>(options) {
                override fun onBindViewHolder(
                    holder: RestaurantViewHolder,
                    position: Int,
                    model: Restaurateur
                ) {
                    val key = getRef(position).key
                    holder.setIsRecyclable(false)
                    holder.setData(model, position, key)
                }

                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int
                ): RestaurantViewHolder {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.restaurant_item, parent, false)
                    val resViewHolder = RestaurantViewHolder(view, context!!)
                    resViewHolder.setFavorite(keys_favorite_restaurant)
                    return resViewHolder
                }
            }
        recyclerView.setAdapter(mAdapter)
        mAdapter.startListening()

        val fav_ref = FirebaseDatabase
            .getInstance().getReference(SharedClass.CUSTOMER_PATH)
            .child(SharedClass.ROOT_UID).child(SharedClass.CUSTOMER_FAVOURITE_RESTAURANT_PATH)
        fav_ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                keys_favorite_restaurant = LinkedList()
                for (d in dataSnapshot.children) {
                    keys_favorite_restaurant!!.add(d.key!!)
                }
                mAdapter.startListening()

            }

            override fun onCancelled(databaseError: DatabaseError) {}
           // mAdapter.stopListening();

        })
     //   recyclerView.setAdapter(mAdapter)
        val query: Query =
            FirebaseDatabase.getInstance().getReference(SharedClass.RESTAURATEUR_INFO)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (d in dataSnapshot.children) {
                        if (cuisineType.add(d.child("info").child("cuisine").value.toString())) {
                            Log.d("CHIP", "building")
                            val chip = Chip(view.context)
                            chip.isCheckable = true
                            chip.text = d.child("info").child("cuisine").value.toString()
                            chips.add(chip)
                            entryChipGroup.addView(chip)
                            chip.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                                if (isChecked && flag) {
                                    setFilter(
                                        d.child("info").child("cuisine").getValue().toString()
                                    )
                                }
                            }
                        }
                    }
                }
                entryChipGroup.setOnCheckedChangeListener(ChipGroup.OnCheckedChangeListener { chipGroup: ChipGroup, i: Int ->
                    if (chipGroup.checkedChipId == View.NO_ID) {
                        mAdapter!!.stopListening()
                        options = FirebaseRecyclerOptions.Builder<Restaurateur>()
                            .setQuery(FirebaseDatabase.getInstance()
                                .getReference(SharedClass.RESTAURATEUR_INFO),
                                SnapshotParser<Restaurateur?> { snapshot ->
                                    val searchRest: Restaurateur
                                    if (snapshot.child("info").child("photoUri").value == null) {
                                        searchRest = Restaurateur(
                                            snapshot.child("info").child("mail").value.toString(),
                                            snapshot.child("info").child("name").value.toString(),
                                            snapshot.child("info").child("addr").value.toString(),
                                            snapshot.child("info")
                                                .child("cuisine").value.toString(),
                                            snapshot.child("info")
                                                .child("openingTime").value.toString(),
                                            snapshot.child("info").child("phone").value.toString(),
                                            "null"
                                        )
                                    } else {
                                        searchRest = Restaurateur(
                                            snapshot.child("info").child("mail").value.toString(),
                                            snapshot.child("info").child("name").value.toString(),
                                            snapshot.child("info").child("addr").value.toString(),
                                            snapshot.child("info")
                                                .child("cuisine").value.toString(),
                                            snapshot.child("info")
                                                .child("openingTime").value.toString(),
                                            snapshot.child("info").child("phone").value.toString(),
                                            snapshot.child("info")
                                                .child("photoUri").value.toString()
                                        )
                                    }

                                     searchRest
                                }).build()
                        mAdapter = object :
                            FirebaseRecyclerAdapter<Restaurateur, RestaurantViewHolder>(options) {
                            override fun onBindViewHolder(
                                holder: RestaurantViewHolder,
                                position: Int,
                                model: Restaurateur
                            ) {
                                val key = getRef(position).key
                                holder.setData(model, position, key)
                            }

                            override fun onCreateViewHolder(
                                parent: ViewGroup,
                                viewType: Int
                            ): RestaurantViewHolder {
                                val view1 = LayoutInflater.from(parent.context)
                                    .inflate(R.layout.restaurant_item, parent, false)
                                val resViewHolder = RestaurantViewHolder(view1, context!!)
                                resViewHolder.setFavorite(keys_favorite_restaurant)
                                return resViewHolder
                            }
                        }
                        recyclerView.setAdapter(mAdapter)
                        mAdapter.startListening()
                    }
                })
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search, menu)
        val searchItem = menu.findItem(R.id.search)
        val searchView = searchItem.actionView as SearchView?
        this.menu = menu
        val heart = menu.findItem(R.id.favorite_res)
        heart.setOnMenuItemClickListener { e: MenuItem? ->
            favourites_selected = !favourites_selected
            mAdapter.stopListening()
            if (favourites_selected) {
                heart.setIcon(R.drawable.heart_fill_white)
                options = FirebaseRecyclerOptions.Builder<Restaurateur>()
                    .setQuery(FirebaseDatabase.getInstance()
                        .getReference(SharedClass.CUSTOMER_PATH)
                        .child(SharedClass.ROOT_UID)
                        .child(SharedClass.CUSTOMER_FAVOURITE_RESTAURANT_PATH),
                        object : SnapshotParser<Restaurateur?> {
                            override fun parseSnapshot(snapshot: DataSnapshot): Restaurateur {
                                val searchRest: Restaurateur
                                if (snapshot.child("photoUri").getValue() == null) {
                                    searchRest = Restaurateur(
                                        snapshot.child("mail").getValue().toString(),
                                        snapshot.child("name").getValue().toString(),
                                        snapshot.child("addr").getValue().toString(),
                                        snapshot.child("cuisine").getValue().toString(),
                                        snapshot.child("openingTime").getValue().toString(),
                                        snapshot.child("phone").getValue().toString(),
                                        "null"
                                    )
                                } else {
                                    searchRest = Restaurateur(
                                        snapshot.child("mail").getValue().toString(),
                                        snapshot.child("name").getValue().toString(),
                                        snapshot.child("addr").getValue().toString(),
                                        snapshot.child("cuisine").getValue().toString(),
                                        snapshot.child("openingTime").getValue().toString(),
                                        snapshot.child("phone").getValue().toString(),
                                        snapshot.child("photoUri").getValue().toString()
                                    )
                                }
                                return searchRest
                            }
                        }).build()
            } else {
                heart.setIcon(ContextCompat.getDrawable((getContext())!!, R.drawable.heart_white))
                options = FirebaseRecyclerOptions.Builder<Restaurateur>()
                    .setQuery(FirebaseDatabase.getInstance()
                        .getReference(SharedClass.RESTAURATEUR_INFO),
                        object : SnapshotParser<Restaurateur?> {
                            override fun parseSnapshot(snapshot: DataSnapshot): Restaurateur {
                                val searchRest: Restaurateur
                                if (snapshot.child("info").child("photoUri").getValue() == null) {
                                    searchRest = Restaurateur(
                                        snapshot.child("info").child("mail").getValue().toString(),
                                        snapshot.child("info").child("name").getValue().toString(),
                                        snapshot.child("info").child("addr").getValue().toString(),
                                        snapshot.child("info").child("cuisine").getValue()
                                            .toString(),
                                        snapshot.child("info").child("openingTime").getValue()
                                            .toString(),
                                        snapshot.child("info").child("phone").getValue().toString(),
                                        "null"
                                    )
                                } else {
                                    searchRest = Restaurateur(
                                        snapshot.child("info").child("mail").getValue().toString(),
                                        snapshot.child("info").child("name").getValue().toString(),
                                        snapshot.child("info").child("addr").getValue().toString(),
                                        snapshot.child("info").child("cuisine").getValue()
                                            .toString(),
                                        snapshot.child("info").child("openingTime").getValue()
                                            .toString(),
                                        snapshot.child("info").child("phone").getValue().toString(),
                                        snapshot.child("info").child("photoUri").getValue()
                                            .toString()
                                    )
                                }
                                return searchRest
                            }
                        }).build()
            }
            mAdapter =
                object : FirebaseRecyclerAdapter<Restaurateur, RestaurantViewHolder>(options) {
                    override fun onBindViewHolder(
                        holder: RestaurantViewHolder,
                        position: Int,
                        model: Restaurateur
                    ) {
                        val key: String? = getRef(position).getKey()
                        holder.setData(model, position, key)
                    }

                    override fun onCreateViewHolder(
                        parent: ViewGroup,
                        viewType: Int
                    ): RestaurantViewHolder {
                        val view: View = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.restaurant_item, parent, false)
                        val resViewHolder: RestaurantViewHolder =
                            RestaurantViewHolder(view, requireContext())
                        resViewHolder.setFavorite(keys_favorite_restaurant)
                        return resViewHolder
                    }
                }
            recyclerView.setAdapter(mAdapter)
            mAdapter.startListening()
            false
        }
        val pop = menu.findItem(R.id.most_popular_res)
        pop.setOnMenuItemClickListener { d: MenuItem? ->
            icon_pop = !icon_pop
            if (icon_pop) {
                pop.setIcon(R.drawable.ic_restaurant)
                mAdapter!!.stopListening()
                options = FirebaseRecyclerOptions.Builder<Restaurateur>()
                    .setQuery(FirebaseDatabase.getInstance()
                        .getReference(SharedClass.RESTAURATEUR_INFO)
                        .orderByChild("stars/sort"),
                        object : SnapshotParser<Restaurateur?> {
                            override fun parseSnapshot(snapshot: DataSnapshot): Restaurateur {
                                val searchRest: Restaurateur
                                if (snapshot.child("info").child("photoUri").getValue() == null) {
                                    searchRest = Restaurateur(
                                        snapshot.child("info").child("mail").getValue().toString(),
                                        snapshot.child("info").child("name").getValue().toString(),
                                        snapshot.child("info").child("addr").getValue().toString(),
                                        snapshot.child("info").child("cuisine").getValue()
                                            .toString(),
                                        snapshot.child("info").child("openingTime").getValue()
                                            .toString(),
                                        snapshot.child("info").child("phone").getValue().toString(),
                                        "null"
                                    )
                                } else {
                                    searchRest = Restaurateur(
                                        snapshot.child("info").child("mail").getValue().toString(),
                                        snapshot.child("info").child("name").getValue().toString(),
                                        snapshot.child("info").child("addr").getValue().toString(),
                                        snapshot.child("info").child("cuisine").getValue()
                                            .toString(),
                                        snapshot.child("info").child("openingTime").getValue()
                                            .toString(),
                                        snapshot.child("info").child("phone").getValue().toString(),
                                        snapshot.child("info").child("photoUri").getValue()
                                            .toString()
                                    )
                                }
                                return searchRest
                            }
                        }).build()
            } else {
                pop.setIcon(R.drawable.ic_chart)
                mAdapter.stopListening()
                options = FirebaseRecyclerOptions.Builder<Restaurateur>()
                    .setQuery(FirebaseDatabase.getInstance()
                        .getReference(SharedClass.RESTAURATEUR_INFO),
                        object : SnapshotParser<Restaurateur?> {
                            override fun parseSnapshot(snapshot: DataSnapshot): Restaurateur {
                                val searchRest: Restaurateur
                                if (snapshot.child("info").child("photoUri").getValue() == null) {
                                    searchRest = Restaurateur(
                                        snapshot.child("info").child("mail").getValue().toString(),
                                        snapshot.child("info").child("name").getValue().toString(),
                                        snapshot.child("info").child("addr").getValue().toString(),
                                        snapshot.child("info").child("cuisine").getValue()
                                            .toString(),
                                        snapshot.child("info").child("openingTime").getValue()
                                            .toString(),
                                        snapshot.child("info").child("phone").getValue().toString(),
                                        "null"
                                    )
                                } else {
                                    searchRest = Restaurateur(
                                        snapshot.child("info").child("mail").getValue().toString(),
                                        snapshot.child("info").child("name").getValue().toString(),
                                        snapshot.child("info").child("addr").getValue().toString(),
                                        snapshot.child("info").child("cuisine").getValue()
                                            .toString(),
                                        snapshot.child("info").child("openingTime").getValue()
                                            .toString(),
                                        snapshot.child("info").child("phone").getValue().toString(),
                                        snapshot.child("info").child("photoUri").getValue()
                                            .toString()
                                    )
                                }
                                return searchRest
                            }
                        }).build()
            }
            mAdapter =
                object : FirebaseRecyclerAdapter<Restaurateur, RestaurantViewHolder>(options) {
                    override fun onBindViewHolder(
                        holder: RestaurantViewHolder,
                        position: Int,
                        model: Restaurateur
                    ) {
                        val key: String? = getRef(position).getKey()
                        holder.setData(model, position, key)
                    }

                    override fun onCreateViewHolder(
                        parent: ViewGroup,
                        viewType: Int
                    ): RestaurantViewHolder {
                        val view: View = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.restaurant_item, parent, false)
                        val resViewHolder: RestaurantViewHolder =
                            RestaurantViewHolder(view, requireContext())
                        resViewHolder.setFavorite(keys_favorite_restaurant)
                        return resViewHolder
                    }
                }
            recyclerView.setAdapter(mAdapter)
            mAdapter.startListening()
            false
        }
        searchView!!.setOnCloseListener {
            entryChipGroup.setVisibility(View.VISIBLE)
            requireActivity().findViewById<View>(R.id.navigation).setVisibility(View.VISIBLE)
            false
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                mAdapter.stopListening()
                if (newText.length == 0) {
                    entryChipGroup!!.visibility = View.GONE
                    activity!!.findViewById<View>(R.id.navigation).visibility = View.GONE
                    options = FirebaseRecyclerOptions.Builder<Restaurateur>()
                        .setQuery(
                            FirebaseDatabase.getInstance()
                                .getReference(SharedClass.RESTAURATEUR_INFO)
                        ) { snapshot: DataSnapshot ->
                            val searchRest: Restaurateur
                            if (snapshot.child("info").child("photoUri").getValue() == null) {
                                searchRest = Restaurateur(
                                    snapshot.child("info").child("mail").getValue().toString(),
                                    snapshot.child("info").child("name").getValue().toString(),
                                    snapshot.child("info").child("addr").getValue().toString(),
                                    snapshot.child("info").child("cuisine").getValue().toString(),
                                    snapshot.child("info").child("openingTime").getValue()
                                        .toString(),
                                    snapshot.child("info").child("phone").getValue().toString(),
                                    "null"
                                )
                            } else {
                                searchRest = Restaurateur(
                                    snapshot.child("info").child("mail").getValue().toString(),
                                    snapshot.child("info").child("name").getValue().toString(),
                                    snapshot.child("info").child("addr").getValue().toString(),
                                    snapshot.child("info").child("cuisine").getValue().toString(),
                                    snapshot.child("info").child("openingTime").getValue()
                                        .toString(),
                                    snapshot.child("info").child("phone").getValue().toString(),
                                    snapshot.child("info").child("photoUri").getValue().toString()
                                )
                            }
                            searchRest
                        }.build()
                } else {
                    entryChipGroup!!.visibility = View.GONE
                    activity!!.findViewById<View>(R.id.navigation).visibility = View.GONE
                    options = FirebaseRecyclerOptions.Builder<Restaurateur>()
                        .setQuery(FirebaseDatabase.getInstance().reference.child(SharedClass.RESTAURATEUR_INFO)) { snapshot: DataSnapshot ->
                            var searchRest: Restaurateur = Restaurateur()
                            if (snapshot.child("info").child("name")
                                    .exists() && snapshot.child("info").child("name").getValue()
                                    .toString().lowercase(
                                    Locale.getDefault()
                                ).contains(newText.lowercase(Locale.getDefault()))
                            ) {
                                if (snapshot.child("info").child("photoUri").getValue() != null) {
                                    searchRest = Restaurateur(
                                        snapshot.child("info").child("mail").getValue().toString(),
                                        snapshot.child("info").child("name").getValue().toString(),
                                        snapshot.child("info").child("addr").getValue().toString(),
                                        snapshot.child("info").child("cuisine").getValue()
                                            .toString(),
                                        snapshot.child("info").child("openingTime").getValue()
                                            .toString(),
                                        snapshot.child("info").child("phone").getValue().toString(),
                                        snapshot.child("info").child("photoUri").getValue()
                                            .toString()
                                    )
                                } else {
                                    searchRest = Restaurateur(
                                        snapshot.child("info").child("mail").getValue().toString(),
                                        snapshot.child("info").child("name").getValue().toString(),
                                        snapshot.child("info").child("addr").getValue().toString(),
                                        snapshot.child("info").child("cuisine").getValue()
                                            .toString(),
                                        snapshot.child("info").child("phone").getValue().toString(),
                                        snapshot.child("info").child("openingTime").getValue()
                                            .toString(),
                                        "null"
                                    )
                                }
                            }
                            searchRest
                        }.build()
                }
                mAdapter =
                    object : FirebaseRecyclerAdapter<Restaurateur, RestaurantViewHolder>(options) {
                        override fun onBindViewHolder(
                            holder: RestaurantViewHolder,
                            position: Int,
                            model: Restaurateur
                        ) {
                            val key = getRef(position).key
                            if (model.name == "") {
                                holder.itemView.findViewById<View>(R.id.restaurant).layoutParams =
                                    FrameLayout.LayoutParams(0, 0)
                                //holder.itemView.setLayoutParams(new ConstraintLayout.LayoutParams(0,0));
                            } else {
                                holder.setData(model, position, key)
                            }
                        }

                        override fun onCreateViewHolder(
                            parent: ViewGroup,
                            viewType: Int
                        ): RestaurantViewHolder {
                            val view = LayoutInflater.from(parent.context)
                                .inflate(R.layout.restaurant_item, parent, false)
                            return RestaurantViewHolder(view, context!!)
                        }
                    }
                recyclerView!!.adapter = mAdapter
                mAdapter.startListening()
                return false
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        return when (id) {
            R.id.search -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setFilter(filter: String) {
        mAdapter!!.stopListening()
        val options = FirebaseRecyclerOptions.Builder<Restaurateur>()
            .setQuery(FirebaseDatabase.getInstance().reference.child(SharedClass.RESTAURATEUR_INFO)) { snapshot ->
                var searchRest = Restaurateur()
                if (snapshot.child("info").child("cuisine").exists() && (snapshot.child("info")
                        .child("cuisine").value.toString() == filter)
                ) {
                    if (snapshot.child("info").child("photoUri").value != null) {
                        searchRest = Restaurateur(
                            snapshot.child("info").child("mail").value.toString(),
                            snapshot.child("info").child("name").value.toString(),
                            snapshot.child("info").child("addr").value.toString(),
                            snapshot.child("info").child("cuisine").value.toString(),
                            snapshot.child("info").child("openingTime").value.toString(),
                            snapshot.child("info").child("phone").value.toString(),
                            snapshot.child("info").child("photoUri").value.toString()
                        )
                    } else {
                        searchRest = Restaurateur(
                            snapshot.child("info").child("mail").value.toString(),
                            snapshot.child("info").child("name").value.toString(),
                            snapshot.child("info").child("addr").value.toString(),
                            snapshot.child("info").child("cuisine").value.toString(),
                            snapshot.child("info").child("openingTime").value.toString(),
                            snapshot.child("info").child("phone").value.toString(),
                            "null"
                        )
                    }
                }
                searchRest
            }.build()
        mAdapter = object : FirebaseRecyclerAdapter<Restaurateur, RestaurantViewHolder>(options) {
            override fun onBindViewHolder(
                holder: RestaurantViewHolder,
                position: Int,
                model: Restaurateur
            ) {
                val key = getRef(position).key
                if (model.name == "") {
                    holder.itemView.findViewById<View>(R.id.restaurant).layoutParams =
                        FrameLayout.LayoutParams(0, 0)
                    //holder.itemView.setLayoutParams(new ConstraintLayout.LayoutParams(0,0));
                } else {
                    holder.setData(model, position, key)
                }
            }

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RestaurantViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.restaurant_item, parent, false)
                return RestaurantViewHolder(view, context!!)
            }
        }
        recyclerView!!.adapter = mAdapter
        mAdapter.startListening()
    }

    override fun onStart() {
        super.onStart()
        if (mAdapter != null) {
            mAdapter!!.startListening()
        }
    }

    override fun onStop() {
        super.onStop()
        mAdapter!!.stopListening()
    }

    override fun onResume() {
        super.onResume()
    }

    interface OnFragmentInteractionListener
}