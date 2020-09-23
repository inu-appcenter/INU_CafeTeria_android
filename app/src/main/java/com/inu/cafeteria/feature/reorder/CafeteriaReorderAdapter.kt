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

package com.inu.cafeteria.feature.reorder

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MotionEventCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.inu.cafeteria.R
import com.inu.cafeteria.common.base.BaseAdapter
import com.inu.cafeteria.common.base.BaseViewHolder
import com.inu.cafeteria.common.widget.ItemTouchHelperAdapter
import com.inu.cafeteria.feature.main.CafeteriaView
import kotlinx.android.synthetic.main.cafeteria.view.cafeteria_name
import kotlinx.android.synthetic.main.cafeteria_reorder_item.view.*
import java.util.*

class CafeteriaReorderAdapter
    : BaseAdapter<CafeteriaReorderView>(),
    ItemTouchHelperAdapter {

    private var touchHelper: ItemTouchHelper? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        BaseViewHolder(parent, R.layout.cafeteria_reorder_item).also(::setTouchListener)

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        with(holder.containerView.cafeteria_name) {
            text = getItem(position)?.displayName
        }
    }

    override fun onSetItemTouchHelper(itemTouchHelper: ItemTouchHelper) {
        touchHelper = itemTouchHelper
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(data, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(data, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    override fun onItemDismiss(position: Int) {
        data.removeAt(position)
        notifyItemRemoved(position)
    }

    private fun setTouchListener(viewHolder: BaseViewHolder) {
        viewHolder.containerView.handle.setOnTouchListener { v, event ->
            v.performClick()
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                touchHelper?.startDrag(viewHolder)
            }
            false
        }
    }
}