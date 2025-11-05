package com.rst.recipeappopsc6312

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ScanStateAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3 // We have three tabs

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ScanCameraFragment()
            1 -> ScanManualFragment()
            2 -> ScanHistoryFragment()
            else -> throw IllegalStateException("Invalid position")
        }
    }
}

