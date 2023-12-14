package com.mad.customer.ViewHoldersimport

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast


import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.hsalf.smilerating.SmileRating
import com.mad.customer.Itemsimport.OrderCustomerItem
import com.mad.customer.R
import com.mad.mylibrary.ReviewItem
import com.mad.mylibrary.SharedClass
import com.mad.mylibrary.StarItem

import java.util.Calendar
import java.util.Date



class OrderViewHolder constructor(var view: View) : RecyclerView.ViewHolder(
    view
) {
    private val name: TextView
    private val date: TextView
    private val delivery: TextView
    private val total: TextView
    private val img: ImageView

    init {
        name = view.findViewById(R.id.order_res_name)
        date = view.findViewById(R.id.order_date)
        delivery = view.findViewById(R.id.order_status)
        total = view.findViewById(R.id.order_tot)
        img = view.findViewById(R.id.order_image)
    }

    fun setData(current: OrderCustomerItem, position: Int, orderKey: String) {
        //Set time in correct format
        val d: Date? = current.time?.let { Date(it) }
        val c: Calendar = Calendar.getInstance()
        c.setTime(d)
        val year: Int = c.get(Calendar.YEAR)
        val month: Int = c.get(Calendar.MONTH) + 1
        val day: Int = c.get(Calendar.DAY_OF_MONTH)
        val date: String = day.toString() + "/" + month + "/" + year
        this.date.setText(date)
        when (current.status) {
            SharedClass.STATUS_UNKNOWN -> delivery.setText("Order sent")
            SharedClass.STATUS_DELIVERED -> {
                delivery.setText("Order delivered")
                delivery.setTextColor(Color.parseColor("#59cc33"))
            }

            SharedClass.STATUS_DISCARDED -> {
                delivery.setText("Order refused")
                delivery.setTextColor(Color.parseColor("#cc3333"))
            }

            SharedClass.STATUS_DELIVERING -> {
                delivery.setText("Delivering...")
                delivery.setTextColor(Color.parseColor("#ffb847"))
            }
        }
        total.setText(current.totPrice + " €")
        val query: Query? =
            current.key?.let {
                FirebaseDatabase.getInstance().getReference(SharedClass.RESTAURATEUR_INFO)
                    .child(it).child("info")

            }
        if (query != null) {
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                public override fun onDataChange(dataSnapshot: DataSnapshot) {
                    name.setText(dataSnapshot.child("name").getValue() as String?)
                    if (dataSnapshot.child("photoUri").exists()) {
                        Glide.with(view).load(dataSnapshot.child("photoUri").getValue()).into(img)
                    }
                }

                public override fun onCancelled(databaseError: DatabaseError) {}
            })
        }
        val rate: Button = (view.findViewById<View>(R.id.order_rate_button) as Button)
        if (current.isRated) {
            rate.setVisibility(View.GONE)
        } else {
            rate.setOnClickListener(View.OnClickListener { a: View? ->
                current.key?.let {
                    showAlertDialogDelivered(
                        it,
                        orderKey
                    )
                }
            })
        }
    }

    private fun showAlertDialogDelivered(resKey: String, orderKey: String) {
        val query: Query =
            FirebaseDatabase.getInstance().getReference(SharedClass.RESTAURATEUR_INFO).child(resKey)
                .child("info")
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            public override fun onDataChange(dataSnapshot: DataSnapshot) {
                val alertDialog: AlertDialog = AlertDialog.Builder(view.getContext()).create()
                val factory: LayoutInflater = LayoutInflater.from(view.getContext())
                val view: View = factory.inflate(R.layout.rating_dialog, null)
                alertDialog.setView(view)
                if (dataSnapshot.child("photoUri").exists()) {
                    Glide.with(view).load(dataSnapshot.child("photoUri").getValue())
                        .into((view.findViewById<View>(R.id.dialog_rating_icon) as ImageView?)!!)
                }
                val smileRating: SmileRating =
                    view.findViewById<View>(R.id.dialog_rating_rating_bar) as SmileRating
                //Button confirm pressed
                view.findViewById<View>(R.id.dialog_rating_button_positive).setOnClickListener(
                    View.OnClickListener { a: View? ->
                        if (smileRating.getRating() != 0) {
                            val myRef: DatabaseReference = FirebaseDatabase.getInstance()
                                .getReference(SharedClass.RESTAURATEUR_INFO + "/" + resKey)
                                .child("review")
                            val myRef2: DatabaseReference = FirebaseDatabase.getInstance()
                                .getReference(SharedClass.CUSTOMER_PATH).child(SharedClass.ROOT_UID)
                                .child("orders").child(orderKey)
                            val rated: HashMap<String, Any> = HashMap()
                            val review: HashMap<String?, Any> = HashMap()
                            val comment: String =
                                (view.findViewById<View>(R.id.dialog_rating_feedback) as EditText).getText()
                                    .toString()
                            updateRestaurantStars(resKey, smileRating.getRating())
                            if (!comment.isEmpty()) {
                                rated.put("rated", true)
                                myRef2.updateChildren(rated)
                                review.put(
                                    myRef.push().getKey(),
                                    ReviewItem(
                                        smileRating.getRating(),
                                        comment,
                                        SharedClass.ROOT_UID,
                                        SharedClass.user?.photoPath,
                                        SharedClass.user?.name
                                    )
                                )
                                myRef.updateChildren(review)
                            } else {
                                rated.put("rated", true)
                                myRef2.updateChildren(rated)
                                review.put(
                                    myRef.push().getKey(),
                                    ReviewItem(
                                        smileRating.getRating(),
                                        null,
                                        SharedClass.ROOT_UID,
                                        SharedClass.user?.photoPath,
                                        SharedClass.user?.name
                                    )
                                )
                                myRef.updateChildren(review)
                            }
                            Toast.makeText(
                                view.getContext(),
                                "Thanks for your review!",
                                Toast.LENGTH_LONG
                            ).show()
                            alertDialog.dismiss()
                        } else {
                            Toast.makeText(
                                view.getContext(),
                                "You forgot to rate!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
                view.findViewById<View>(R.id.dialog_rating_button_negative).setOnClickListener(
                    View.OnClickListener({ b: View? -> alertDialog.dismiss() })
                )
                alertDialog.show()
            }

            public override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun updateRestaurantStars(resKey: String, stars: Int) {
        val query: Query =
            FirebaseDatabase.getInstance().getReference(SharedClass.RESTAURATEUR_INFO).child(resKey)
                .child("stars")
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            public override fun onDataChange(dataSnapshot: DataSnapshot) {
                val star: HashMap<String, Any> = HashMap()
                val myRef: DatabaseReference = FirebaseDatabase.getInstance()
                    .getReference(SharedClass.RESTAURATEUR_INFO + "/" + resKey)
                if (dataSnapshot.exists()) {
                    val s: Int = (dataSnapshot.child("tot_stars").getValue() as Long?)!!.toInt()
                    val p: Int = (dataSnapshot.child("tot_review").getValue() as Long?)!!.toInt()
                    star.put("stars", StarItem(s + stars, p + 1, -s - stars))
                    myRef.updateChildren(star)
                }
            }

            public override fun onCancelled(databaseError: DatabaseError) {}
        })
    }
}