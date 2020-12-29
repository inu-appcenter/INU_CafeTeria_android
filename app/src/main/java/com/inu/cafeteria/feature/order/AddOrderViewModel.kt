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

package com.inu.cafeteria.feature.order

import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.inu.cafeteria.common.base.BaseViewModel
import com.inu.cafeteria.common.extension.onChanged
import com.inu.cafeteria.entities.Cafeteria
import com.inu.cafeteria.entities.OrderInput
import com.inu.cafeteria.usecase.AddWaitingOrder
import com.inu.cafeteria.usecase.GetCafeteriaOnly
import com.inu.cafeteria.util.SingleLiveEvent
import org.koin.core.inject
import timber.log.Timber

class AddOrderViewModel : BaseViewModel() {

    private val addWaitingOrder: AddWaitingOrder by inject()
    private val getCafeteriaOnly: GetCafeteriaOnly by inject()

    private val cameraProviderLiveData = MutableLiveData<ProcessCameraProvider>()

    private val _cameraViewVisible = MutableLiveData(true)
    val cameraViewVisible: LiveData<Boolean> = _cameraViewVisible

    private val _manualViewVisible = MutableLiveData(false)
    val manualViewVisible: LiveData<Boolean> = _manualViewVisible

    private val _cafeteriaToChoose = MutableLiveData<List<CafeteriaSelectionView>>()
    val cafeteriaToChoose: LiveData<List<CafeteriaSelectionView>> = _cafeteriaToChoose

    private val _waitingNumberInputDone = MutableLiveData(false)
    val waitingNumberInputDone: LiveData<Boolean> = _waitingNumberInputDone

    val waitingNumberInput = ObservableField<String>().apply {
        onChanged {
            val numberString = get() ?: return@onChanged

            _waitingNumberInputDone.value = (numberString.length >= WAITING_NUMBER_LENGTH)
        }
    }

    val orderSuccessfullyAddedEvent = SingleLiveEvent<Unit>()

    fun getProcessorCameraProvider(): LiveData<ProcessCameraProvider> {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(mContext)

        cameraProviderFuture.addListener({
            try {
                cameraProviderLiveData.value = cameraProviderFuture.get()
            } catch (e: Exception) {
                Timber.e("Unhandled exception: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(mContext))

        return cameraProviderLiveData
    }

    /** Final destination */
    fun handleOrderInput(input: OrderInput) {
        // It is its responsibility to add the order to repository.
        addWaitingOrder(input) {
            it.onSuccess { orderSuccessfullyAddedEvent.call() }.onError(::handleFailure)
        }
    }

    fun changeToManualInput() {
        waitingNumberInput.set("")

        setInputMode(InputMode.MODE_MANUAL)
    }

    fun changeToCameraScan() {
        setInputMode(InputMode.MODE_CAMERA)
    }

    private fun setInputMode(mode: InputMode) {
        _cameraViewVisible.value = (mode == InputMode.MODE_CAMERA)
        _manualViewVisible.value = (mode == InputMode.MODE_MANUAL)
    }

    fun fetchCafeteriaSelectionOptions() {
        getCafeteriaOnly(Unit) {
            it.onSuccess(::handleCafeteriaResult).onError(::handleFailure)
        }
    }

    private fun handleCafeteriaResult(cafeteria: List<Cafeteria>) {
        _cafeteriaToChoose.value = cafeteria
            .filter { it.supportNotification }
            .map {
                CafeteriaSelectionView(
                    id = it.id,
                    displayName = it.displayName ?: it.name
                )
            }
    }

    fun handleManualCafeteriaSelection(cafeteria: CafeteriaSelectionView) {
        val waitingNumber = waitingNumberInput.get()?.toInt() ?: return
        val cafeteriaId = cafeteria.id

        handleOrderInput(OrderInput.UserFriendly(waitingNumber, cafeteriaId))
    }

    private enum class InputMode {
        MODE_CAMERA,
        MODE_MANUAL
    }

    companion object {
        private const val WAITING_NUMBER_LENGTH = 4
    }
}