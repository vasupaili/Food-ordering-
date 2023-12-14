package com.mad.appetit.ProfileActivitiesimport

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.mad.appetit.R
import com.mad.mylibrary.Restaurateur
import com.mad.mylibrary.SharedClass.RESTAURATEUR_INFO
import com.mad.mylibrary.SharedClass.ROOT_UID


class Profile : Fragment() {
    private var addr: String? = null
    private var descr: String? = null
    private var mail: String? = null
    private var phone: String? = null
    private var time: String? = null
    private var mListener: Profile.OnFragmentInteractionListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        val myRef = FirebaseDatabase.getInstance().reference
        val query: Query = myRef.child(RESTAURATEUR_INFO + "/" + ROOT_UID).child("info")
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val restaurateur = dataSnapshot.getValue(
                        Restaurateur::class.java
                    )
                    addr = restaurateur!!.addr
                    descr = restaurateur.cuisine
                    mail = restaurateur.mail
                    phone = restaurateur.phone
                    time = restaurateur.openingTime
                    (view.findViewById<View>(R.id.address) as TextView).text = addr
                    (view.findViewById<View>(R.id.description) as TextView).text = descr
                    (view.findViewById<View>(R.id.mail) as TextView).text = mail
                    (view.findViewById<View>(R.id.phone2) as TextView).text = phone
                    (view.findViewById<View>(R.id.time_text) as TextView).text = time
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("DAILY OFFER", "Failed to read value.", error.toException())
            }
        })
        return view
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri?) {
        if (mListener != null) {
            mListener!!.onFragmentInteraction(uri)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is Profile.OnFragmentInteractionListener) {
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