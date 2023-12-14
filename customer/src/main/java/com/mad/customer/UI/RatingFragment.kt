package com.mad.customer.UIimport


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.FirebaseDatabase
import com.mad.customer.R
import com.mad.customer.UI.TabApp
import com.mad.customer.ViewHoldersimport.RatingViewHolder
import com.mad.mylibrary.Restaurateur
import com.mad.mylibrary.ReviewItem
import com.mad.mylibrary.SharedClass


class RatingFragment constructor() : Fragment() {
    private var item: Restaurateur? = null
    private lateinit var recyclerView: RecyclerView
    private var mAdapter: FirebaseRecyclerAdapter<ReviewItem, RatingViewHolder>? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    public override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_rating, container, false)
        RatingFragment.Companion.key = (getActivity() as TabApp?)?.key
        item = (getActivity() as TabApp?)?.item
        val options: FirebaseRecyclerOptions<ReviewItem> =
            FirebaseRecyclerOptions.Builder<ReviewItem>()
                .setQuery(
                    FirebaseDatabase.getInstance().getReference(SharedClass.RESTAURATEUR_INFO)
                        .child(RatingFragment.Companion.key!!).child("review"),
                    ReviewItem::class.java
                ).build()
        recyclerView = view.findViewById<RecyclerView>(R.id.rating_recyclerview)
        mAdapter = object : FirebaseRecyclerAdapter<ReviewItem, RatingViewHolder>(options) {
            public override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RatingViewHolder {
                val view: View = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.rating_item, viewGroup, false)
                return RatingViewHolder(view)
            }

            override fun onBindViewHolder(
                holder: RatingViewHolder,
                position: Int,
                model: ReviewItem
            ) {
                holder.setData(model)
            }
        }
        layoutManager = LinearLayoutManager(getContext())
        recyclerView.setAdapter(mAdapter)
        recyclerView.setLayoutManager(layoutManager)
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

    companion object {
        private var key: String? = null
    }
}