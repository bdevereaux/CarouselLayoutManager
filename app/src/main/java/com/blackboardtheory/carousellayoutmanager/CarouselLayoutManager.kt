package com.blackboardtheory.carousellayoutmanager

import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.CallSuper
import android.support.annotation.NonNull
import android.support.annotation.Nullable
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearSmoothScroller
import android.support.v7.widget.OrientationHelper
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import java.lang.ref.WeakReference

/**
 * Created by bdevereaux3 on 2/13/18.
 */

class CarouselLayoutManager(val orientation: Int, val circleLayout: Boolean) : RecyclerView.LayoutManager(), RecyclerView.SmoothScroller.ScrollVectorProvider {

    /*
        Companion object to hold all of our static variables
     */
    companion object {
        const val HORIZONTAL = OrientationHelper.HORIZONTAL
        const val VERTICAL = OrientationHelper.VERTICAL

        const val INVALID_POSITION = -1
        const val MAX_VISIBLE_ITEMS = 2

        private const val CIRCLE_LAYOUT = false

        private fun makeScrollPositionInRange0ToCount(currentScrollPosition: Float, count: Int) : Float {
            var absCurrentScrollPosition = currentScrollPosition
            while(0 > absCurrentScrollPosition) {
                absCurrentScrollPosition += count
            }
            while(Math.round(absCurrentScrollPosition) >= count) {
                absCurrentScrollPosition -= count
            }
            return absCurrentScrollPosition
        }
    }

    private var mDecoratedChildWidth: Int? = null
    private var mDecoratedChildHeight: Int? = null

    var mOrientation: Int = orientation
    private var mCircleLayout: Boolean = circleLayout

    private var mPendingScrollPosition: Int = INVALID_POSITION

    private val mLayoutHelper = LayoutHelper(Companion.MAX_VISIBLE_ITEMS)

    private var mViewPostLayout: PostLayoutListener? = null

    private val mOnCenterItemSelectionListeners: MutableList<OnCenterItemSelectionListener> = arrayListOf()
    var mCenterItemPosition = INVALID_POSITION
    private var mItemsCount: Int = 0

    private var mPendingCarouselSavedState: CarouselSavedState? = null

    //todo CarouselSavedState class and mPendingCarouselSavedState variable


    /**********************************************************************************************/
    /************************************** Constructors ******************************************/
    /**********************************************************************************************/

    constructor(orientation: Int) : this(orientation, false)

    init {
        if(HORIZONTAL != orientation && VERTICAL != orientation) {
            throw IllegalArgumentException("orientation should be HORIZONTAL or VERTICAL")
        }
    }

    /**********************************************************************************************/
    /************************************* Public Methods *****************************************/
    /**********************************************************************************************/

    fun setPostLayoutListener(@Nullable postLayoutListener: PostLayoutListener) {
        mViewPostLayout = postLayoutListener
        requestLayout()
    }

    fun setMaxVisibleItems(maxVisibleItems: Int) {
        if(0 >= maxVisibleItems) {
            throw IllegalArgumentException("maxVisibleItems can't be less than 1")
        }
        mLayoutHelper.mMaxVisibleItems = maxVisibleItems
        requestLayout()
    }

    fun getMaxVisibleItems() : Int {
        return mLayoutHelper.mMaxVisibleItems
    }

    fun addOnItemSelectionListener(@NonNull onCenterItemSelectionListener: OnCenterItemSelectionListener) {
        mOnCenterItemSelectionListeners.add(onCenterItemSelectionListener)
    }

    fun removeOnItemSelectionListener(@NonNull onCenterItemSelectionListener: OnCenterItemSelectionListener) {
        mOnCenterItemSelectionListeners.remove(onCenterItemSelectionListener)
    }

    fun getWidthNoPadding() : Int {
        return width - paddingStart - paddingEnd
    }

    fun getHeightNoPadding() : Int {
        return height - paddingEnd - paddingStart
    }

    fun getOffsetForCurrentView(@NonNull view: View) : Int {
        val targetPosition = getPosition(view)
        val directionDistance = getScrollDirection(targetPosition)

        val distance = Math.round(directionDistance * getScrollItemSize())
        return if(mCircleLayout) distance else distance;// presumably something to change if in mCircleLayout or not
    }

