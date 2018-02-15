package com.blackboardtheory.carousellayoutmanager

import android.support.annotation.NonNull
import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * Created by bdevereaux3 on 2/13/18.
 */
abstract class CarouselChildSelectionListener(@NonNull recyclerView: RecyclerView, @NonNull carouselLayoutManager: CarouselLayoutManager) {
    val mRecyclerView = recyclerView
    val mCarouselLayoutManager = carouselLayoutManager

    val mOnClickListener = object : View.OnClickListener {
        override fun onClick(v: View?) {
            val holder = mRecyclerView.getChildViewHolder(v!!)
            val position = holder.adapterPosition

            if(position == mCarouselLayoutManager.mCenterItemPosition) {
                onCenterItemClicked(mRecyclerView, mCarouselLayoutManager, v)
            }
            else {
                onBackItemClicked(mRecyclerView, mCarouselLayoutManager, v)
            }
        }
    }


    init {
        mRecyclerView.addOnChildAttachStateChangeListener(object: RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewDetachedFromWindow(view: View?) {
                view?.let{
                    it.setOnClickListener(mOnClickListener)
                }
            }

            override fun onChildViewAttachedToWindow(view: View?) {
                view?.let{
                    it.setOnClickListener(null)
                }
            }
        })
    }

    abstract fun onCenterItemClicked(@NonNull recyclerView: RecyclerView, @NonNull carouselLayoutManager: CarouselLayoutManager, @NonNull view: View)

    abstract fun onBackItemClicked(@NonNull recyclerView: RecyclerView, @NonNull carouselLayoutManager: CarouselLayoutManager, @NonNull view: View)
}