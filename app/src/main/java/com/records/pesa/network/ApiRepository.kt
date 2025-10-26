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
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface ApiRepository {

    suspend fun registerUser(
        userRegistrationPayload: UserRegistrationPayload
    ): Response<AuthResponseBody>

    suspend fun login(
        userLoginPayload: UserLoginPayload
    ): Response<AuthResponseBody>

    suspend fun refreshToken(
        refreshToken: TokenRefreshPayload
    ): Response<AuthResponseBody>

    suspend fun getMe(
        token: String
    ): Response<UserProfileResponseBody>

    suspend fun resetPassword(
        passwordResetPayload: PasswordResetPayload
    ): Response<GeneralResponseBody>

    suspend fun updateUserProfile(
        token: String,
        userProfileUpdatePayload: UserProfileUpdatePayload
    ): Response<UserProfileResponseBody>

    suspend fun updateUserProfileBackupData(
        token: String,
        userBackupDataUpdatePayload: UserBackupDataUpdatePayload
    ): Response<UserProfileResponseBody>

    suspend fun getUserByUserId(id: String): Response<UserProfileResponseBody>

    suspend fun getUserByPhoneNumber(phoneNumber: String): Response<UserProfileResponseBody>

    suspend fun lipa(
        token: String,
        paymentPayload: PaymentPayload
    ): Response<SubscriptionTransactionResponseBody>

    suspend fun getTransaction(
        token: String,
        id: Long
    ): Response<SubscriptionTransactionResponseBody>

    suspend fun getSubscriptionPackageContainer(
        token: String,
        id: Int
    ): Response<SubscriptionContainerResponseBody>

    suspend fun lipaStatus(paymentStatusPayload: IntasendPaymentStatusPayload): Response<IntasendPaymentStatusResponseBody>

    suspend fun savePayment(paymentSavePayload: PaymentSavePayload): Response<PaymentSaveResponseBody>

    suspend fun getUserPayments(userId: String): Response<PaymentsResponseBody>

    suspend fun uploadFiles(
        token: String,
        files: List<MultipartBody.Part>
    ): Response<FilesUploadResponseBody>

    suspend fun getFile(
        token: String,
        fileName: String
    ): Response<ResponseBody>

    suspend fun submitMessages(
        token: String,
        messages: List<FinancialMessagePayload>
    ): Response<FinancialMessagesResponseBody>

    suspend fun getUserSubscription(
        token: String,
        containerId: Int
    ): Response<UserSubscriptionResponseBody>
}

