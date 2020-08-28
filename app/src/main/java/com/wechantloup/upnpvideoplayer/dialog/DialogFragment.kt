package com.wechantloup.upnpvideoplayer.dialog

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import com.wechantloup.upnpvideoplayer.browse2.SuperBrowseActivity

class DialogFragment : GuidedStepSupportFragment() {

    private lateinit var params: Params

    fun bind(params: Params) {
        this.params = params
    }

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
        for (option in params.options) {
            val action = GuidedAction.Builder(context)
                .id(params.options.indexOf(option).toLong())
                .title(getString(option.text)).build()
            actions.add(action)
        }
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        (activity as? SuperBrowseActivity)?.let {
            val option = params.options[action.id.toInt()]
            option.action()
            it.removeDialog()
            return
        }
    }

    class Option(
        @StringRes val text: Int,
        val action: () -> Unit
    )

    class Params(
        @StringRes val title: Int,
        @StringRes val message: Int,
        val options: List<Option>
    )
}