    fun getOffsetCenterView() : Int {
        return Math.round(getCurrentScrollPosition()) * getScrollItemSize() - mLayoutHelper.mScrollOffset
    }

    /**********************************************************************************************/
    /************************************ Protected Methods ***************************************/
    /**********************************************************************************************/

    @CallSuper
    protected fun scrollBy(diff: Int, @NonNull recycler: RecyclerView.Recycler, @NonNull state: RecyclerView.State) : Int {
        if(null == mDecoratedChildWidth || null == mDecoratedChildHeight || 0 == childCount || 0 == diff) {
            return 0
        }
        var resultScroll = 0
        if(mCircleLayout) {
            resultScroll = diff

            mLayoutHelper.mScrollOffset += resultScroll

            val maxOffset = getScrollItemSize() * mItemsCount
            while(0 > mLayoutHelper.mScrollOffset) {
                mLayoutHelper.mScrollOffset += maxOffset
            }
            while(mLayoutHelper.mScrollOffset > maxOffset) {
                mLayoutHelper.mScrollOffset -= maxOffset
            }

            mLayoutHelper.mScrollOffset -= resultScroll
        }
        else {
            val maxOffset = getMaxScrollOffset()
            if(0 > mLayoutHelper.mScrollOffset + diff) {
                resultScroll = -mLayoutHelper.mScrollOffset
            }
            else if(mLayoutHelper.mScrollOffset + diff > maxOffset) {
                resultScroll = maxOffset - mLayoutHelper.mScrollOffset
            }
            else {
                resultScroll = diff
            }
        }
        if(0 != resultScroll) {
            mLayoutHelper.mScrollOffset += resultScroll
            fillData(recycler, state, false)
        }
        return resultScroll
    }

    protected fun getCardOffsetByPositionDiff(itemPositionDiff: Float) : Int {
        val smoothPosition = convertItemPositionDiffToSmoothPositionDiff(itemPositionDiff)

        var dimenDiff = 0
        if(VERTICAL == mOrientation) {
            dimenDiff = (getHeightNoPadding() - mDecoratedChildHeight!!) / 2
        }
        else {
            dimenDiff = (getWidthNoPadding() - mDecoratedChildWidth!!) / 2
        }

        return Math.round(Math.signum(itemPositionDiff) * dimenDiff * smoothPosition).toInt()
    }

    protected fun convertItemPositionDiffToSmoothPositionDiff(itemPositionDiff: Float) : Double {
        val absItemPositionDiff: Double = Math.abs(itemPositionDiff).toDouble()
        if(absItemPositionDiff > StrictMath.pow(1.0 / mLayoutHelper.mMaxVisibleItems, 1.0/ 3)) {
            return StrictMath.pow(absItemPositionDiff / mLayoutHelper.mMaxVisibleItems, 1/ 2.0.toDouble())
        }
        else {
            return StrictMath.pow(absItemPositionDiff, 2.0)
        }
    }

    protected fun getScrollItemSize() : Int {
        if(VERTICAL == mOrientation) {
            return mDecoratedChildHeight!!
        }
        else {
            return mDecoratedChildWidth!!
        }
    }

    /**********************************************************************************************/
    /************************************* Private Methods ****************************************/
    /**********************************************************************************************/

    private fun getScrollDirection(targetPosition: Int) : Float {
        val currentScrollPosition = makeScrollPositionInRange0ToCount(getCurrentScrollPosition(), mItemsCount)

        if(mCircleLayout) {
            val t1 = currentScrollPosition - targetPosition
            val t2 = Math.abs(t1) - mItemsCount
            if(Math.abs(t1) > Math.abs(t2)) {
                return Math.signum(t1) * t2
            }
            else {
                return t1
            }
        }
        else {
            return currentScrollPosition - targetPosition
        }
    }

    private fun calculateScrollForSelectingPosition(itemPosition: Int, state: RecyclerView.State) : Int {
        val fixedItemPosition = if(itemPosition < state.itemCount) itemPosition else state.itemCount - 1
        return fixedItemPosition  * (if(VERTICAL == mOrientation) mDecoratedChildHeight!! else mDecoratedChildWidth!!)
    }

