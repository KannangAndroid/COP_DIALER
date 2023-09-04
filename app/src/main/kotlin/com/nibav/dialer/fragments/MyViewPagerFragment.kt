package com.nibav.dialer.fragments

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.nibav.commons.adapters.MyRecyclerViewAdapter
import com.nibav.commons.extensions.getProperPrimaryColor
import com.nibav.commons.extensions.getProperTextColor
import com.nibav.commons.extensions.getTextSize
import com.nibav.commons.helpers.SORT_BY_FIRST_NAME
import com.nibav.commons.helpers.SORT_BY_SURNAME
import com.nibav.commons.views.MyRecyclerView
import com.nibav.dialer.activities.MainActivity
import com.nibav.dialer.activities.SimpleActivity
import com.nibav.dialer.adapters.ContactsAdapter
import com.nibav.dialer.adapters.RecentCallsAdapter
import com.nibav.dialer.databinding.FragmentLettersLayoutBinding
import com.nibav.dialer.databinding.FragmentRecentsBinding
import com.nibav.dialer.extensions.config
import com.nibav.dialer.helpers.Config

abstract class MyViewPagerFragment<BINDING : MyViewPagerFragment.InnerBinding>(context: Context, attributeSet: AttributeSet) :
    RelativeLayout(context, attributeSet) {
    protected var activity: SimpleActivity? = null
    protected lateinit var innerBinding: BINDING
    private lateinit var config: Config

    fun setupFragment(activity: SimpleActivity) {
        config = activity.config
        if (this.activity == null) {
            this.activity = activity

            setupFragment()
            setupColors(activity.getProperTextColor(), activity.getProperPrimaryColor(), activity.getProperPrimaryColor())
        }
    }

    fun startNameWithSurnameChanged(startNameWithSurname: Boolean) {
        if (this !is RecentsFragment) {
            (innerBinding.fragmentList?.adapter as? ContactsAdapter)?.apply {
                config.sorting = if (startNameWithSurname) SORT_BY_SURNAME else SORT_BY_FIRST_NAME
                (this@MyViewPagerFragment.activity!! as MainActivity).refreshFragments()
            }
        }
    }

    fun finishActMode() {
        (innerBinding.fragmentList?.adapter as? MyRecyclerViewAdapter)?.finishActMode()
        (innerBinding.recentsList?.adapter as? MyRecyclerViewAdapter)?.finishActMode()
    }

    fun fontSizeChanged() {
        if (this is RecentsFragment) {
            (innerBinding.recentsList?.adapter as? RecentCallsAdapter)?.apply {
                fontSize = activity.getTextSize()
                notifyDataSetChanged()
            }
        } else {
            (innerBinding.fragmentList?.adapter as? ContactsAdapter)?.apply {
                fontSize = activity.getTextSize()
                notifyDataSetChanged()
            }
        }
    }

    abstract fun setupFragment()

    abstract fun setupColors(textColor: Int, primaryColor: Int, properPrimaryColor: Int)

    abstract fun onSearchClosed()

    abstract fun onSearchQueryChanged(text: String)

    interface InnerBinding {
        val fragmentList: MyRecyclerView?
        val recentsList: MyRecyclerView?
    }

    class LettersInnerBinding(val binding: FragmentLettersLayoutBinding) : InnerBinding {
        override val fragmentList: MyRecyclerView = binding.fragmentList
        override val recentsList = null
    }

    class RecentsInnerBinding(val binding: FragmentRecentsBinding) : InnerBinding {
        override val fragmentList = null
        override val recentsList = binding.recentsList
    }
}
