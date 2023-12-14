package com.mad.customer.UI

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mad.customer.R
import com.mad.customer.UIimport.EditProfile
import com.mad.customer.UIimport.MainActivity
import com.mad.mylibrary.SharedClass
import com.mad.mylibrary.User
import java.util.Objects

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [Profile.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [Profile.newInstance] factory method to
 * create an instance of this fragment.
 */
class Profile : Fragment() {
    private var mListener: OnFragmentInteractionListener? = null
    private lateinit var view: View
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_profile, container, false)
        view.findViewById<View>(R.id.loadingProfile).visibility = View.GONE
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference(SharedClass.CUSTOMER_PATH).child(SharedClass.ROOT_UID)
        setHasOptionsMenu(true)
        Log.d("PATH", SharedClass.CUSTOMER_PATH + "/" + SharedClass.ROOT_UID)
        view.findViewById<View>(R.id.button_logout).setOnClickListener { e: View? ->
            auth.signOut()
            val mainActivity = Intent(context, MainActivity::class.java)
            mainActivity.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(mainActivity)
        }
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d("ROOT_UID", SharedClass.ROOT_UID)
                    SharedClass.user = dataSnapshot.child("customer_info").getValue(
                        User::class.java
                    )
                    (view.findViewById<View>(R.id.name) as TextView).text = SharedClass.user!!.name
                    (view.findViewById<View>(R.id.surname) as TextView).text =
                        SharedClass.user!!.surname
                    (view.findViewById<View>(R.id.mail) as TextView).text = SharedClass.user!!.email
                    (view.findViewById<View>(R.id.phone) as TextView).text =
                        SharedClass.user!!.phone
                    (view.findViewById<View>(R.id.address) as TextView).text =
                        (SharedClass.user?.addr
                            ?: if (SharedClass.user!!.photoPath != null) Glide.with(
                                Objects.requireNonNull(
                                    view.getContext()
                                )
                            )
                                .load(SharedClass.user!!.photoPath)
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                .into((view.findViewById<View>(R.id.profile_image) as ImageView)) else Glide.with(
                                Objects.requireNonNull(view.getContext())
                            )
                                .load(R.drawable.person)
                                .into((view.findViewById<View>(R.id.profile_image) as ImageView))).toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("MAIN", "Failed to read value.", error.toException())
            }
        })
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return when (id) {
            R.id.add -> {
                val i = Intent(context, EditProfile::class.java)
                startActivity(i)
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
        mListener = if (context is OnFragmentInteractionListener) {
            context
        } else {
            throw RuntimeException(
                context.toString()
                        + " must implement OnFragmentInteractionListener"
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val myRef = FirebaseDatabase.getInstance().getReference(SharedClass.CUSTOMER_PATH)
            .child(SharedClass.ROOT_UID)
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d("ROOT_UID", SharedClass.ROOT_UID)
                if (dataSnapshot.exists()) {
                    SharedClass.user = dataSnapshot.child("customer_info").getValue(
                        User::class.java
                    )
                    (view.findViewById<View>(R.id.name) as TextView).text = SharedClass.user!!.name
                    (view.findViewById<View>(R.id.surname) as TextView).text =
                        SharedClass.user!!.surname
                    (view!!.findViewById<View>(R.id.mail) as TextView).text =
                        SharedClass.user!!.email
                    (view!!.findViewById<View>(R.id.phone) as TextView).text =
                        SharedClass.user!!.phone
                    (view!!.findViewById<View>(R.id.address) as TextView).text =
                        SharedClass.user!!.addr
                    if (SharedClass.user!!.photoPath != null) Glide.with(
                        Objects.requireNonNull(
                            view!!.context
                        )
                    )
                        .load(SharedClass.user!!.photoPath)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into((view!!.findViewById<View>(R.id.profile_image) as ImageView)) else Glide.with(
                        Objects.requireNonNull(
                            view!!.context
                        )
                    )
                        .load(R.drawable.person)
                        .into((view!!.findViewById<View>(R.id.profile_image) as ImageView))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("MAIN", "Failed to read value.", error.toException())
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
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
        // TODO: Rename and change types and number of parameters
        fun newInstance(): Profile {
            val fragment = Profile()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}