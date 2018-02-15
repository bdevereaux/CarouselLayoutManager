package com.blackboardtheory.carousellayoutmanager

import android.support.annotation.NonNull
import android.support.v7.widget.RecyclerView
import android.view.View


/**
 * Created by bdevereaux3 on 2/13/18.
 */

class DefaultChildSelectionListener(@NonNull onCenterItemClickListener: OnCenterItemClickListener, @NonNull recyclerView: RecyclerView, carouselLayoutManager: CarouselLayoutManager) : CarouselChildSelectionListener(recyclerView, carouselLayoutManager) {
    val mOnCenterItemClickListener = onCenterItemClickListener

    companion object Factory {
        fun initCenterItemListener(@NonNull onCenterItemClickListener: OnCenterItemClickListener, @NonNull recyclerView: RecyclerView, @NonNull carouselLayoutManager: CarouselLayoutManager) : DefaultChildSelectionListener {
            return DefaultChildSelectionListener(onCenterItemClickListener, recyclerView, carouselLayoutManager)
        }
    }

    interface OnCenterItemClickListener {
        fun onCenterItemClicked(@NonNull recyclerView: RecyclerView, @NonNull carouselLayoutManager: CarouselLayoutManager, @NonNull view: View)
    }

    override fun onCenterItemClicked(recyclerView: RecyclerView, carouselLayoutManager: CarouselLayoutManager, view: View) {
        mOnCenterItemClickListener.onCenterItemClicked(recyclerView, carouselLayoutManager, view)
    }

    override fun onBackItemClicked(recyclerView: RecyclerView, carouselLayoutManager: CarouselLayoutManager, view: View) {
        recyclerView.smoothScrollToPosition(carouselLayoutManager.getPosition(view))
    }
}