package com.mad.customer.UIimport

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.mad.customer.Itemsimport.DishItem
import com.mad.customer.R
import com.mad.customer.UI.Confirm
import com.mad.customer.UI.TabApp
import com.mad.customer.ViewHoldersimport.DailyOfferViewHolder
import com.mad.mylibrary.Restaurateur
import com.mad.mylibrary.SharedClass


class OrderingFragment constructor() : Fragment() {
    //Strings of ordered Items
    var keys: ArrayList<String?> = ArrayList()
    var names: ArrayList<String> = ArrayList()
    var nums: ArrayList<String> = ArrayList()
    var prices: ArrayList<String> = ArrayList()
    private lateinit var recyclerView: RecyclerView
    private var mAdapter: FirebaseRecyclerAdapter<DishItem, DailyOfferViewHolder>? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    public override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true)
        val view: View = inflater.inflate(R.layout.fragment_ordering, container, false)
        recyclerView = view.findViewById<RecyclerView>(R.id.dish_recyclerview)
        layoutManager = LinearLayoutManager(getContext())
        recyclerView.setLayoutManager(layoutManager)
        val key: String? = (getActivity() as TabApp?)!!.key
        val item: Restaurateur? = (getActivity() as TabApp?)?.item
        val options: FirebaseRecyclerOptions<DishItem> = FirebaseRecyclerOptions.Builder<DishItem>()
            .setQuery(FirebaseDatabase.getInstance()
                .getReference(SharedClass.RESTAURATEUR_INFO + "/" + key + "/dishes"),
                object : SnapshotParser<DishItem?> {
                    public override fun parseSnapshot(snapshot: DataSnapshot): DishItem {
                        val dishItem: DishItem
                        if (snapshot.child("photo").getValue() != null) {
                            dishItem = DishItem(
                                snapshot.child("name").getValue().toString(),
                                snapshot.child("desc").getValue().toString(),
                                snapshot.child("price").getValue().toString().toFloat(),
                                snapshot.child("quantity").getValue().toString().toInt(),
                                snapshot.child("photo").getValue().toString()
                            )
                        } else {
                            dishItem = DishItem(
                                snapshot.child("name").getValue().toString(),
                                snapshot.child("desc").getValue().toString(),
                                snapshot.child("price").getValue().toString().toFloat(),
                                snapshot.child("quantity").getValue().toString().toInt(),
                                null
                            )
                        }
                        return dishItem
                    }
                }).build()
        mAdapter = object : FirebaseRecyclerAdapter<DishItem, DailyOfferViewHolder>(options) {
            override fun onBindViewHolder(
                holder: DailyOfferViewHolder,
                position: Int,
                model: DishItem
            ) {
                holder.setData(model, position)
                val numView: TextView = holder.view.findViewById(R.id.dish_num)
                val dish_key: String? = getRef(position).getKey()
                if (keys.contains(dish_key)) {
                    val pos: Int = keys.indexOf(dish_key)
                    val value_num: String = nums.get(pos)
                    numView.setText(value_num)
                    requireActivity().invalidateOptionsMenu()
                } else {
                    numView.setText("0")
                    requireActivity().invalidateOptionsMenu()
                }
                holder.view.findViewById<View>(R.id.add_dish).setOnClickListener(
                    View.OnClickListener({ a: View? ->
                        var num: Int = (numView).getText().toString().toInt()
                        num++
                        if (num > model.quantity) {
                            Toast.makeText(
                                holder.view.getContext(),
                                "Maximum quantity exceeded",
                                Toast.LENGTH_LONG
                            ).show()
                        } else if (num > 99) {
                            Toast.makeText(
                                holder.view.getContext(),
                                "Contact us to get more than this quantity",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            numView.setText(num.toString())
                            AddDish(
                                dish_key,
                                model.name,
                                java.lang.Float.toString(model.price),
                                "add"
                            )
                            requireActivity().invalidateOptionsMenu()
                        }
                        if (!keys.isEmpty()) {
                            view.findViewById<View>(R.id.next)
                                .setBackgroundColor(Color.parseColor("#5aad54"))
                        }
                    })
                )
                holder.view.findViewById<View>(R.id.delete_dish).setOnClickListener(
                    View.OnClickListener({ b: View? ->
                        var num: Int = (numView).getText().toString().toInt()
                        num--
                        if (num < 0) {
                            Toast.makeText(
                                holder.view.getContext(),
                                "Please select the right quantity",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            numView.setText(num.toString())
                            AddDish(
                                dish_key,
                                model.name,
                                java.lang.Float.toString(model.price),
                                "remove"
                            )
                            requireActivity().invalidateOptionsMenu()
                        }
                        if (keys.isEmpty()) {
                            view.findViewById<View>(R.id.next)
                                .setBackgroundColor(Color.parseColor("#c1c1c1"))
                        }
                    })
                )
            }

            public override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): DailyOfferViewHolder {
                val view: View = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.dish_item, parent, false)
                return DailyOfferViewHolder(view)
            }
        }
        recyclerView.setAdapter(mAdapter)

        //Ordine scelto, pulsante per andare avanti
        view.findViewById<View>(R.id.next).setOnClickListener(View.OnClickListener({ w: View? ->
            if (keys.size == 0) {
                Toast.makeText(view.getContext(), "Inserire un piatto.", Toast.LENGTH_LONG)
            } else {
                val intent: Intent = Intent(getContext(), Confirm::class.java)
                intent.putExtra("key", key)
                intent.putExtra("raddr", item!!.addr)
                intent.putExtra("rname", item.name)
                intent.putExtra("photo", item.photoUri)
                intent.putStringArrayListExtra("keys", keys as ArrayList<String?>?)
                intent.putStringArrayListExtra("names", names as ArrayList<String>?)
                intent.putStringArrayListExtra("prices", prices as ArrayList<String>?)
                intent.putStringArrayListExtra("nums", nums as ArrayList<String>?)
                startActivityForResult(intent, 0)
            }
        }))
        return view
    }

    public override fun onStart() {
        super.onStart()
        mAdapter!!.startListening()
    }

    public override fun onStop() {
        super.onStop()
        mAdapter!!.stopListening()
    }

    fun AddDish(key: String?, name: String, price: String, mode: String) {
        if (keys.contains(key) && (mode == "add")) {
            val i: Int = keys.indexOf(key)
            val num: Int = nums.get(i).toInt() + 1
            nums.set(i, num.toString())
        } else if (keys.contains(key) && (mode == "remove")) {
            val i: Int = keys.indexOf(key)
            val num: Int = nums.get(i).toInt() - 1
            if ((num == 0)) {
                keys.removeAt(i)
                nums.removeAt(i)
                names.removeAt(i)
                prices.removeAt(i)
            } else {
                nums.set(i, num.toString())
            }
        } else {
            keys.add(key)
            nums.add("1")
            names.add(name)
            prices.add(price)
        }
    }

    public override fun onPrepareOptionsMenu(menu: Menu) {
        if (keys.size != 0) {
            val menuItem: MenuItem = menu.findItem(R.id.action_custom_button) as MenuItem
            val cart: TextView = menuItem.getActionView()!!.findViewById(R.id.money)
            val snum: String = getQuantity(nums)
            val tot: String = calcoloTotale(prices, nums)
            cart.setText(snum + " | " + tot + "€")
        }
        super.onPrepareOptionsMenu(menu)
    }

    fun getQuantity(nums: ArrayList<String>): String {
        var num: Int = 0
        for (a: String in nums) {
            num += a.toInt()
        }
        val snum: String = Integer.toString(num)
        return snum
    }

    public override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.ordering, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun calcoloTotale(prices: ArrayList<String>, nums: ArrayList<String>): String {
        var tot: Float = 0f
        for (i in prices.indices) {
            val price: Float = prices.get(i).toFloat()
            val num: Float = nums.get(i).toFloat()
            tot = tot + (price * num)
        }
        return java.lang.Float.toString(tot)
    }
}