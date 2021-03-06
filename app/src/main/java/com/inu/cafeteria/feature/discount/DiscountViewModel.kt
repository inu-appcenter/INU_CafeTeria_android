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

package com.inu.cafeteria.feature.discount

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.inu.cafeteria.R
import com.inu.cafeteria.common.base.BaseViewModel
import com.inu.cafeteria.common.navigation.Navigator
import com.inu.cafeteria.common.onboarding.OnboardingHintEventEmitter
import com.inu.cafeteria.entities.Account
import com.inu.cafeteria.entities.OnboardingHint
import com.inu.cafeteria.exception.NoAccountException
import com.inu.cafeteria.exception.UnauthorizedException
import com.inu.cafeteria.feature.main.EasterEggHelper
import com.inu.cafeteria.service.AccountService
import com.inu.cafeteria.usecase.*
import com.inu.cafeteria.util.SingleLiveEvent
import org.koin.core.inject
import timber.log.Timber

class DiscountViewModel : BaseViewModel() {

    private val createBarcode: CreateBarcode by inject()
    private val rememberedLogin: RememberedLogin by inject()
    private val activateBarcode: ActivateBarcode by inject()
    private val getSavedAccount: GetSavedAccount by inject()
    private val getCafeteria: GetDiscountSupportingCafeteria by inject()

    private val accountService: AccountService by inject()

    private val navigator: Navigator by inject()

    private val eggs = EasterEggHelper.getBarcodeCardEasterEggs()

    private val _barcodeCardReady = MutableLiveData(false)
    val barcodeCardReady: LiveData<Boolean> = _barcodeCardReady

    private val _onceLoggedIn = MutableLiveData(false)
    val onceLoggedIn: LiveData<Boolean> = _onceLoggedIn

    private val _barcodeBitmap = MutableLiveData<Bitmap>()
    val barcodeBitmap: LiveData<Bitmap> = _barcodeBitmap

    private val _barcodeContent = MutableLiveData<String>(null)
    val barcodeContent: LiveData<String> = _barcodeContent

    private val _studentId = MutableLiveData<String>(null)
    val studentId: LiveData<String> = _studentId

    val loggedInStatus = accountService.loggedInStatus()

    private val _bright = MutableLiveData(false)
    val bright: LiveData<Boolean> = _bright

    private val brightnessHintEmitter = OnboardingHintEventEmitter(OnboardingHint.ToggleBrightness)
    val showBrightnessToggleHintEvent = brightnessHintEmitter.event

    val showDiscountServiceDescriptionEvent = SingleLiveEvent<Pair<String, String>>()

    fun emitHintEvent() {
        // This will be called every onResume.
        if (_barcodeCardReady.value == true) {
            // However, the hint chance comes only when barcode card is ready.
            brightnessHintEmitter.emitIfAvailable()
        }
    }

    fun markHintShown() {
        brightnessHintEmitter.markHintAccepted()
    }

    fun preload() {
        _barcodeCardReady.value = false
        _onceLoggedIn.value = accountService.isLoggedIn() || accountService.hasSavedAccount()
    }

    fun load() {
        if (handleIfOffline()) {
            Timber.d("Device is offline. Cancel loading discount view model.")
            return
        }

        Timber.d("Loading discount view model(maybe again)!")

        preload()

        when {
            accountService.isLoggedIn() -> {
                Timber.d("User is logged in! Just show the barcode.")

                showBarcode()
            }
            accountService.hasSavedAccount() -> {
                Timber.d("User is not logged in but has saved account! Remembered login available.")

                loginAndShowBarcode()
            }
            else -> {
                // Prompt user to login
                Timber.d("Login is needed! Prompt user to do it.")
            }
        }
    }

    fun onClickLogin() {
        navigator.showLogin()
    }

    fun onClickBarcodeCard() {
        toggleBrightness()
        giveSomeEggs()
    }

    private fun toggleBrightness() {
        _bright.value = !(_bright.value ?: false)
    }

    private fun giveSomeEggs() {
        eggs.haveSomeFun()
    }

    private fun showBarcode() {
        getSavedAccount(Unit) {
            it.onSuccess(::showBarcodeForAccount).onError(::handleFailure)
        }
    }

    private fun loginAndShowBarcode() {
        rememberedLogin(Unit) {
            it.onSuccess { showBarcode() }.onError(::handleLoginFailure)
        }
    }

    private fun showBarcodeForAccount(account: Account) {
        val barcodeRatio = context.getString(R.string.barcode_ratio).toFloat()
        val barcodeWidth = context.resources.getDimensionPixelSize(R.dimen.barcode_width)
        val barcodeHeight = (barcodeWidth / barcodeRatio).toInt()

        createBarcode(Triple(account.barcode, barcodeWidth, barcodeHeight)) {
            it.onSuccess(::handleBarcodeImage).onError(::handleFailure)
        }

        activateBarcode(Unit) {
            it.onError(::handleFailure)
        }

        _studentId.value = account.id.toString()
        _barcodeContent.value = account.barcode
    }

    private fun handleBarcodeImage(image: Bitmap) {
        _barcodeBitmap.value = image
        _barcodeCardReady.value = true

        emitHintEvent()
    }

    private fun handleLoginFailure(exception: Exception) {
        when (exception) {
            is NoAccountException -> promptLoginAgain()
            is UnauthorizedException -> promptLoginAgain()
            else -> handleFailure(exception)
        }
    }

    private fun promptLoginAgain() {
        // Invalidate
        accountService.deleteSavedAccount()

        _barcodeCardReady.value = false
        _onceLoggedIn.value = false
    }

    fun onClickOptionMenu(menuItemId: Int): Boolean {
        when(menuItemId) {
            R.id.menu_info -> showDiscountServiceDescription()
        }

        return true
    }

    private fun showDiscountServiceDescription() {
        getCafeteria(Unit) { result ->
            result.onSuccess {
                val title = context.getString(R.string.title_discount_service_description)
                val body = context.getString(R.string.description_discount_service, it.joinToString { c -> c.displayName ?: c.name })

                showDiscountServiceDescriptionEvent.value = Pair(title, body)
            }
        }
    }
}