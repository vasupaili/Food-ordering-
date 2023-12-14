package com.mad.appetit.ProfileActivitiesimport

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.FirebaseDatabase
import com.mad.appetit.R
import com.mad.mylibrary.ReviewItem

import com.mad.mylibrary.SharedClass.RESTAURATEUR_INFO
import com.mad.mylibrary.SharedClass.ROOT_UID


private class RatingViewHolder(val view: View) : RecyclerView.ViewHolder(
    view
) {
    private val name: TextView
    private val comment: TextView
    private val ratingBar: RatingBar
    private val img: ImageView

    init {
        name = itemView.findViewById(R.id.rating_item_name)
        comment = itemView.findViewById(R.id.rating_item_comment)
        ratingBar = itemView.findViewById(R.id.ratingbaritem)
        img = itemView.findViewById(R.id.rating_item_img)
    }

    fun setData(ri: ReviewItem) {
        name.text = ri.name
        ratingBar.rating = ri.stars.toFloat()
        if (ri.comment != null) comment.text = ri.comment else comment.visibility = View.GONE
        if (!ri.img!!.isEmpty() && ri.img != null && ri.img != "null") Glide.with(view.context)
            .load(ri.img).into(img)
    }
}

 class Rating : Fragment() {
    private var mListener: Rating.OnFragmentInteractionListener? = null
    private lateinit var recyclerView: RecyclerView
    private var mAdapter: FirebaseRecyclerAdapter<ReviewItem, RatingViewHolder>? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_rating, container, false)
        recyclerView = view.findViewById(R.id.rating_recyclerview)
        mAdapter = object :
            FirebaseRecyclerAdapter<ReviewItem, RatingViewHolder>(options) {
            override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RatingViewHolder {
                val view = LayoutInflater.from(viewGroup.context)
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
        layoutManager = LinearLayoutManager(context)
        recyclerView.setAdapter(mAdapter)
        recyclerView.setLayoutManager(layoutManager)
        return view
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
        mListener = if (context is Rating.OnFragmentInteractionListener) {
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
        private val options = FirebaseRecyclerOptions.Builder<ReviewItem>()
            .setQuery(
                FirebaseDatabase.getInstance().getReference(RESTAURATEUR_INFO).child(ROOT_UID)
                    .child("review"),
                ReviewItem::class.java
            ).build()
    }
}