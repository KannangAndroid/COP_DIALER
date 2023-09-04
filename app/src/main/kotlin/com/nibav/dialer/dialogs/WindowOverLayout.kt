package com.nibav.dialer.dialogs

import android.app.ActionBar
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.nibav.dialer.R
import com.nibav.dialer.interfaces.OverlayCallListener

class WindowOverLayout(private val mContext: Context, callListener: OverlayCallListener) : View(
    mContext
) {
    private val mPopupLayout: ViewGroup
    private val mParentView: ViewGroup
    private val status: TextView
    private val overlayCallListener = callListener

    init {
        val params = WindowManager.LayoutParams(
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        val mWinManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mPopupLayout = inflater.inflate(R.layout.layout_call_overlay, null) as RelativeLayout
        status = mPopupLayout.findViewById(R.id.tvStatus)
        mPopupLayout.setVisibility(GONE)
        params.width = ActionBar.LayoutParams.WRAP_CONTENT
        params.height = ActionBar.LayoutParams.WRAP_CONTENT
        params.gravity = Gravity.TOP or Gravity.RIGHT
        // Default variant
        // params.windowAnimations = android.R.style.Animation_Translucent;
        mParentView = FrameLayout(mContext)
        mWinManager.addView(mParentView, params)
        mParentView.addView(mPopupLayout)
        mPopupLayout.setVisibility(GONE)
        mPopupLayout.setOnClickListener { overlayCallListener.onClickedCall() }
    }

    /**
     * Shows view
     */
    fun show() {
        val anim = AnimationUtils.loadAnimation(mContext, android.R.anim.fade_in)
        anim.duration = 2000
        mPopupLayout.visibility = VISIBLE
        mPopupLayout.startAnimation(anim)
    }

    /**
     * Hides view
     */
    fun hide() {
        mPopupLayout.visibility = GONE
    }

    /**
     * Update status
     */
    fun updateStatus(statusMsg: String) {
        status?.text = statusMsg
    }

    /**
     * IS VISIBLE view
     */
    fun isShowing(): Boolean {
        return mPopupLayout.isVisible
    }
}
