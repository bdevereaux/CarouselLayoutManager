package com.blackboardtheory.carousellayoutmanager

import android.graphics.PointF
import android.support.annotation.NonNull
import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * Created by bdevereaux3 on 2/13/18.
 */

class CarouselSmoothScroller(@NonNull state: RecyclerView.State, position: Int) {
    init {
        if(0 > position) {
            throw IllegalArgumentException("position can't be less than 0. position is : " + position)
        }
        if(position >= state.itemCount) {
            throw IllegalArgumentException("position can't be greater than adapter itemCount. position is : " + position)
        }
    }

    fun computeScrollVectorForPosition(targetPosition: Int, @NonNull carouselLayoutManager: CarouselLayoutManager): PointF {
        return carouselLayoutManager.computeScrollVectorForPosition(targetPosition)!!
    }

    fun calculateDyToMakeVisible(view: View, @NonNull carouselLayoutManager: CarouselLayoutManager): Int {
        if(!carouselLayoutManager.canScrollVertically())
            return 0
        return carouselLayoutManager.getOffsetForCurrentView(view)
    }

    fun calculateDxToMakeVisible(view: View, @NonNull carouselLayoutManager: CarouselLayoutManager): Int {
        if(!carouselLayoutManager.canScrollHorizontally())
            return 0
        return carouselLayoutManager.getOffsetForCurrentView(view)
    }
}