    private fun fillData(@NonNull recycler: RecyclerView.Recycler?, @NonNull state: RecyclerView.State?, childMeasuringNeeded: Boolean) {
        val currentScrollPosition = getCurrentScrollPosition()
        generateLayoutOrder(currentScrollPosition, state!!)
        detachAndScrapAttachedViews(recycler)

        val width = getWidthNoPadding()
        val height = getHeightNoPadding()
        if(VERTICAL == mOrientation) {
            fillDataVertical(recycler!!, width, height, childMeasuringNeeded)
        }
        else {
            fillDataHorizontal(recycler!!, width, height, childMeasuringNeeded)
        }

        recycler?.let {
            it.clear()
        }

        detectOnItemSelectionChanged(currentScrollPosition, state)
    }

    private fun detectOnItemSelectionChanged(currentScrollPosition: Float, state: RecyclerView.State) {
        val absCurrentScrollPosition = makeScrollPositionInRange0ToCount(currentScrollPosition, state.itemCount)
        val centerItem = Math.round(absCurrentScrollPosition)

        if(mCenterItemPosition != centerItem) {
            mCenterItemPosition = centerItem
            Handler(Looper.getMainLooper()).post({
                selectItemCenterPosition(centerItem)
            })
        }
    }

    private fun selectItemCenterPosition(centerItem: Int) {
        for(onCenterItemSelectionListener: OnCenterItemSelectionListener in mOnCenterItemSelectionListeners) {
            onCenterItemSelectionListener.onCenterItemChanged(centerItem)
        }
    }

    private fun fillDataVertical(recycler: RecyclerView.Recycler, width: Int, height: Int, childMeasuringNeeded: Boolean) {
        val start = (width - mDecoratedChildWidth!!) / 2
        val end = start + mDecoratedChildWidth!!

        val centerViewTop = (height - mDecoratedChildHeight!!) / 2

        for(i in mLayoutHelper.mLayoutOrder.indices) {
            val layoutOrder = mLayoutHelper.mLayoutOrder[i]
            val offset = getCardOffsetByPositionDiff(layoutOrder!!.mItemPositionDiff!!)
            val top = centerViewTop + offset
            val bottom = top + mDecoratedChildHeight!!
            fillChildItem(start, top, end, bottom, layoutOrder!!, recycler, i, childMeasuringNeeded)
        }
    }

    private fun fillDataHorizontal(recycler: RecyclerView.Recycler, width: Int, height: Int, childMeasuringNeeded: Boolean) {
        val top = (height - mDecoratedChildHeight!!) / 2
        val bottom = top + mDecoratedChildHeight!!

        val centerViewStart = (width - mDecoratedChildWidth!!) / 2

        for(i in mLayoutHelper.mLayoutOrder.indices) {
            val layoutOrder = mLayoutHelper.mLayoutOrder[i]
            val offset = getCardOffsetByPositionDiff(layoutOrder!!.mItemPositionDiff!!)
            val start = centerViewStart + offset
            val end = start + mDecoratedChildWidth!!
            fillChildItem(start, top, end, bottom, layoutOrder!!, recycler, i, childMeasuringNeeded)
        }
    }

    private fun fillChildItem(start: Int, top: Int, end: Int, bottom: Int, @NonNull layoutOrder: LayoutOrder, @NonNull recycler: RecyclerView.Recycler, i: Int, childMeasuringNeeded: Boolean) {
        val view = bindChild(layoutOrder.mItemAdapterPosition!!, recycler, childMeasuringNeeded)
        ViewCompat.setElevation(view, i.toFloat())
        var transformation : ItemTransformation? = null
        mViewPostLayout?.let {
            transformation = it.transformChild(view, layoutOrder.mItemPositionDiff!!, mOrientation)
        }

        if(null == transformation) {
            view.layout(start, top, end, bottom)
        }
        else {
            view.layout(Math.round(start + transformation!!.mTranslationX).toInt(), Math.round(top + transformation!!.mTranslationY).toInt(),
                    Math.round(end + transformation!!.mTranslationX).toInt(), Math.round(bottom + transformation!!.mTranslationY).toInt())
            ViewCompat.setScaleX(view, transformation!!.mScaleX)
            ViewCompat.setScaleY(view, transformation!!.mScaleY)
        }
    }

