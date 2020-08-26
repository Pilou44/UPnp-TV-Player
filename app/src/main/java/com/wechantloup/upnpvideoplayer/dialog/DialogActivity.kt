package com.wechantloup.upnpvideoplayer.dialog

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment
import com.wechantloup.upnpvideoplayer.utils.Serializer.serialize

class DialogActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Color.parseColor("#21272A")))
        if (savedInstanceState == null) {
            val fragment: GuidedStepSupportFragment = DialogFragment()
            val params = intent.getStringExtra(EXTRA_PARAMS)
            val bundle = Bundle()
            bundle.putString(EXTRA_PARAMS, params)
            fragment.arguments = bundle
            GuidedStepSupportFragment.addAsRoot(this, fragment, android.R.id.content)
        }
    }

    class Params(
        val requestCode: Int,
        @StringRes val title: Int,
        @StringRes val message: Int,
        @StringRes val positiveButton: Int,
        @StringRes val negativeButton: Int
    )

    companion object {
        const val ACTION_ID_POSITIVE = 1
        const val ACTION_ID_NEGATIVE = ACTION_ID_POSITIVE + 1
        const val EXTRA_PARAMS = "params"

        fun launch(fragment: Fragment, params: Params) {
            val intent = Intent(fragment.context, DialogActivity::class.java)
            val serializedParams = params.serialize()
            intent.putExtra(EXTRA_PARAMS, serializedParams)
            fragment.startActivityForResult(intent, params.requestCode)
        }
    }
}