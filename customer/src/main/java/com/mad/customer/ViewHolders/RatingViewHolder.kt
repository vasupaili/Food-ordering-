package com.mad.customer.ViewHoldersimport

import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.mad.customer.R
import com.mad.mylibrary.ReviewItem
import com.mad.mylibrary.SharedClass


class RatingViewHolder constructor(val view: View) : RecyclerView.ViewHolder(
    view
) {
    private val name: TextView
    private val comment: TextView
    private val ratingBar: RatingBar
    private val img: ImageView

    init {
        name = view.findViewById(R.id.rating_item_name)
        comment = view.findViewById(R.id.rating_item_comment)
        ratingBar = view.findViewById(R.id.ratingbaritem)
        img = view.findViewById(R.id.rating_item_img)
    }

    fun setData(ri: ReviewItem) {
        name.setText(ri.name)
        if (ri.comment != null) {
            comment.setText(ri.comment)
        } else {
            comment.setVisibility(View.GONE)
        }
        val query: Query = FirebaseDatabase.getInstance().getReference(SharedClass.CUSTOMER_PATH)
            .child(ri.user_key!!).child("customer_info").child("photoPath")
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            public override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    Glide.with(view).load(dataSnapshot.getValue()).into(img)
                }
            }

            public override fun onCancelled(databaseError: DatabaseError) {}
        }
        )
        ratingBar.setRating(ri.stars.toFloat())
    }
}