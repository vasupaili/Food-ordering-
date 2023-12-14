package com.mad.customer.UI

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.mad.customer.Adaptersimport.SectionsPageAdapter
import com.mad.customer.R
import com.mad.customer.UIimport.OrderingFragment
import com.mad.customer.UIimport.RatingFragment
import com.mad.mylibrary.Restaurateur
import com.mad.mylibrary.SharedClass


class TabApp : AppCompatActivity() {
    //Info about restaurant
    var item: Restaurateur? = null
        private set
    var key: String? = null
        private set

    //Handle switch of tabs
    private var mSectionsPageAdapter: SectionsPageAdapter? = null
    private var mViewPager: ViewPager? = null
    private val stars: Long = 0
    private val count: Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tab_app)
        val r = findViewById<RatingBar>(R.id.ratingbar)

        //r.setNumStars();
        incomingIntent

        //Functions for menu
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        mSectionsPageAdapter = SectionsPageAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById<View>(R.id.view_pager_id) as ViewPager
        setupViewPager(mViewPager)
        val tabLayout = findViewById<View>(R.id.tab_layout_id) as TabLayout
        tabLayout.setupWithViewPager(mViewPager)
    }

    private fun setupViewPager(viewPager: ViewPager?) {
        val adapter = SectionsPageAdapter(supportFragmentManager)
        adapter.addFragment(OrderingFragment(), "Order")
        adapter.addFragment(RatingFragment(), "Rewiew")
        viewPager!!.adapter = adapter
    }

    private val incomingIntent: Unit
        private get() {
            item = intent.getSerializableExtra("res_item") as Restaurateur?
            key = intent.getStringExtra("key")
            val query: Query =
                FirebaseDatabase.getInstance().getReference(SharedClass.RESTAURATEUR_INFO).child(
                    key!!
                ).child("stars")
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val r = findViewById<RatingBar>(R.id.ratingbar)
                    val number = findViewById<View>(R.id.number_rating) as TextView
                    if (dataSnapshot.exists()) {
                        val s = (dataSnapshot.child("tot_stars").value as Long?)!!.toFloat()
                        val p = (dataSnapshot.child("tot_review").value as Long?)!!.toFloat()
                        if (p != 0f) {
                            r.rating = s / p
                            number.text = String.format("%.2f", s / p)
                        } else {
                            r.rating = 0f
                            number.visibility = View.GONE
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
            item!!.openingTime?.let {
                item!!.photoUri?.let { it1 ->
                    setFields(
                        item!!.name,
                        item!!.addr,
                        item!!.phone,
                        item!!.cuisine,
                        item!!.mail,
                        it,
                        it1
                    )
                }
            }
        }

    private fun setFields(
        name: String,
        addr: String,
        cell: String,
        description: String,
        email: String,
        opening: String,
        img: String
    ) {
        val mname = findViewById<TextView>(R.id.rest_info_name)
        val maddr = findViewById<TextView>(R.id.rest_info_addr)
        val mcell = findViewById<TextView>(R.id.rest_info_cell)
        val memail = findViewById<TextView>(R.id.rest_info_mail)
        val mimg = findViewById<ImageView>(R.id.imageView)
        mname.text = name
        maddr.text = addr
        mcell.text = cell
        memail.text = email
        if (img != "null") {
            Glide.with(applicationContext)
                .load(img)
                .into(mimg)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == 1) {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    } /*@Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }*/
}