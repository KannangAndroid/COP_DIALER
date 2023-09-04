package com.nibav.dialer.dialogs

import androidx.appcompat.app.AlertDialog
import com.nibav.commons.activities.BaseSimpleActivity
import com.nibav.commons.extensions.getAlertDialogBuilder
import com.nibav.commons.extensions.setupDialogStuff
import com.nibav.commons.extensions.viewBinding
import com.nibav.dialer.activities.SimpleActivity
import com.nibav.dialer.adapters.RecentCallsAdapter
import com.nibav.dialer.databinding.DialogShowGroupedCallsBinding
import com.nibav.dialer.helpers.RecentsHelper
import com.nibav.dialer.models.RecentCall

class ShowGroupedCallsDialog(val activity: BaseSimpleActivity, callIds: ArrayList<Int>) {
    private var dialog: AlertDialog? = null
    private val binding by activity.viewBinding(DialogShowGroupedCallsBinding::inflate)

    init {
        RecentsHelper(activity).getRecentCalls(false) { allRecents ->
            val recents = allRecents.filter { callIds.contains(it.id) }.toMutableList() as ArrayList<RecentCall>
            activity.runOnUiThread {
                RecentCallsAdapter(activity as SimpleActivity, recents, binding.selectGroupedCallsList, null, false) {
                }.apply {
                    binding.selectGroupedCallsList.adapter = this
                }
            }
        }

        activity.getAlertDialogBuilder()
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }
}
