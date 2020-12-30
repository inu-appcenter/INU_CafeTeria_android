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

package com.inu.cafeteria.usecase

import com.inu.cafeteria.entities.OrderInput
import com.inu.cafeteria.exception.NoCredentialsException
import com.inu.cafeteria.functional.Result
import com.inu.cafeteria.interactor.UseCase
import com.inu.cafeteria.repository.ExternalCredentialsRepository
import com.inu.cafeteria.repository.WaitingOrderRepository

class AddWaitingOrder(
    private val waitingOrderRepo: WaitingOrderRepository,
    private val externalCredentialsRepo: ExternalCredentialsRepository
) : UseCase<OrderInput, Unit>() {

    override fun run(params: OrderInput) = Result.of {
        val deviceIdentifier = externalCredentialsRepo.getFirebaseToken() ?: throw NoCredentialsException("Firebase token not found!")

        waitingOrderRepo.addWaitingOrder(params, deviceIdentifier)
    }
}