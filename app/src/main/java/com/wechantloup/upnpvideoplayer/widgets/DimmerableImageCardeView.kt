package com.wechantloup.upnpvideoplayer.widgets

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.leanback.widget.BaseCardView
import com.wechantloup.upnpvideoplayer.R

open class DimmerableImageCardeView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : BaseCardView(context, attrs, defStyleAttr) {


    val CARD_TYPE_FLAG_IMAGE_ONLY = 0
    val CARD_TYPE_FLAG_TITLE = 1
    val CARD_TYPE_FLAG_CONTENT = 2
    val CARD_TYPE_FLAG_ICON_RIGHT = 4
    val CARD_TYPE_FLAG_ICON_LEFT = 8

    private val ALPHA = "alpha"

    private lateinit var mImageView: ImageView
    private lateinit var mInfoArea: ViewGroup
    private var mTitleView: TextView? = null
    private var mContentView: TextView? = null
    private lateinit var mBadgeImage: ImageView
    private var mAttachedToWindow = false
    lateinit var mFadeInAnimator: ObjectAnimator

    /**
     * @see View.View
     */
    init {
        buildImageCardView(attrs, defStyleAttr, androidx.leanback.R.style.Widget_Leanback_ImageCardView)
    }

    private fun buildImageCardView(
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyle: Int
    ) {
        // Make sure the ImageCardView is focusable.
        isFocusable = true
        isFocusableInTouchMode = true
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.dimmerable_image_card_view, this)
        val cardAttrs = context.obtainStyledAttributes(
            attrs,
            androidx.leanback.R.styleable.lbImageCardView, defStyleAttr, defStyle
        )
        val cardType = cardAttrs
            .getInt(androidx.leanback.R.styleable.lbImageCardView_lbImageCardViewType, CARD_TYPE_FLAG_IMAGE_ONLY)
        val hasImageOnly = cardType == CARD_TYPE_FLAG_IMAGE_ONLY
        val hasTitle = cardType and CARD_TYPE_FLAG_TITLE == CARD_TYPE_FLAG_TITLE
        val hasContent = cardType and CARD_TYPE_FLAG_CONTENT == CARD_TYPE_FLAG_CONTENT
        val hasIconRight = cardType and CARD_TYPE_FLAG_ICON_RIGHT == CARD_TYPE_FLAG_ICON_RIGHT
        val hasIconLeft =
            !hasIconRight && cardType and CARD_TYPE_FLAG_ICON_LEFT == CARD_TYPE_FLAG_ICON_LEFT
        mImageView = findViewById(R.id.main_image)
        if (mImageView.getDrawable() == null) {
            mImageView.setVisibility(View.INVISIBLE)
        }
        // Set Object Animator for image view.
        mFadeInAnimator = ObjectAnimator.ofFloat(mImageView, ALPHA, 1f)
        mFadeInAnimator.setDuration(
            mImageView.getResources().getInteger(android.R.integer.config_shortAnimTime).toLong()
        )
        mInfoArea = findViewById(R.id.info_field)
        if (hasImageOnly) {
            removeView(mInfoArea)
            cardAttrs.recycle()
            return
        }
        // Create children
        if (hasTitle) {
            mTitleView = inflater.inflate(
                androidx.leanback.R.layout.lb_image_card_view_themed_title,
                mInfoArea, false
            ) as TextView?
            mInfoArea.addView(mTitleView)
        }
        if (hasContent) {
            mContentView = inflater.inflate(
                androidx.leanback.R.layout.lb_image_card_view_themed_content,
                mInfoArea, false
            ) as TextView?
            mInfoArea.addView(mContentView)
        }
        if (hasIconRight || hasIconLeft) {
            var layoutId: Int = androidx.leanback.R.layout.lb_image_card_view_themed_badge_right
            if (hasIconLeft) {
                layoutId = androidx.leanback.R.layout.lb_image_card_view_themed_badge_left
            }
            mBadgeImage = inflater.inflate(layoutId, mInfoArea, false) as ImageView
            mInfoArea.addView(mBadgeImage)
        }

        // Set up LayoutParams for children
        if (hasTitle && !hasContent && mBadgeImage != null) {
            val relativeLayoutParams =
                mTitleView!!.layoutParams as RelativeLayout.LayoutParams
            // Adjust title TextView if there is an icon but no content
            if (hasIconLeft) {
                relativeLayoutParams.addRule(RelativeLayout.END_OF, mBadgeImage!!.id)
            } else {
                relativeLayoutParams.addRule(RelativeLayout.START_OF, mBadgeImage!!.id)
            }
            mTitleView!!.layoutParams = relativeLayoutParams
        }

