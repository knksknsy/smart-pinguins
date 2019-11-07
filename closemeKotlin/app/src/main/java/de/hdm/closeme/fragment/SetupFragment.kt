package de.hdm.closeme.fragment


import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.hdm.closeme.MainActivity

import de.hdm.closeme.R
import de.hdm.closeme.adapter.SetupPagerAdapter
import de.hdm.closeme.constant.Constants
import de.hdm.closeme.service.BluetoothService
import kotlinx.android.synthetic.main.fragment_setup.*


class SetupFragment : Fragment() {

    var pagerAdapter: SetupPagerAdapter? = null
    private var bottomNavigation: BottomNavigationView? = null


    companion object {
        @JvmStatic
        fun newInstance() = SetupFragment().apply {
            arguments = Bundle().apply {}
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setup, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setViewPager()
        ((activity) as MainActivity).registerSetupViewPagerNavigationListener(
                { offset -> setupViewPager?.setCurrentItem(setupViewPager.currentItem + offset, true) },
                { bottomNavigation -> this.bottomNavigation = bottomNavigation })
    }

    private fun setViewPager() {
        pagerAdapter = SetupPagerAdapter(((activity) as MainActivity).supportFragmentManager, getFragmentList())
        setupViewPager.setOffscreenPageLimit(4);
        setupViewPager.adapter = pagerAdapter
        setupViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                if (position + 1 == pagerAdapter?.count) {
                    bottomNavigation?.menu?.getItem(Constants.NAVIGATION_ITEM_FORWARD)?.setTitle("...")
                    bottomNavigation?.menu?.getItem(Constants.NAVIGATION_ITEM_FORWARD)?.setIcon(null)
                } else {
                    bottomNavigation?.menu?.getItem(Constants.NAVIGATION_ITEM_FORWARD)?.setTitle(getString(R.string.forward))
                    bottomNavigation?.menu?.getItem(Constants.NAVIGATION_ITEM_FORWARD)?.setIcon(R.drawable.arrow_right_vektor)
                }
                if (position == 0) {
                    bottomNavigation?.menu?.getItem(Constants.NAVIGATION_ITEM_BACK)?.setTitle("...")
                    bottomNavigation?.menu?.getItem(Constants.NAVIGATION_ITEM_BACK)?.setIcon(null)
                } else {
                    bottomNavigation?.menu?.getItem(Constants.NAVIGATION_ITEM_BACK)?.setTitle(getString(R.string.back))
                    bottomNavigation?.menu?.getItem(Constants.NAVIGATION_ITEM_BACK)?.setIcon(R.drawable.arrow_left_vektor)
                }
            }

        })
        dotsIndicator.setViewPager(setupViewPager)

    }

    private fun getFragmentList(): ArrayList<Fragment> {
        val setupFragmentList = ArrayList<Fragment>()
        setupFragmentList.add(SetupInformationFragment.newInstance(Constants.ARGUMENT_SETUP_FIRST_PAGE))
        setupFragmentList.add(ScannerFragment.newInstance( true))
        setupFragmentList.add(MapsFragment.newInstance(true,null))
        setupFragmentList.add(SetupInformationFragment.newInstance(Constants.ARGUMENT_SETUP_SECOND_PAGE))
        return setupFragmentList
    }
}

