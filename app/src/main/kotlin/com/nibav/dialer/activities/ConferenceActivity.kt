package com.nibav.dialer.activities

import android.os.Bundle
import com.nibav.commons.extensions.viewBinding
import com.nibav.commons.helpers.NavigationIcon
import com.nibav.dialer.adapters.ConferenceCallsAdapter
import com.nibav.dialer.databinding.ActivityConferenceBinding
import com.nibav.dialer.helpers.CallManager

class ConferenceActivity : SimpleActivity() {
    private val binding by viewBinding(ActivityConferenceBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.apply {
            updateMaterialActivityViews(conferenceCoordinator, conferenceList, useTransparentNavigation = true, useTopSearchMenu = false)
            setupMaterialScrollListener(conferenceList, conferenceToolbar)
            conferenceList.adapter = ConferenceCallsAdapter(this@ConferenceActivity, conferenceList, ArrayList(CallManager.getConferenceCalls())) {}
        }
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.conferenceToolbar, NavigationIcon.Arrow)
    }
}
