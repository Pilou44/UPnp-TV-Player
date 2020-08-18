package com.wechantloup.upnpvideoplayer.browse2

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.SearchOrbView
import androidx.leanback.widget.TitleViewAdapter
import com.wechantloup.upnpvideoplayer.R

class BrowseTitleView(context: Context) : FrameLayout(context), TitleViewAdapter.Provider {

    private var flags = 0
    private var mHasSearchListener = false
    private val mSearchOrbView: SearchOrbView
    private val mTextView: TextView
    private val mBadgeView: ImageView

    init {
        val inflater = LayoutInflater.from(context)
        val rootView: View = inflater.inflate(R.layout.browse_title_view, this)
        this.mBadgeView = rootView.findViewById<View>(R.id.title_badge) as ImageView
        this.mTextView = rootView.findViewById<View>(R.id.title_text) as TextView
        this.mSearchOrbView = rootView.findViewById<View>(R.id.title_orb) as SearchOrbView
        this.setClipToPadding(false)
        this.setClipChildren(false)
    }

    val mTitleViewAdapter = object : TitleViewAdapter() {
        override fun getSearchAffordanceView(): View {
            return this@BrowseTitleView.getSearchAffordanceView()
        }

        override fun setOnSearchClickedListener(listener: OnClickListener) {
            this@BrowseTitleView.setOnSearchClickedListener(listener)
        }

        override fun setAnimationEnabled(enable: Boolean) {
            this@BrowseTitleView.enableAnimation(enable)
        }

        override fun getBadgeDrawable(): Drawable {
            return this@BrowseTitleView.getBadgeDrawable()
        }

        override fun getSearchAffordanceColors(): SearchOrbView.Colors {
            return this@BrowseTitleView.getSearchAffordanceColors()
        }

        override fun getTitle(): CharSequence {
            return this@BrowseTitleView.getTitle()
        }

        override fun setBadgeDrawable(drawable: Drawable?) {
            this@BrowseTitleView.setBadgeDrawable(drawable)
        }

        override fun setSearchAffordanceColors(colors: SearchOrbView.Colors) {
            this@BrowseTitleView.setSearchAffordanceColors(colors)
        }

        override fun setTitle(titleText: CharSequence?) {
            this@BrowseTitleView.setTitle(titleText)
        }

        override fun updateComponentsVisibility(flags: Int) {
            this@BrowseTitleView.updateComponentsVisibility(flags)
        }
    }

    fun setTitle(titleText: CharSequence?) {
        this.mTextView.setText(titleText)
        updateBadgeVisibility()
    }

    fun getTitle(): CharSequence {
        return this.mTextView.getText()
    }

    fun setBadgeDrawable(drawable: Drawable?) {
        this.mBadgeView.setImageDrawable(drawable)
        updateBadgeVisibility()
    }

    fun getBadgeDrawable(): Drawable {
        return this.mBadgeView.getDrawable()
    }

    fun setOnSearchClickedListener(listener: OnClickListener?) {
        this.mHasSearchListener = listener != null
        this.mSearchOrbView.setOnOrbClickedListener(listener)
        updateSearchOrbViewVisiblity()
    }

    fun getSearchAffordanceView(): View {
        return this.mSearchOrbView
    }

    fun setSearchAffordanceColors(colors: SearchOrbView.Colors?) {
        this.mSearchOrbView.setOrbColors(colors)
    }

    fun getSearchAffordanceColors(): SearchOrbView.Colors {
        return this.mSearchOrbView.getOrbColors()
    }

    fun enableAnimation(enable: Boolean) {
        this.mSearchOrbView.enableOrbColorAnimation(enable && this.mSearchOrbView.hasFocus())
    }

    fun updateComponentsVisibility(flags: Int) {
        this.flags = flags
        if (flags and 2 == 2) {
            updateBadgeVisibility()
        } else {
            this.mBadgeView.setVisibility(GONE)
            this.mTextView.setVisibility(GONE)
        }
        updateSearchOrbViewVisiblity()
    }

    private fun updateSearchOrbViewVisiblity() {
        val visibility = if (this.mHasSearchListener && this.flags and 4 == 4) View.VISIBLE else View.INVISIBLE
        this.mSearchOrbView.setVisibility(visibility)
    }

    private fun updateBadgeVisibility() {
        val drawable: Drawable? = this.mBadgeView.getDrawable()
        if (drawable != null) {
            this.mBadgeView.setVisibility(View.VISIBLE)
            this.mTextView.setVisibility(GONE)
        } else {
            this.mBadgeView.setVisibility(GONE)
            this.mTextView.setVisibility(VISIBLE)
        }
    }

    override fun getTitleViewAdapter(): TitleViewAdapter = mTitleViewAdapter

}
