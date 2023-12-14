package com.mad.appetit.OrderActivitiesimport

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.mad.appetit.OrderActivities.Order
import com.mad.appetit.OrderActivities.Reservation
import com.mad.appetit.R


private class PagerAdapter(fm: FragmentManager?, private val mNumOfTabs: Int) :
    FragmentPagerAdapter(
        fm!!
    ) {
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> Reservation()
            1 -> Order()
            else -> Reservation()
        }
    }

    override fun getCount(): Int {
        return mNumOfTabs
    }
}

 class PagerAdapterOrder : Fragment() {
    private var mListener: PagerAdapterOrder.OnFragmentInteractionListener? = null
    private var pagerAdapter: PagerAdapter? = null
    private lateinit var tab: TabLayout
    private lateinit var viewPager: ViewPager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =
            inflater.inflate(R.layout.fragment_pager_adapter_order_reservation, container, false)
        tab = view.findViewById(R.id.tablayout)
        viewPager = view.findViewById(R.id.view_pager_id)
        tab.addTab(tab.newTab().setText("Pending orders"))
        tab.addTab(tab.newTab().setText("Accepted orders"))
        pagerAdapter =
            PagerAdapter(childFragmentManager, tab.getTabCount())
        viewPager.setAdapter(pagerAdapter)
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tab))
        tab.setOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.setCurrentItem(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
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
        mListener = if (context is PagerAdapterOrder.OnFragmentInteractionListener) {
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