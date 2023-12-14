package com.mad.riders.NavigationFragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mad.mylibrary.SharedClass.RIDERS_PATH
import com.mad.mylibrary.SharedClass.ROOT_UID
import com.mad.mylibrary.User
import com.mad.riders.MainActivity
import com.mad.riders.ProfileManagment.EditPassword
import com.mad.riders.ProfileManagment.EditProfile

import java.util.Objects
import com.mad.riders.R

class Profile constructor() : Fragment() {
    private var mListener: OnFragmentInteractionListener? = null
   // lateinit var view: View
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    public override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
       val view: View = inflater.inflate(R.layout.fragment_profile, container, false)
        val policy: ThreadPolicy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        val auth: FirebaseAuth = FirebaseAuth.getInstance()
        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        val myRef: DatabaseReference = database.getReference(RIDERS_PATH).child(ROOT_UID)
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            public override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                if (dataSnapshot.exists()) {
                    val user: User?
                    user = dataSnapshot.child("rider_info").getValue(User::class.java)
                    if (user != null) {
                        (view.findViewById<View>(R.id.name) as TextView).setText(user.name)
                        (view.findViewById<View>(R.id.surname) as TextView).setText(user.surname)
                        (view.findViewById<View>(R.id.mail) as TextView).setText(user.email)
                        (view.findViewById<View>(R.id.phone) as TextView).setText(user.phone)
                        if (user.photoPath != null) Glide.with(Objects.requireNonNull(view.getContext()))
                            .load(user.photoPath)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .into((view.findViewById<View>(R.id.profile_image) as ImageView?)!!) else Glide.with(
                            Objects.requireNonNull(view.getContext())
                        )
                            .load(R.drawable.rider)
                            .into((view.findViewById<View>(R.id.profile_image) as ImageView?)!!)
                    }
                }
            }

            public override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w("MAIN", "Failed to read value.", error.toException())
            }
        })
        return view
    }

    public override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    public override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.getItemId()
        when (id) {
            R.id.add -> {
                val i: Intent = Intent(getContext(), EditProfile::class.java)
                startActivity(i)
                return true
            }

            R.id.edit_password -> {
                val editPsw: Intent = Intent(getContext(), EditPassword::class.java)
                startActivity(editPsw)
                return true
            }

            R.id.logout -> {
                FirebaseAuth.getInstance().signOut()
                val mainActivity: Intent = Intent(getContext(), MainActivity::class.java)
                mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(mainActivity)
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    public override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(
                (context.toString()
                        + " must implement OnFragmentInteractionListener")
            )
        }
    }

    public override fun onResume() {
        super.onResume()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    public override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    open interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        //fun onFragmentInteraction(uri: Uri?)
    }

    companion object {
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String?, param2: String?): Profile {
            val fragment: Profile = Profile()
            return fragment
        }
    }
}