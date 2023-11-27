package com.dynatech2012.kamleonuserapp.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class TabItemAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    //override fun getItemCount(): Int = 3
    private val fragmentList: ArrayList<Fragment> = ArrayList()

    override fun createFragment(position: Int): Fragment {
        // Return a NEW fragment instance in createFragment(int)
        /*val fragment = ResumeFragment()
        Log.d(TAG, "page position: $position")
        fragment.arguments = Bundle().apply {
            // Our object is just an integer :-P
            putInt(ARG_PAGE, position)
        }
        return fragment*/
        return fragmentList[position]
    }

    fun addPrevious(fragment: Fragment) {
        fragmentList.add(0, fragment)
        //fragmentList.removeLast()
    }

    fun addNext(fragment: Fragment) {
        fragmentList.add(fragment)
        //fragmentList.removeFirst()
    }


    fun addNext() {
        val lastPos = fragmentList.size
        val fragment = createFragment(lastPos)
        fragmentList.add(fragment)
        //fragmentList.removeFirst()
    }

    fun addFragment(fragment: Fragment, index: Int) {
        fragmentList.add(index, fragment)
    }

    fun addFragment(fragment: Fragment) {
        fragmentList.add(fragment)
    }

    override fun getItemCount(): Int {
        return fragmentList.size
    }

    companion object {
        private val TAG = this::class.simpleName
    }

}
