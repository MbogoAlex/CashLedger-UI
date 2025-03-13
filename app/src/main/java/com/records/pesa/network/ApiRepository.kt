package com.records.pesa.network

import com.records.pesa.models.backup.FilesUploadResponseBody
import com.records.pesa.models.payment.intasend.IntasendPaymentPayload
import com.records.pesa.models.payment.intasend.IntasendPaymentResponseBody
import com.records.pesa.models.payment.intasend.IntasendPaymentStatusPayload
import com.records.pesa.models.payment.intasend.IntasendPaymentStatusResponseBody
import com.records.pesa.models.payment.intasend.PaymentSavePayload
import com.records.pesa.models.payment.intasend.PaymentSaveResponseBody
import com.records.pesa.models.payment.intasend.PaymentsResponseBody
import com.records.pesa.models.user.UserResponseBody
import com.records.pesa.models.user.registration.UserRegistrationPayload
import com.records.pesa.models.user.update.UserBackupDataUpdatePayload
import com.records.pesa.models.user.update.UserProfileUpdatePayload
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response

interface ApiRepository {

    suspend fun registerUser(userRegistrationPayload: UserRegistrationPayload): Response<UserResponseBody>

    suspend fun updateUserProfile(userProfileUpdatePayload: UserProfileUpdatePayload): Response<UserResponseBody>

    suspend fun updateUserProfileBackupData(userBackupDataUpdatePayload: UserBackupDataUpdatePayload): Response<UserResponseBody>

    suspend fun getUserByUserId(id: String): Response<UserResponseBody>

    suspend fun getUserByPhoneNumber(phoneNumber: String): Response<UserResponseBody>

    suspend fun lipa(paymentPayload: IntasendPaymentPayload): Response<IntasendPaymentResponseBody>

    suspend fun lipaStatus(paymentStatusPayload: IntasendPaymentStatusPayload): Response<IntasendPaymentStatusResponseBody>

    suspend fun savePayment(paymentSavePayload: PaymentSavePayload): Response<PaymentSaveResponseBody>

    suspend fun getUserPayments(userId: String): Response<PaymentsResponseBody>

    suspend fun uploadFiles(files: List<MultipartBody.Part>): Response<FilesUploadResponseBody>

    suspend fun getFile(
        fileName: String
    ): Response<ResponseBody>
}

