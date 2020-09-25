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

package com.inu.cafeteria.feature.main

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.inu.cafeteria.R
import com.inu.cafeteria.common.Navigator
import com.inu.cafeteria.common.base.BaseViewModel
import com.inu.cafeteria.entities.Cafeteria
import com.inu.cafeteria.extension.afterDays
import com.inu.cafeteria.extension.applyOrder
import com.inu.cafeteria.extension.format
import com.inu.cafeteria.usecase.GetCafeteria
import com.inu.cafeteria.usecase.GetCafeteriaOrder
import com.inu.cafeteria.util.SingleLiveEvent
import org.koin.core.inject
import java.util.*

class CafeteriaViewModel : BaseViewModel() {

    private val getCafeteria: GetCafeteria by inject()
    private val getCafeteriaOrder: GetCafeteriaOrder by inject()

    private val navigator: Navigator by inject()

    private val _cafeteria = MutableLiveData<List<CafeteriaView>>()
    val cafeteria: LiveData<List<CafeteriaView>> = _cafeteria

    private val _loading = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

    private val cafeteriaCache: MutableMap<String, List<Cafeteria>> = mutableMapOf()

    val moreClickEvent = SingleLiveEvent<CafeteriaView>()

    fun preFetch(howMany: Int) {
        (0 until howMany).map(::daysAfter).forEach { date ->
            getCafeteria(date) { result ->
                result
                    .onSuccess { saveToCache(date, it) }
                    .onError(::handleFailure)
            }
        }
    }

    fun onSelectDateTab(tabPosition: Int) {
        fetch(daysAfter(tabPosition))
    }

    private fun daysAfter(days: Int): String = Date().afterDays(days).format()

    private fun fetch(date: String = daysAfter(0)) {
        startLoading()

        getFromCache(date)?.let {
            handleCafeteria(it)
            finishLoading(slowly = false)
            return
        }

        getCafeteria(date) { result ->
            result
                .onSuccess { saveToCache(date, it) }
                .onSuccess(::handleCafeteria)
                .onError(::handleFailure)
        }
    }

    private fun getFromCache(date: String) = cafeteriaCache[date]

    private fun saveToCache(date: String, data: List<Cafeteria>) {
        cafeteriaCache[date] = data
    }

    private fun startLoading() {
        _loading.value = true
    }

    private fun finishLoading(slowly: Boolean) {
        // God damn point: Even if the network job is finished and the result arrived,
        // we have to wait for a few more moments before we show up the cafeteria_recycler.
        // Otherwise it will slow down UI rendering.
        // We needed to right like below:
        //
        // Handler(Looper.getMainLooper()).postDelayed({
        //     _loading.value = false
        // }, 250)
        //
        // However it doesn't matter because we can pre-fetch all data(number of them are fixed!).

        Handler(Looper.getMainLooper()).postDelayed({
            _loading.value = false
        }, if (slowly) 250 else 0)
    }

    fun onClickOptionMenu(menuItemId: Int): Boolean {
        when(menuItemId) {
            R.id.menu_reorder -> navigator.showSorting()
        }

        return true
    }

    private fun handleCafeteria(allCafeteria: List<Cafeteria>) {
        getCafeteriaOrder(Unit) {
            it.onSuccess{ orderedIds ->
                handleCafeteriaOrdered(allCafeteria.applyOrder(orderedIds) { id })
            }.onError(::handleFailure)
        }
    }

    private fun handleCafeteriaOrdered(allCafeteriaOrdered: List<Cafeteria>) {
        val result = allCafeteriaOrdered.map { cafeteria ->
            CafeteriaView(
                id = cafeteria.id,
                name = cafeteria.displayName ?: cafeteria.name,
                wholeMenus = cafeteria.corners.map { corner ->
                    corner.menus.map { menu ->
                        MenuView.fromCornerAndMenu(corner, menu)
                    }
                }.flatten()
            )
        }

        this._cafeteria.value = result

        finishLoading(slowly = result.isNotEmpty())
    }

    private fun handleFailure(e: Exception) {
        Toast.makeText(mContext, e.message, Toast.LENGTH_SHORT).show()

        finishLoading(slowly = false)
    }

    fun onViewMore(cafeteriaView: CafeteriaView) {
        moreClickEvent.postValue(cafeteriaView)
    }
}