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

import com.inu.cafeteria.extension.onResult
import com.inu.cafeteria.model.scheme.LoginParams
import com.inu.cafeteria.model.scheme.LoginResult
import com.inu.cafeteria.model.scheme.LogoutParams
import com.inu.cafeteria.model.scheme.LogoutResult
import com.inu.cafeteria.service.CafeteriaNetworkService
import timber.log.Timber

class LoginRepositoryImpl(
    private val networkService: CafeteriaNetworkService
) : LoginRepository() {

    private var login = false

    override fun isLoggedIn(): Boolean {
        return login
    }

    override fun login(params: LoginParams, callback: DataCallback<LoginResult>) {
        networkService.getLoginResult(params).onResult(
            async = callback.async,
            onSuccess = {
                setLoginIn(true)
                callback.onSuccess(it)
            },
            onFail = callback.onFail
        )
    }

    override fun logout(params: LogoutParams, callback: DataCallback<LogoutResult>) {
        networkService.getLogoutResult(params).onResult(
            async = callback.async,
            onSuccess = {
                setLoginIn(false)
                callback.onSuccess(it)
            },
            onFail = callback.onFail
        )
    }

    private fun setLoginIn(isLoggedIn: Boolean) {
        login = isLoggedIn
        Timber.i("Login state update. Logged ${if (isLoggedIn) "in" else "out"}.")
    }
}