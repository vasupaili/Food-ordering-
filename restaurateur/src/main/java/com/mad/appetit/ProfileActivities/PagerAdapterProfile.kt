package com.mad.appetit.ProfileActivitiesimport

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.mad.appetit.R
import com.mad.appetit.Startup.MainActivity
import com.mad.mylibrary.Restaurateur
import com.mad.mylibrary.ReviewItem
import com.mad.mylibrary.SharedClass.Address
import com.mad.mylibrary.SharedClass.Description
import com.mad.mylibrary.SharedClass.Mail
import com.mad.mylibrary.SharedClass.Name
import com.mad.mylibrary.SharedClass.Phone
import com.mad.mylibrary.SharedClass.Photo
import com.mad.mylibrary.SharedClass.RESTAURATEUR_INFO
import com.mad.mylibrary.SharedClass.ROOT_UID
import com.mad.mylibrary.SharedClass.Time


import java.util.Objects


private class PagerAdapter(fm: FragmentManager?, private val mNumOfTabs: Int) :
    FragmentPagerAdapter(
        fm!!
    ) {
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> Profile()
            1 -> Rating()
            else -> Profile()
        }
    }

    override fun getCount(): Int {
        return mNumOfTabs
    }
}

class PagerAdapterProfile : Fragment() {
    private var name: String? = null
    private var addr: String? = null
    private var descr: String? = null
    private var mail: String? = null
    private var phone: String? = null
    private var photoUri: String? = null
    private var time: String? = null
    private var mListener: PagerAdapterProfile.OnFragmentInteractionListener? = null
    private var pagerAdapter: PagerAdapter? = null
    private lateinit var tab: TabLayout
    private lateinit var viewPager: ViewPager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pager_adapter_profile, container, false)
        val getRestaurantInfo: Query =
            FirebaseDatabase.getInstance().reference.child(RESTAURATEUR_INFO + "/" + ROOT_UID)
                .child("info")
        getRestaurantInfo.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val restaurateur = dataSnapshot.getValue(
                        Restaurateur::class.java
                    )
                    name = restaurateur!!.name
                    addr = restaurateur.addr
                    descr = restaurateur.cuisine
                    mail = restaurateur.mail
                    phone = restaurateur.phone
                    photoUri = restaurateur.photoUri
                    time = restaurateur.openingTime
                    (view.findViewById<View>(R.id.name) as TextView).text = name
                    if (photoUri != null) {
                        Glide.with(Objects.requireNonNull(view.context))
                            .load(photoUri)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .into((view.findViewById<View>(R.id.profile_image) as ImageView))
                    } else {
                        Glide.with(Objects.requireNonNull(view.context))
                            .load(R.drawable.restaurant_white)
                            .into((view.findViewById<View>(R.id.profile_image) as ImageView))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("PAGER ADAPTER PROFILE", "Failed to read value.", error.toException())
            }
        })
        val getGlobalRating: Query =
            FirebaseDatabase.getInstance().reference.child(RESTAURATEUR_INFO + "/" + ROOT_UID)
                .child("review")
        getGlobalRating.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    var totRating = 0f
                    val nReviews = dataSnapshot.childrenCount
                    for (d in dataSnapshot.children) {
                        val reviewItem = d.getValue(ReviewItem::class.java)
                        totRating += reviewItem!!.stars.toFloat()
                    }
                    totRating = totRating / nReviews
                    (view.findViewById<View>(R.id.ratingbaritem) as RatingBar).rating = totRating
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("PAGER ADAPTER PROFILE", "Failed to read value.", databaseError.toException())
            }
        })
        tab = view.findViewById(R.id.tablayout)
        viewPager = view.findViewById(R.id.view_pager_id)
        tab.addTab(tab.newTab().setText("Profile"))
        tab.addTab(tab.newTab().setText("Rating"))
        pagerAdapter =
            PagerAdapter(childFragmentManager, tab.getTabCount())
        viewPager.setAdapter(pagerAdapter)
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tab))
        tab.setOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.setCurrentItem(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_profile, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return when (id) {
            R.id.edit_profile -> {
                val editProfile = Intent(context, EditProfile::class.java)
                editProfile.putExtra(Name, name)
                editProfile.putExtra(Description, descr)
                editProfile.putExtra(Address, addr)
                editProfile.putExtra(Mail, mail)
                editProfile.putExtra(Phone, phone)
                editProfile.putExtra(Photo, photoUri)
                editProfile.putExtra(Time, time)
                startActivity(editProfile)
                true
            }

            R.id.edit_password -> {
                val editPsw = Intent(context, EditPassword::class.java)
                startActivity(editPsw)
                true
            }

            R.id.logout -> {
                FirebaseAuth.getInstance().signOut()
                ROOT_UID = ""
                val mainActivity = Intent(context, MainActivity::class.java)
                mainActivity.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(mainActivity)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri?) {
        if (mListener != null) {
            mListener!!.onFragmentInteraction(uri)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is PagerAdapterProfile.OnFragmentInteractionListener) {
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
}