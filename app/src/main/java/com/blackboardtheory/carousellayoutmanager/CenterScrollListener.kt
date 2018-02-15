package com.blackboardtheory.carousellayoutmanager

import android.support.v7.widget.RecyclerView

/**
 * Created by bdevereaux3 on 2/13/18.
 */

class CenterScrollListener: RecyclerView.OnScrollListener() {
    private var mAutoSet = true

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)

        var layoutManager  = recyclerView.layoutManager
        if(!(layoutManager is CarouselLayoutManager)) {
            mAutoSet = true
            return
        }
        layoutManager =  layoutManager as CarouselLayoutManager
        if(!mAutoSet) {
            if(RecyclerView.SCROLL_STATE_IDLE == newState) {
                val scrollNeeded = layoutManager.getOffsetCenterView()
                if(CarouselLayoutManager.HORIZONTAL == layoutManager.orientation)
                    recyclerView.smoothScrollBy(scrollNeeded, 0)
                else
                    recyclerView.smoothScrollBy(0, scrollNeeded)
                mAutoSet = true
            }
        }
        if(RecyclerView.SCROLL_STATE_DRAGGING == newState || RecyclerView.SCROLL_STATE_SETTLING == newState)
            mAutoSet = false
    }
}