        // Set up LayoutParams for children
        if (hasContent) {
            val relativeLayoutParams =
                mContentView!!.layoutParams as RelativeLayout.LayoutParams
            if (!hasTitle) {
                relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            }
            // Adjust content TextView if icon is on the left
            if (hasIconLeft) {
                relativeLayoutParams.removeRule(RelativeLayout.START_OF)
                relativeLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_START)
                relativeLayoutParams.addRule(RelativeLayout.END_OF, mBadgeImage!!.id)
            }
            mContentView!!.layoutParams = relativeLayoutParams
        }
        if (mBadgeImage != null) {
            val relativeLayoutParams =
                mBadgeImage!!.layoutParams as RelativeLayout.LayoutParams
            if (hasContent) {
                relativeLayoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, mContentView!!.id)
            } else if (hasTitle) {
                relativeLayoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, mTitleView!!.id)
            }
            mBadgeImage!!.layoutParams = relativeLayoutParams
        }

        // Backward compatibility: Newly created ImageCardViews should change
        // the InfoArea's background color in XML using the corresponding style.
        // However, since older implementations might make use of the
        // 'infoAreaBackground' attribute, we have to make sure to support it.
        // If the user has set a specific value here, it will differ from null.
        // In this case, we do want to override the value set in the style.
        val background = cardAttrs.getDrawable(androidx.leanback.R.styleable.lbImageCardView_infoAreaBackground)
        background?.let { setInfoAreaBackground(it) }
        // Backward compatibility: There has to be an icon in the default
        // version. If there is one, we have to set its visibility to 'GONE'.
        // Disabling 'adjustIconVisibility' allows the user to set the icon's
        // visibility state in XML rather than code.
        if (mBadgeImage != null && mBadgeImage!!.drawable == null) {
            mBadgeImage!!.visibility = View.GONE
        }
        cardAttrs.recycle()
    }

    /**
     * @see View.View
     */
    constructor(context: Context) : this(context, null)

    /**
     * @see View.View
     */
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, androidx.leanback.R.attr.imageCardViewStyle)

    /**
     * Returns the main image view.
     */
    fun getMainImageView(): ImageView {
        return mImageView
    }

    /**
     * Enables or disables adjustment of view bounds on the main image.
     */
    fun setMainImageAdjustViewBounds(adjustViewBounds: Boolean) {
        mImageView.adjustViewBounds = adjustViewBounds
    }

    /**
     * Sets the ScaleType of the main image.
     */
    fun setMainImageScaleType(scaleType: ImageView.ScaleType?) {
        mImageView.scaleType = scaleType
    }

    /**
     * Sets the image drawable with fade-in animation.
     */
    fun setMainImage(drawable: Drawable?) {
        setMainImage(drawable, true)
    }

    /**
     * Sets the image drawable with optional fade-in animation.
     */
    fun setMainImage(drawable: Drawable?, fade: Boolean) {
        if (mImageView == null) {
            return
        }
        mImageView!!.setImageDrawable(drawable)
        if (drawable == null) {
            mFadeInAnimator!!.cancel()
            mImageView!!.alpha = 1f
            mImageView!!.visibility = View.INVISIBLE
        } else {
            mImageView!!.visibility = View.VISIBLE
            if (fade) {
                fadeIn()
            } else {
                mFadeInAnimator!!.cancel()
                mImageView!!.alpha = 1f
            }
        }
    }

    /**
     * Sets the layout dimensions of the ImageView.
     */
    fun setMainImageDimensions(width: Int, height: Int) {
        val lp = mImageView!!.layoutParams
        lp.width = width
        lp.height = height
        mImageView!!.layoutParams = lp
    }

    /**
     * Returns the ImageView drawable.
     */
    fun getMainImage(): Drawable? {
        return if (mImageView == null) {
            null
        } else mImageView!!.drawable
    }

    /**
     * Returns the info area background drawable.
     */
    fun getInfoAreaBackground(): Drawable? {
        return if (mInfoArea != null) {
            mInfoArea!!.background
        } else null
    }

    /**
     * Sets the info area background drawable.
     */
    fun setInfoAreaBackground(drawable: Drawable?) {
        if (mInfoArea != null) {
            mInfoArea!!.background = drawable
        }
    }

    /**
     * Sets the info area background color.
     */
    fun setInfoAreaBackgroundColor(@ColorInt color: Int) {
        if (mInfoArea != null) {
            mInfoArea!!.setBackgroundColor(color)
        }
    }

    /**
     * Sets the title text.
     */
    fun setTitleText(text: CharSequence?) {
        if (mTitleView == null) {
            return
        }
        mTitleView!!.text = text
    }

    /**
     * Returns the title text.
     */
    fun getTitleText(): CharSequence? {
        return if (mTitleView == null) {
            null
        } else mTitleView!!.text
    }

    /**
     * Sets the content text.
     */
    fun setContentText(text: CharSequence?) {
        if (mContentView == null) {
            return
        }
        mContentView!!.text = text
    }

    /**
     * Returns the content text.
     */
    fun getContentText(): CharSequence? {
        return if (mContentView == null) {
            null
        } else mContentView!!.text
    }

    /**
     * Sets the badge image drawable.
     */
    fun setBadgeImage(drawable: Drawable?) {
        if (mBadgeImage == null) {
            return
        }
        mBadgeImage!!.setImageDrawable(drawable)
        if (drawable != null) {
            mBadgeImage!!.visibility = View.VISIBLE
        } else {
            mBadgeImage!!.visibility = View.GONE
        }
    }

    /**
     * Returns the badge image drawable.
     */
    fun getBadgeImage(): Drawable? {
        return if (mBadgeImage == null) {
            null
        } else mBadgeImage!!.drawable
    }

    private fun fadeIn() {
        mImageView!!.alpha = 0f
        if (mAttachedToWindow) {
            mFadeInAnimator!!.start()
        }
    }

    override fun hasOverlappingRendering(): Boolean {
        return false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mAttachedToWindow = true
        if (mImageView!!.alpha == 0f) {
            fadeIn()
        }
    }

    override fun onDetachedFromWindow() {
        mAttachedToWindow = false
        mFadeInAnimator!!.cancel()
        mImageView!!.alpha = 1f
        super.onDetachedFromWindow()
    }
}