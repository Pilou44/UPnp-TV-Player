package com.wechantloup.upnpvideoplayer.browse

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler

/**
 * [GridLayoutManager] extension which introduces workaround for focus finding bug when
 * navigating with dpad.
 *
 * @see [http://stackoverflow.com/questions/31596801/recyclerview-focus-scrolling](http://stackoverflow.com/questions/31596801/recyclerview-focus-scrolling)
 */
class BrowserLayoutManager : androidx.recyclerview.widget.GridLayoutManager {

    constructor(
        context: Context?, attrs: AttributeSet?, defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    constructor(context: Context?, spanCount: Int) : super(context, spanCount) {}
    constructor(
        context: Context?, spanCount: Int, orientation: Int,
        reverseLayout: Boolean
    ) : super(context, spanCount, orientation, reverseLayout) {
    }

    override fun onFocusSearchFailed(
        focused: View, focusDirection: Int,
        recycler: Recycler, state: RecyclerView.State
    ): View? {
        // Need to be called in order to layout new row/column
        val nextFocus: View = super.onFocusSearchFailed(focused, focusDirection, recycler, state)
            ?: return null
        val fromPos: Int = getPosition(focused)
        val nextPos = getNextViewPos(fromPos, focusDirection)
        return findViewByPosition(nextPos)
    }

    /**
     * Manually detect next view to focus.
     *
     * @param fromPos from what position start to seek.
     * @param direction in what direction start to seek. Your regular `View.FOCUS_*`.
     * @return adapter position of next view to focus. May be equal to `fromPos`.
     */
    protected fun getNextViewPos(fromPos: Int, direction: Int): Int {
        val offset = calcOffsetToNextView(direction)
        return if (hitBorder(fromPos, offset)) {
            fromPos
        } else fromPos + offset
    }

    /**
     * Calculates position offset.
     *
     * @param direction regular `View.FOCUS_*`.
     * @return position offset according to `direction`.
     */
    protected fun calcOffsetToNextView(direction: Int): Int {
        val spanCount: Int = getSpanCount()
        val orientation: Int = getOrientation()
        if (orientation == VERTICAL) {
            when (direction) {
                View.FOCUS_DOWN -> return spanCount
                View.FOCUS_UP -> return -spanCount
                View.FOCUS_RIGHT -> return 1
                View.FOCUS_LEFT -> return -1
            }
        } else if (orientation == HORIZONTAL) {
            when (direction) {
                View.FOCUS_DOWN -> return 1
                View.FOCUS_UP -> return -1
                View.FOCUS_RIGHT -> return spanCount
                View.FOCUS_LEFT -> return -spanCount
            }
        }
        return 0
    }

    /**
     * Checks if we hit borders.
     *
     * @param from from what position.
     * @param offset offset to new position.
     * @return `true` if we hit border.
     */
    private fun hitBorder(from: Int, offset: Int): Boolean {
        val spanCount: Int = getSpanCount()
        return if (Math.abs(offset) == 1) {
            val spanIndex = from % spanCount
            val newSpanIndex = spanIndex + offset
            newSpanIndex < 0 || newSpanIndex >= spanCount
        } else {
            val newPos = from + offset
            newPos < 0 && newPos >= spanCount
        }
    }
}