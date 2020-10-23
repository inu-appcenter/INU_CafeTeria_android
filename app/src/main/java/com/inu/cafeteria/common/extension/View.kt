/**
 * This file is part of INU Cafeteria.
 *
 * Copyright (C) 2020 INU Global App Center <potados99@gmail.com>
 *
 * INU Cafeteria is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * INU Cafeteria is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.inu.cafeteria.common.extension

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

fun View.cancelTransition() {
    transitionName = null
}

fun View.setBackgroundTint(color: Int) {

    // API 21 doesn't support this
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
        background?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    }

    backgroundTintList = ColorStateList.valueOf(color)
}

fun View.setPadding(left: Int? = null, top: Int? = null, right: Int? = null, bottom: Int? = null) {
    setPadding(left ?: paddingLeft, top ?: paddingTop, right ?: paddingRight, bottom ?: paddingBottom)
}

var View.isVisible: Boolean
    get() = this.visibility == View.VISIBLE
    set(visible) {
        this.visibility = if (visible) View.VISIBLE else View.GONE
    }

fun View.setVisible(visible: Boolean, invisible: Int = View.GONE) {
    visibility = if (visible) View.VISIBLE else invisible
}

/**
 * If a view captures clicks at all, then the parent won't ever receive touch events. This is a
 * problem when we're trying to capture link clicks, but tapping or long pressing other areas of
 * the view no longer work. Also problematic when we try to long press on an image in the message
 * view
 */

fun View.forwardTouches(parent: View) {
    var isLongClick = false

    setOnLongClickListener {
        isLongClick = true
        true
    }

    setOnTouchListener { v, event ->
        parent.onTouchEvent(event)

        when {
            event.action == MotionEvent.ACTION_UP && isLongClick -> {
                isLongClick = true
                true
            }

            event.action == MotionEvent.ACTION_DOWN -> {
                isLongClick = false
                v.onTouchEvent(event)
            }

            else -> v.onTouchEvent(event)
        }
    }
}

fun <T: View> T?.withinAlphaAnimation(from: Float, to: Float, delay: Long = 0, action: T?.() -> Unit = {}) {
    this?.let {
        alpha = from
    }

    action(this)

    this?.let {
        Handler(Looper.getMainLooper()).postDelayed({ animate().alpha(to) }, delay)
    }
}

fun View.slideInWithFade(directionVector: Int) {
    alpha = 0f
    x += -30f * directionVector
    animate().alpha(1f).x(0f)
}

fun View.removeFromParent() {
    (parent as? ViewGroup)?.removeView(this)
}