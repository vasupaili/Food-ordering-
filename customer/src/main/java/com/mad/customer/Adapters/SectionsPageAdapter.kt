package com.mad.customer.Adaptersimport

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter



class SectionsPageAdapter constructor(fm: FragmentManager?) : FragmentPagerAdapter(
    (fm)!!
) {
    private val mFragmentList: MutableList<Fragment> = ArrayList()
    private val mFragmentTitleList: MutableList<String> = ArrayList()
    fun addFragment(fragment: Fragment, title: String) {
        mFragmentList.add(fragment)
        mFragmentTitleList.add(title)
    }

    public override fun getPageTitle(position: Int): CharSequence? {
        return mFragmentTitleList.get(position)
    }

    public override fun getItem(position: Int): Fragment {
        return mFragmentList.get(position)
    }

    public override fun getCount(): Int {
        return mFragmentList.size
    }
}