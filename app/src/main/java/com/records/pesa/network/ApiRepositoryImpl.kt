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

class ApiRepositoryImpl(private val apiService: ApiService): ApiRepository {
    override suspend fun registerUser(userRegistrationPayload: UserRegistrationPayload): Response<UserResponseBody> =
        apiService.registerUser(
            userRegistrationPayload = userRegistrationPayload
        )

    override suspend fun updateUserProfile(userProfileUpdatePayload: UserProfileUpdatePayload): Response<UserResponseBody> =
        apiService.updateUserProfile(
            userProfileUpdatePayload = userProfileUpdatePayload
        )

    override suspend fun updateUserProfileBackupData(userBackupDataUpdatePayload: UserBackupDataUpdatePayload): Response<UserResponseBody> =
        apiService.updateUserProfileBackupData(
            userBackupDataUpdatePayload = userBackupDataUpdatePayload
        )

    override suspend fun getUserByUserId(id: String): Response<UserResponseBody> =
        apiService.getUserByUserId(
            id = id
        )

    override suspend fun getUserByPhoneNumber(phoneNumber: String): Response<UserResponseBody> =
        apiService.getUserByPhoneNumber(
            phoneNumber = phoneNumber
        )

    override suspend fun lipa(paymentPayload: IntasendPaymentPayload): Response<IntasendPaymentResponseBody> =
        apiService.lipa(
            paymentPayload = paymentPayload
        )

    override suspend fun lipaStatus(paymentStatusPayload: IntasendPaymentStatusPayload): Response<IntasendPaymentStatusResponseBody> =
        apiService.lipaStatus(
            paymentStatusPayload = paymentStatusPayload
        )

    override suspend fun savePayment(paymentSavePayload: PaymentSavePayload): Response<PaymentSaveResponseBody> =
        apiService.savePayment(
            paymentSavePayload = paymentSavePayload
        )

    override suspend fun getUserPayments(userId: String): Response<PaymentsResponseBody> =
        apiService.getUserPayments(
            userId = userId
        )

    override suspend fun uploadFiles(files: List<MultipartBody.Part>): Response<FilesUploadResponseBody> =
        apiService.uploadFiles(
            files = files
        )

    override suspend fun getFile(fileName: String): Response<ResponseBody> =
        apiService.getFile(
            fileName = fileName
        )

}