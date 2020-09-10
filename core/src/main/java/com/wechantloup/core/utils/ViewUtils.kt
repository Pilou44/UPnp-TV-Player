package com.wechantloup.core.utils

import android.app.Activity
import android.transition.AutoTransition
import android.transition.Transition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

object ViewUtils {

    fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View =
        LayoutInflater
            .from(context)
            .inflate(layoutRes, this, attachToRoot)

    fun View.startAnimatingConstraints(transition: Transition? = null, onCompleted: (() -> Unit)? = null) {
        (context as? Activity)?.findViewById<ViewGroup>(android.R.id.content)?.also { rootView ->
            TransitionManager.beginDelayedTransition(
                rootView,
                (transition ?: AutoTransition())
                    .apply { duration = 150 }
                    .apply { addListener(object : Transition.TransitionListener {
                        override fun onTransitionEnd(transition: Transition?) { onCompleted?.invoke() }
                        override fun onTransitionResume(transition: Transition?) {}
                        override fun onTransitionPause(transition: Transition?) {}
                        override fun onTransitionCancel(transition: Transition?) {}
                        override fun onTransitionStart(transition: Transition?) {}
                    }) }
            )
        }
    }

}