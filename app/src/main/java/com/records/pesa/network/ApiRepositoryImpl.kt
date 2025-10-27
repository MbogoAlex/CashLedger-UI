package com.records.pesa.network

import com.records.pesa.models.FinancialMessagePayload
import com.records.pesa.models.FinancialMessagesResponseBody
import com.records.pesa.models.GeneralResponseBody
import com.records.pesa.models.backup.FilesUploadResponseBody
import com.records.pesa.models.payment.PaymentPayload
import com.records.pesa.models.payment.intasend.IntasendPaymentPayload
import com.records.pesa.models.payment.intasend.IntasendPaymentResponseBody
import com.records.pesa.models.payment.intasend.IntasendPaymentStatusPayload
import com.records.pesa.models.payment.intasend.IntasendPaymentStatusResponseBody
import com.records.pesa.models.payment.intasend.PaymentSavePayload
import com.records.pesa.models.payment.intasend.PaymentSaveResponseBody
import com.records.pesa.models.payment.intasend.PaymentsResponseBody
import com.records.pesa.models.subscription.SubscriptionContainerResponseBody
import com.records.pesa.models.subscription.SubscriptionTransactionResponseBody
import com.records.pesa.models.subscription.UserSubscriptionResponseBody
import com.records.pesa.models.user.TokenRefreshPayload
import com.records.pesa.models.user.UserProfileResponseBody
import com.records.pesa.models.user.login.AuthResponseBody
import com.records.pesa.models.user.login.UserLoginPayload
import com.records.pesa.models.user.registration.UserRegistrationPayload
import com.records.pesa.models.user.update.PasswordResetPayload
import com.records.pesa.models.user.update.UserBackupDataUpdatePayload
import com.records.pesa.models.user.update.UserProfileUpdatePayload
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response

class ApiRepositoryImpl(private val apiService: ApiService): ApiRepository {
    override suspend fun registerUser(userRegistrationPayload: UserRegistrationPayload): Response<AuthResponseBody> =
        apiService.registerUser(
            userRegistrationPayload = userRegistrationPayload
        )

    override suspend fun login(userLoginPayload: UserLoginPayload): Response<AuthResponseBody> =
        apiService.login(
            userLoginPayload = userLoginPayload
        )

    override suspend fun refreshToken(refreshToken: TokenRefreshPayload): Response<AuthResponseBody> =
        apiService.refreshToken(
            refreshToken = refreshToken
        )

    override suspend fun getMe(token: String): Response<UserProfileResponseBody> =
        apiService.getMe(
            token = "Bearer $token"
        )

    override suspend fun resetPassword(
        passwordResetPayload: PasswordResetPayload
    ): Response<GeneralResponseBody> =
        apiService.resetPassword(
            passwordResetPayload = passwordResetPayload
        )

    override suspend fun updateUserProfile(
        token: String,
        userProfileUpdatePayload: UserProfileUpdatePayload
    ): Response<GeneralResponseBody> =
        apiService.updateUserProfile(
            token = "Bearer $token",
            userProfileUpdatePayload = userProfileUpdatePayload
        )

    override suspend fun updateUserProfileBackupData(
        token: String,
        userBackupDataUpdatePayload: UserBackupDataUpdatePayload
    ): Response<UserProfileResponseBody> =
        apiService.updateUserProfileBackupData(
            token = "Bearer $token",
            userBackupDataUpdatePayload = userBackupDataUpdatePayload
        )

    override suspend fun getUserByUserId(id: String): Response<UserProfileResponseBody> =
        apiService.getUserByUserId(
            id = id
        )

    override suspend fun getUserByPhoneNumber(phoneNumber: String): Response<UserProfileResponseBody> =
        apiService.getUserByPhoneNumber(
            phoneNumber = phoneNumber
        )

    override suspend fun lipa(
        token: String,
        paymentPayload: PaymentPayload
    ): Response<SubscriptionTransactionResponseBody> =
        apiService.lipa(
            token = "Bearer $token",
            paymentPayload = paymentPayload
        )

    override suspend fun getTransaction(
        token: String,
        id: Long
    ): Response<SubscriptionTransactionResponseBody> =
        apiService.getTransaction(
            token = "Bearer $token",
            id = id
        )

    override suspend fun getSubscriptionPackageContainer(
        token: String,
        id: Int
    ): Response<SubscriptionContainerResponseBody> =
        apiService.getSubscriptionPackageContainer(
            token = "Bearer $token",
            id = id
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

    override suspend fun uploadFiles(
        token: String,
        files: List<MultipartBody.Part>
    ): Response<FilesUploadResponseBody> =
        apiService.uploadFiles(
            token = "Bearer $token",
            files = files
        )

    override suspend fun getFile(
        token: String,
        fileName: String
    ): Response<ResponseBody> =
        apiService.getFile(
            token = "Bearer $token",
            fileName = fileName
        )

    override suspend fun submitMessages(
        token: String,
        messages: List<FinancialMessagePayload>
    ): Response<FinancialMessagesResponseBody> =
        apiService.submitMessages(
            token = "Bearer $token",
            messages = messages
        )

    override suspend fun getUserSubscription(
        token: String,
        containerId: Int
    ): Response<UserSubscriptionResponseBody> =
        apiService.getUserSubscription(
            token = "Bearer $token",
            containerId = containerId
        )


}