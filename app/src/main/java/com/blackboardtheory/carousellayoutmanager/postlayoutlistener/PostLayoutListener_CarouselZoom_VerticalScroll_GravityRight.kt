package com.blackboardtheory.carousellayoutmanager.postlayoutlistener

import android.view.View
import com.blackboardtheory.carousellayoutmanager.CarouselLayoutManager
import com.blackboardtheory.carousellayoutmanager.ItemTransformation

/**
 * Created by bdevereaux3 on 2/16/18.
 */

class PostLayoutListener_CarouselZoom_VerticalScroll_GravityRight : CarouselLayoutManager.PostLayoutListener {
    override fun transformChild(child: View, itemPositionToCenterDiff: Float, orientation: Int): ItemTransformation {
        val scale: Float = (2 * (2 * -StrictMath.atan(Math.abs(itemPositionToCenterDiff) + 1.0) / Math.PI + 1)).toFloat()

        var translateY: Float
        var translateX: Float

        if(CarouselLayoutManager.VERTICAL == orientation) {
            val translateYGeneral = child.measuredHeight * (1 - scale)
            translateY = Math.signum(itemPositionToCenterDiff) * translateYGeneral
            translateX = child.measuredWidth - (child.measuredWidth * scale)
        }
        else {// this should never happen
            val translateXGeneral = child.measuredWidth  * (1 - scale)
            translateX = Math.signum(itemPositionToCenterDiff) * translateXGeneral
            translateY = child.measuredHeight - (child.measuredHeight * scale)
        }
        return ItemTransformation(scale, scale, translateX, translateY)
    }
}