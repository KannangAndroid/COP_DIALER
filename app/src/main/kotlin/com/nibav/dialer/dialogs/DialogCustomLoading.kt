package com.nibav.dialer.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import com.nibav.dialer.databinding.CustomLoadingBinding

class DialogCustomLoading(context: Context, private val description: String) : Dialog(context) {

    private lateinit var dialogBinding: CustomLoadingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogBinding = CustomLoadingBinding.inflate(layoutInflater)
        setContentView(dialogBinding.root)
        dialogBinding.tvDescription.text = description
        setCancelable(false)
    }

}