    private fun getCurrentScrollPosition() : Float {
        val fullScrollSize = getMaxScrollOffset()
        return if(0 == fullScrollSize) 0f else 1.0f * mLayoutHelper.mScrollOffset / getScrollItemSize()
    }

    private fun getMaxScrollOffset() : Int {
        return getScrollItemSize() * (mItemsCount - 1)
    }

    private fun generateLayoutOrder(currentScrollPosition: Float, @NonNull state: RecyclerView.State) {
        mItemsCount = state.itemCount
        val absCurrentScrollPosition = makeScrollPositionInRange0ToCount(currentScrollPosition, mItemsCount)
        val centerItem = Math.round(absCurrentScrollPosition)

        if(mCircleLayout && 1 < mItemsCount) {
            val layoutCount = Math.min(mLayoutHelper.mMaxVisibleItems * 2 + 3, mItemsCount)

            mLayoutHelper.initLayoutOrder(layoutCount)

            val countLayoutHalf: Int = layoutCount / 2
            for(i in 1..countLayoutHalf) {
                val position: Int = Math.round(absCurrentScrollPosition - i + mItemsCount) % mItemsCount
                mLayoutHelper.setLayoutOrder(countLayoutHalf - i, position, centerItem - absCurrentScrollPosition - i)
            }
            for(i in layoutCount-1 downTo countLayoutHalf + 1) {
                val position: Int = Math.round(absCurrentScrollPosition - i + layoutCount) % mItemsCount
                mLayoutHelper.setLayoutOrder(i - 1, position, centerItem - absCurrentScrollPosition + layoutCount - i)
            }
            mLayoutHelper.setLayoutOrder(layoutCount - 1, centerItem, centerItem - absCurrentScrollPosition)
        }
        else {
            val firstVisible = Math.max(centerItem - mLayoutHelper.mMaxVisibleItems - 1, 0)
            val lastVisible = Math.min(centerItem + mLayoutHelper.mMaxVisibleItems + 1, mItemsCount - 1)
            val layoutCount = lastVisible - firstVisible + 1

            Log.d("bdev", "firstVisible: " + firstVisible.toString())
            Log.d("bdev", "lastVisible: " + lastVisible.toString())
            Log.d("bdev", "layoutCount: " + layoutCount.toString())

            mLayoutHelper.initLayoutOrder(layoutCount)

            for(i in firstVisible..lastVisible) {
                if(i == centerItem) {
                    Log.d("bdev", i.toString());
                    mLayoutHelper.setLayoutOrder(layoutCount - 1, i, i - absCurrentScrollPosition)
                }
                else if(i < centerItem) {
                    mLayoutHelper.setLayoutOrder(i - firstVisible, i, i - absCurrentScrollPosition)
                }
                else {
                    mLayoutHelper.setLayoutOrder(layoutCount - (i - centerItem) - 1, i, i - absCurrentScrollPosition)
                }
            }
        }
    }

    private fun bindChild(position: Int, @NonNull recycler: RecyclerView.Recycler?, childMeasuringNeeded: Boolean) : View {
        val view = recycler!!.getViewForPosition(position)
        addView(view)
        measureChildWithMargins(view, 0, 0)
        return view
    }

    /**********************************************************************************************/
    /************************************ Override Methods ****************************************/
    /**********************************************************************************************/

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun canScrollHorizontally() : Boolean {
        return 0 != childCount && HORIZONTAL == mOrientation
    }

    override fun canScrollVertically() : Boolean {
        return 0 != childCount && VERTICAL == mOrientation
    }

