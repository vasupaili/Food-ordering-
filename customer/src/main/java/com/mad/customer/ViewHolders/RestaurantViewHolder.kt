package com.mad.customer.ViewHoldersimport

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.mad.customer.R
import com.mad.customer.UI.TabApp
import com.mad.mylibrary.Restaurateur
import com.mad.mylibrary.SharedClass
import java.util.Calendar
import java.util.Date
import java.util.LinkedList



class RestaurantViewHolder constructor(itemView: View, private val context: Context) :
    RecyclerView.ViewHolder(itemView), View.OnClickListener {
    private val name: TextView
    private val addr: TextView
    private val cuisine: TextView
    private val opening: TextView
    private val img: ImageView
    private var current: Restaurateur? = null
    private var key: String? = null
    private var favorite_bool: Boolean = false
    private var favorite_visible: Boolean = false
    private var list_favorite: LinkedList<String>? = null
    private val ratingBar: RatingBar
    private val star_value: TextView

    init {
        name = itemView.findViewById(R.id.restaurant_name)
        addr = itemView.findViewById(R.id.listview_address)
        cuisine = itemView.findViewById(R.id.listview_cuisine)
        img = itemView.findViewById(R.id.restaurant_image)
        opening = itemView.findViewById(R.id.listview_opening)
        ratingBar = itemView.findViewById(R.id.ratingBaritem)
        star_value = itemView.findViewById(R.id.textView14)
        itemView.setOnClickListener(this)
    }

    fun setData(current: Restaurateur, position: Int, key: String?) {
        name.setText(current.name)
        addr.setText(current.addr)
        cuisine.setText(current.cuisine)
        opening.setText(current.openingTime)
        if (!(current.photoUri == "null")) {
            Glide.with(itemView).load(current.photoUri).into(img)
        }
        //Opening --> controllo se il ristorante è aperto o chiuso
        val open_h: Int =
            current.openingTime?.split(" - ".toRegex())?.dropLastWhile({ it.isEmpty() })
                ?.toTypedArray()!!.get(0).split(":".toRegex()).dropLastWhile({ it.isEmpty() })
                .toTypedArray().get(0).toInt()
        val open_m: Int =
            current.openingTime?.split(" - ".toRegex())?.dropLastWhile({ it.isEmpty() })!!
                .toTypedArray().get(0).split(":".toRegex()).dropLastWhile({ it.isEmpty() })
                .toTypedArray().get(1).toInt()
        val close_h: Int =
            current.openingTime?.split(" - ".toRegex())?.dropLastWhile({ it.isEmpty() })!!
                .toTypedArray().get(1).split(":".toRegex()).dropLastWhile({ it.isEmpty() })
                .toTypedArray().get(0).toInt()
        val close_m: Int =
            current.openingTime?.split(" - ".toRegex())?.dropLastWhile({ it.isEmpty() })!!
                .toTypedArray().get(1).split(":".toRegex()).dropLastWhile({ it.isEmpty() })
                .toTypedArray().get(1).toInt()
        val opening: Long = getDate(open_h, open_m, 0, 0L)
        val closing: Long = getDate(close_h, close_m, 1, opening)
        if ((System.currentTimeMillis() <= closing) and (System.currentTimeMillis() >= opening)) {
            itemView.findViewById<View>(R.id.imageView4).setVisibility(View.GONE)
        } else {
            Log.d("TAG", "ristorante chiuso")
            itemView.setOnClickListener(null)
        }
        this.current = current
        this.key = key
        var d: Drawable?
        val favorite: ImageView = itemView.findViewById<ImageView>(R.id.star_favorite)
        if (favorite_visible) {
//            for (key_res: String in list_favorite!!) {
            for (key_res: String in list_favorite ?: emptyList()) {
//                if (key_res.compareTo((key)!!) == 0) {
                if (key_res == key) {

                    favorite_bool = true
                    val start: ImageView? = itemView?.findViewById(R.id.star_favorite)
                    start?.setImageResource(R.drawable.heart_fill)
//                    val start: ImageView = itemView.findViewById<ImageView>(R.id.star_favorite)
//                    start.setImageResource(R.drawable.heart_fill)
                }
            }
            favorite.setOnClickListener(View.OnClickListener { e: View? ->
                val ref: DatabaseReference =
                    FirebaseDatabase.getInstance().getReference(SharedClass.CUSTOMER_PATH)
                        .child(SharedClass.ROOT_UID)
                        .child(SharedClass.CUSTOMER_FAVOURITE_RESTAURANT_PATH)
                if (favorite_bool) {
                    ref.child((key)!!).removeValue()
                    val start: ImageView = itemView.findViewById<ImageView>(R.id.star_favorite)
                    start.setImageResource(R.drawable.heart)
                    favorite_bool = false
                    Toast.makeText(
                        context, "Removed from favorite",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val favorite_restaurant: HashMap<String?, Any> = HashMap()
                    favorite_restaurant.put(key, current)
                    ref.updateChildren(favorite_restaurant)
                    val start: ImageView = itemView.findViewById<ImageView>(R.id.star_favorite)
                    start.setImageResource(R.drawable.heart_fill)
                    favorite_bool = true
                    Toast.makeText(
                        context, "Added to favourite",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        } else {
            favorite.setVisibility(View.INVISIBLE)
        }
        val query: Query =
            FirebaseDatabase.getInstance().getReference(SharedClass.RESTAURATEUR_INFO).child(
                (key)!!
            ).child("stars")
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            public override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    if ((dataSnapshot.child("tot_review").getValue() as Long?)!!.toInt() == 0) {
                        ratingBar.setRating(0f)
                        star_value.setVisibility(View.GONE)
                    } else {
                        val s: Float =
                            (dataSnapshot.child("tot_stars").getValue() as Long?)!!.toFloat()
                        val p: Float =
                            (dataSnapshot.child("tot_review").getValue() as Long?)!!.toFloat()
                        ratingBar.setRating(s / p)
                        star_value.setText(String.format("%.2f", s / p))
                    }
                }
            }

            public override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    public override fun onClick(view: View) {
        val intent: Intent = Intent(view.getContext(), TabApp::class.java)
        intent.putExtra("res_item", current)
        intent.putExtra("key", key)
        view.getContext().startActivity(intent)
    }

    fun setFavorite(favorite: LinkedList<String>?) {
        list_favorite = favorite
        favorite_visible = true
    }

    private fun getDate(hour: Int, min: Int, mode: Int, prev: Long): Long {
        val cal: Calendar = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, min)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        var date: Date = cal.getTime()
        if (mode == 1 && date.getTime() < prev) {
            cal.set(Calendar.DATE, cal.get(Calendar.DATE) + 1)
            date = cal.getTime()
        }
        return date.getTime()
    }
}