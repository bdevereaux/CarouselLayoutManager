package com.blackboardtheory.carousellayoutmanager.postlayoutlistener

import android.view.View
import com.blackboardtheory.carousellayoutmanager.CarouselLayoutManager
import com.blackboardtheory.carousellayoutmanager.ItemTransformation

/**
 * Created by bdevereaux3 on 2/13/18.
 */


class CarouselZoomPostLayoutListener_Centered : CarouselLayoutManager.PostLayoutListener {

    override fun transformChild(child: View, itemPositionToCenterDiff: Float, orientation: Int): ItemTransformation {
        val scale: Float = (2 * (2 * -StrictMath.atan(Math.abs(itemPositionToCenterDiff) + 1.0) / Math.PI + 1)).toFloat()

        var translateY: Float
        var translateX: Float

        if(CarouselLayoutManager.VERTICAL == orientation) {
            val translateYGeneral = child.measuredHeight * (1 - scale) / 2f
            translateY = Math.signum(itemPositionToCenterDiff) * translateYGeneral
            translateX = 0f
        }
        else {
            val translateXGeneral = child.measuredWidth * (1 - scale) / 2f
            translateX = Math.signum(itemPositionToCenterDiff) * translateXGeneral
            translateY = 0f
        }
        return ItemTransformation(scale, scale, translateX, translateY)
    }
}

