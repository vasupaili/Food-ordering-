package com.mad.customer.UIimport

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.mad.customer.Adaptersimport.OrderDetailsRecyclerAdapter
import com.mad.customer.Itemsimport.OrderCustomerItem
import com.mad.customer.R
import com.mad.mylibrary.SharedClass

import java.util.Calendar
import java.util.Date
import java.util.function.BiConsumer

class OrderDetails constructor() : AppCompatActivity() {
    private var item: OrderCustomerItem? = null
    private lateinit var recyclerView: RecyclerView
    private var mAdapter: OrderDetailsRecyclerAdapter? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    private val keys: ArrayList<String> = ArrayList()
    private val nums: ArrayList<String> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_details)
        item = getIntent().getSerializableExtra("order_item") as OrderCustomerItem?
        getSupportActionBar()!!.setDisplayShowHomeEnabled(true)
        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
        insertItems()
        insertRecyclerView()
    }

    private fun insertItems() {
        //Set time in correct format
        val d: Date = item!!.time?.let { Date(it) }!!
        val c: Calendar = Calendar.getInstance()
        c.setTime(d)
        val year: Int = c.get(Calendar.YEAR)
        val month: Int = c.get(Calendar.MONTH)
        val day: Int = c.get(Calendar.DATE)
        val date: String = day.toString() + "/" + month + "/" + year
        val twdate: TextView = findViewById(R.id.order_det_date)
        twdate.setText(date)
        //Set restaurant Name and image
        val query: Query =
            FirebaseDatabase.getInstance().getReference(SharedClass.RESTAURATEUR_INFO).child(
                item!!.key!!
            ).child("info")
        query.addValueEventListener(object : ValueEventListener {
            public override fun onDataChange(dataSnapshot: DataSnapshot) {
                (findViewById<View>(R.id.order_res_det_name) as TextView).setText(
                    dataSnapshot.child(
                        "name"
                    ).getValue() as String?
                )
                (findViewById<View>(R.id.order_det_name) as TextView).setText(
                    dataSnapshot.child("name").getValue() as String?
                )
                (findViewById<View>(R.id.order_det_addr) as TextView).setText(
                    dataSnapshot.child("addr").getValue() as String?
                )
                (findViewById<View>(R.id.order_det_cell) as TextView).setText(
                    dataSnapshot.child("phone").getValue() as String?
                )
                if (dataSnapshot.child("photoUri").exists()) {
                    Glide.with(getApplicationContext())
                        .load(dataSnapshot.child("photoUri").getValue()).into(
                        (findViewById<View>(R.id.order_det_image) as ImageView?)!!
                    )
                }
            }

            public override fun onCancelled(databaseError: DatabaseError) {}
        })
        //Set status
        val tw_orrder_status: TextView = (findViewById<View>(R.id.order_det_status) as TextView)
        when (item!!.status) {
            SharedClass.STATUS_UNKNOWN -> tw_orrder_status.setText("Order sent")
            SharedClass.STATUS_DELIVERED -> {
                tw_orrder_status.setText("Order delivered")
                tw_orrder_status.setTextColor(Color.parseColor("#59cc33"))
            }

            SharedClass.STATUS_DISCARDED -> {
                tw_orrder_status.setText("Order refused")
                tw_orrder_status.setTextColor(Color.parseColor("#cc3333"))
            }

            SharedClass.STATUS_DELIVERING -> {
                tw_orrder_status.setText("Delivering...")
                tw_orrder_status.setTextColor(Color.parseColor("#ffb847"))
            }
        }
        //Set total
        (findViewById<View>(R.id.order_det_tot1) as TextView).setText(item!!.totPrice + " €")
        (findViewById<View>(R.id.order_det_tot) as TextView).setText(item!!.totPrice + " €")
        //Set customer addr
        (findViewById<View>(R.id.order_det_deladdr) as TextView).setText(item!!.addrCustomer)
        //Set hour
        val hourValue: Int = c.get(Calendar.HOUR)
        val minValue: Int = c.get(Calendar.MINUTE)
        var hourString: String = Integer.toString(hourValue)
        var minString: String = Integer.toString(minValue)
        if (hourValue < 10) hourString = "0" + hourValue
        if (minValue < 10) minString = "0" + minValue
        val orario: String = hourString + ":" + minString
        (findViewById<View>(R.id.order_det_hour) as TextView).setText(orario)
    }

    private fun insertRecyclerView() {
        recyclerView = findViewById<RecyclerView>(R.id.order_det_recyclerview)
        val knv: HashMap<String, Int> = item!!.dishes!!
        knv.forEach(BiConsumer({ key: String, `val`: Int ->
            keys.add(key)
            nums.add(`val`.toString())
        }))
        mAdapter = OrderDetailsRecyclerAdapter(this, keys, nums, item!!.key!!)
        layoutManager = LinearLayoutManager(this)
        recyclerView.setAdapter(mAdapter)
        recyclerView.setLayoutManager(layoutManager)
    }

    public override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.getItemId()
        if (id == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}