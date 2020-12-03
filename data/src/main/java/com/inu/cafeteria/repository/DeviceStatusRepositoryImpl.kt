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

package com.inu.cafeteria.repository

import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.inu.cafeteria.util.NetworkHelper

class DeviceStatusRepositoryImpl(
    private val manager: ConnectivityManager
) : DeviceStatusRepository {

    private val online = MutableLiveData(isOnline())

    override fun init() {
        startObservingNetworkState()
    }

    private fun startObservingNetworkState() {
        val onOnline = { postAfterSomeTime() }
        val onOffline = { online.postValue(false) }

        NetworkHelper.onNetworkChange(manager, onOnline, onOffline)
    }

    private fun postAfterSomeTime() {
        // Don't know why but after network turned available and isOnline() returns true,
        // the network doesn't work for a while.
        // So we need to wait for it become 'really' available.
        Handler(Looper.getMainLooper()).postDelayed({
            if (online.value != true) {
                // Prevent duplicated event!
                online.postValue(true)
            }
        }, 500)
    }

    override fun isOnline(): Boolean {
        return NetworkHelper.isOnline(manager)
    }

    override fun isOnlineEvent(): LiveData<Boolean> = online
}