    override fun scrollToPosition(position: Int) {
        if(0 > position) {
            throw IllegalArgumentException("position can't be less than 0. position is : " + position)
        }
        mPendingScrollPosition = position
        requestLayout()
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView?, state: RecyclerView.State?, position: Int) {
        val linearSmoothScroller = object : LinearSmoothScroller(recyclerView?.context) {
            override fun calculateDyToMakeVisible(view: View, snapPreference: Int): Int {
                if(!canScrollVertically()) {
                    return 0
                }
                return getOffsetForCurrentView(view)
            }

            override fun calculateDxToMakeVisible(view: View, snapPreference: Int): Int {
                if(!canScrollHorizontally()) {
                    return 0
                }
                return getOffsetForCurrentView(view)
            }
        }
        linearSmoothScroller.targetPosition = position
        startSmoothScroll(linearSmoothScroller)
    }

    override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
        if(0 == childCount)
            return null

        var directionDistance = getScrollDirection(targetPosition)
        var direction = (-Math.signum(directionDistance)).toFloat()

        if(HORIZONTAL == mOrientation) {
            return PointF(direction, 0f)
        }
        else {
            return PointF(0f, direction)
        }
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        if(HORIZONTAL == mOrientation) {
            return 0
        }
        return scrollBy(dy, recycler!!, state!!)
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        if(VERTICAL == mOrientation) {
            return 0
        }
        return scrollBy(dx, recycler!!, state!!)
    }

    override fun onMeasure(recycler: RecyclerView.Recycler?, state: RecyclerView.State?, widthSpec: Int, heightSpec: Int) {
        mDecoratedChildHeight = null
        mDecoratedChildWidth = null

        super.onMeasure(recycler, state, widthSpec, heightSpec)
    }

    override fun onAdapterChanged(oldAdapter: RecyclerView.Adapter<*>?, newAdapter: RecyclerView.Adapter<*>?) {
        super.onAdapterChanged(oldAdapter, newAdapter)
        removeAllViews()
    }

    @CallSuper
    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        if(0 == state!!.itemCount) {
            removeAndRecycleAllViews(recycler)
            selectItemCenterPosition(INVALID_POSITION)
            return
        }

        var childMeasuringNeeded = false
        if(null == mDecoratedChildWidth) {
            val view = recycler!!.getViewForPosition(0)
            addView(view)
            measureChildWithMargins(view, 0, 0)

            mDecoratedChildWidth = getDecoratedMeasuredWidth(view)
            mDecoratedChildHeight = getDecoratedMeasuredHeight(view)
            removeAndRecycleView(view, recycler)

            if(INVALID_POSITION == mPendingScrollPosition && null == mPendingCarouselSavedState) {
                mPendingScrollPosition = mCenterItemPosition
            }

            childMeasuringNeeded = true
        }

        if(INVALID_POSITION != mPendingScrollPosition) {
            val itemsCount = state.itemCount
            mPendingScrollPosition = if(0 == itemsCount) INVALID_POSITION else Math.max(0, Math.min(itemsCount - 1, mPendingScrollPosition))
        }
        if(INVALID_POSITION != mPendingScrollPosition) {
            mLayoutHelper.mScrollOffset = calculateScrollForSelectingPosition(mPendingScrollPosition, state)
            mPendingScrollPosition = INVALID_POSITION
            mPendingCarouselSavedState = null
        }
        else if(null != mPendingCarouselSavedState) {
            mLayoutHelper.mScrollOffset = calculateScrollForSelectingPosition(mPendingCarouselSavedState!!.mCenterItemPosition, state)
            mPendingCarouselSavedState = null
        }
        else if(state.didStructureChange() && INVALID_POSITION != mCenterItemPosition) {
            mLayoutHelper.mScrollOffset = calculateScrollForSelectingPosition(mCenterItemPosition, state)
        }

        fillData(recycler, state, childMeasuringNeeded)
    }

    override fun onSaveInstanceState(): Parcelable {
        mPendingCarouselSavedState?.let {
            return CarouselSavedState(it)
        }
        val carouselSavedState = CarouselSavedState(super.onSaveInstanceState())
        carouselSavedState.mCenterItemPosition = mCenterItemPosition
        return carouselSavedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if(state is CarouselSavedState) {
            mPendingCarouselSavedState = state
            super.onRestoreInstanceState(mPendingCarouselSavedState!!.mSuperState)
        }
        else {
            super.onRestoreInstanceState(state)
        }
    }


    /**********************************************************************************************/
    /********************************** Necessary helper classes***********************************/
    /**********************************************************************************************/

    /**
     * Helper class that holds currently visible items.
     * Generally this class fills this list
     *
     * This class holds all scroll and maxVisible items state
     */
    private class LayoutHelper(maxVisibleItems: Int) {
        var mMaxVisibleItems : Int = maxVisibleItems
        var mScrollOffset: Int = 0

        var mLayoutOrder: Array<LayoutOrder?> = arrayOf()
        private var mReusedItems: MutableList<WeakReference<LayoutOrder>> = arrayListOf()

        fun initLayoutOrder(layoutCount: Int) {
           if(null == mLayoutOrder || mLayoutOrder?.size != layoutCount) {
               mLayoutOrder?.let {
                   recycleItems(*it)
               }
               mLayoutOrder = arrayOfNulls(layoutCount)
               fillLayoutOrder()
           }
        }

        fun setLayoutOrder(arrayPosition: Int, itemAdapterPosition: Int, itemPositionDiff: Float) {
            Log.d("bdev", "mLayoutOrder size: " + mLayoutOrder.size.toString())
            mLayoutOrder[arrayPosition]?.let {
                it.mItemPositionDiff = itemPositionDiff
                it.mItemAdapterPosition = itemAdapterPosition
            }
        }

        fun hasAdapterPosition(adapterPosition: Int): Boolean {
            mLayoutOrder?.let {
                for(layoutOrder : LayoutOrder? in mLayoutOrder) {
                    layoutOrder?.let {
                        if(it.mItemAdapterPosition == adapterPosition) {
                            return true
                        }
                    }
                }
            }
            return false
        }

        private fun recycleItems(vararg layoutOrders: LayoutOrder?) {
            for(layoutOrder : LayoutOrder? in layoutOrders) {
                layoutOrder?.let {
                    mReusedItems.add(WeakReference(it))
                }
            }
        }

        private fun fillLayoutOrder() {
            for(i in mLayoutOrder.indices) {
                if(null == mLayoutOrder[i]) {
                    mLayoutOrder[i] = createLayoutOrder()
                }
            }
        }

        private fun createLayoutOrder(): LayoutOrder {
            val iterator = mReusedItems.iterator()
            while(iterator.hasNext()) {
                val layoutOrderWeakReference = iterator.next()
                val layoutOrder = layoutOrderWeakReference.get()
                iterator.remove()
                layoutOrder?.let {
                    return it
                }
            }
            return LayoutOrder()
        }

    }

    /**
     * Class that holds item data
     * This class is filled during #generateLayoutOrder(float, RecyclerView.State) and used during #fillData(RecyclerView.Recycler, RecyclerView.State, boolean)
     */
    private class LayoutOrder {
        var mItemAdapterPosition: Int? = null;
        var mItemPositionDiff: Float? = null;
    }

    interface OnCenterItemSelectionListener {
        fun onCenterItemChanged(adapterPosition: Int)
    }

    interface PostLayoutListener {
        fun transformChild(@NonNull child: View, itemPositionToCenterDiff: Float, orientation: Int): ItemTransformation
    }

    protected class CarouselSavedState : Parcelable {

        lateinit var mSuperState: Parcelable
        var mCenterItemPosition: Int = -1

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<CarouselSavedState> {
                override fun createFromParcel(parcel: Parcel): CarouselSavedState {
                    return CarouselSavedState(parcel)
                }

                override fun newArray(size: Int): Array<CarouselSavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }

        constructor(superState: Parcelable) {
            mSuperState = superState
        }

        constructor(other: CarouselSavedState) {
            mSuperState = other.mSuperState
            mCenterItemPosition = other.mCenterItemPosition
        }

        private constructor(@NonNull inParcel: Parcel) {
            mSuperState = inParcel.readParcelable<Parcelable>(Parcelable::class.java.classLoader)
            mCenterItemPosition = inParcel.readInt()
        }

        override fun writeToParcel(outParcel: Parcel, flags: Int) {
            outParcel.writeParcelable(mSuperState, flags)
            outParcel.writeInt(mCenterItemPosition)
        }

        override fun describeContents(): Int {
            return 0
        }
    }

}