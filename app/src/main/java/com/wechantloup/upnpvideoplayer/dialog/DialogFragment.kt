package com.wechantloup.upnpvideoplayer.dialog

import android.content.Intent
import android.os.Bundle
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import com.wechantloup.upnpvideoplayer.dialog.DialogActivity.Companion.ACTION_ID_NEGATIVE
import com.wechantloup.upnpvideoplayer.dialog.DialogActivity.Companion.ACTION_ID_POSITIVE
import com.wechantloup.upnpvideoplayer.utils.Serializer.deserialize

class DialogFragment : GuidedStepSupportFragment() {

    private lateinit var params: DialogActivity.Params

    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance {
        return Guidance(
            getString(params.title),
            getString(params.message),
            "", null
        )
    }

    override fun onCreateActions(
        actions: MutableList<GuidedAction?>,
        savedInstanceState: Bundle?
    ) {
        val serializedParams = requireNotNull(arguments?.getString(DialogActivity.EXTRA_PARAMS))
        params = serializedParams.deserialize()
        var action = GuidedAction.Builder(context)
            .id(ACTION_ID_POSITIVE.toLong())
            .title(getString(params.positiveButton)).build()
        actions.add(action)
        action = GuidedAction.Builder(context)
            .id(ACTION_ID_NEGATIVE.toLong())
            .title(getString(params.negativeButton)).build()
        actions.add(action)
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        val activity = requireActivity()
        val returnIntent = Intent()
        activity.setResult(action.id.toInt(), returnIntent)
        activity.finish()
    }
}