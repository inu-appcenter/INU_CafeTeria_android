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

package com.inu.cafeteria.feature.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.inu.cafeteria.R
import com.inu.cafeteria.common.Navigator
import com.inu.cafeteria.common.base.BaseFragment
import com.inu.cafeteria.common.base.BaseViewModel
import com.inu.cafeteria.common.extension.defaultDataErrorHandle
import com.inu.cafeteria.common.extension.finishActivity
import com.inu.cafeteria.exception.ResponseFailException
import com.inu.cafeteria.model.scheme.LoginParams
import com.inu.cafeteria.model.scheme.LoginResult
import com.inu.cafeteria.repository.LoginRepository
import com.inu.cafeteria.repository.StudentInfoRepository
import com.inu.cafeteria.usecase.Login
import org.koin.core.inject
import timber.log.Timber

class LoginViewModel : BaseViewModel() {

    private val login: Login by inject()

    private val studentInfoRepo: StudentInfoRepository by inject()
    private val loginRepo: LoginRepository by inject()

    private val navigator: Navigator by inject()

    /**
     * Represents login status.
     * Fragment will observe this and take an adequate action.
     */

private val _loginInProgress = MutableLiveData<Boolean>()
    var loginInProgress: LiveData<Boolean> = _loginInProgress

    init {
        failables += this
        failables += login
        failables += studentInfoRepo
        failables += navigator
    }

    fun passIfLoggedIn(fragment: BaseFragment): Boolean {
        if (loginRepo.isLoggedIn()) {
            Timber.i("Already logged in. Pass!")
            showMain(fragment)
            return true
        }

        return false
    }

    /**
     * Try auto login.
     *
     * @param onNoToken launched when no token saved.
     * @param onSuccess launched when successfully login with token.
     * @param onFail launched when request failed.
     */

fun tryAutoLogin(
        onNoToken: () -> Unit,
        onSuccess: (LoginResult) -> Unit,
        onFail: (Exception) -> Unit
    ) {
        _loginInProgress.value = true

        val token = studentInfoRepo.getLoginToken()

        if (token.isNullOrEmpty()) {
            onNoToken()
            _loginInProgress.value = false
            return
        }

        login(LoginParams.ofUsingToken(token)) {
            it.onSuccess(onSuccess).onError(onFail)

            // Finally
            _loginInProgress.value = false
        }
    }

    /**
     * Try login with given ID and password.
     *
     * @param id student id.
     * @param pw the password.  TODO: ENCRYPT IT.
     * @param auto to save login data or not.
     * @param onSuccess launched when successfully logged in.
     * @param onFail launched when request failed.
     */

fun tryLoginWithIdAndPw(
        id: String,
        pw: String,
        auto: Boolean,
        onSuccess: (LoginResult) -> Unit,
        onFail: (Exception) -> Unit
    ) {
        _loginInProgress.value = true

        login(LoginParams.ofFirstLogin(id = id, pw = pw, save = auto)) {
            it.onSuccess(onSuccess).onError(onFail)

            // Finally
            _loginInProgress.value = false
        }
    }

    /**
     * We need to invalidate stored student id if we failed to auto login.
     */

fun handleAutoLoginFailure(e: Exception) {
        when (e) {
            is ResponseFailException -> {
                studentInfoRepo.invalidate()
                fail(R.string.fail_token_invalid, show = true)
                Timber.w("Token is invalid. Invalidate all student data.")
            }
            else -> defaultDataErrorHandle(e)
        }
    }

    /**
     * Handle login failures.
     */

fun handleLoginFailure(e: Exception) {
        when (e) {
            is ResponseFailException -> {
                fail(R.string.fail_wrong_auth, show = true)
            }
            else -> defaultDataErrorHandle(e)
        }
    }

    /**
     * Save token and barcode obtained from successful login.
     */

fun saveLoginResult(result: LoginResult, id: String) {
        with(studentInfoRepo) {
            setStudentId(id)
            setLoginToken(result.token)
            setBarcode(result.barcode)
        }
    }

    /**
     * Navigate to MainActivity and close current activity.
     */

fun showMain(fragment: BaseFragment) {
        navigator.showMain()
        fragment.finishActivity()
    }
}