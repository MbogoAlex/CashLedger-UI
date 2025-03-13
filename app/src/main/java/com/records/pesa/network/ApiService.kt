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
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    @POST("user-account")
    suspend fun registerUser(@Body userRegistrationPayload: UserRegistrationPayload): Response<UserResponseBody>

    @PUT("user-profile-update")
    suspend fun updateUserProfile(@Body userProfileUpdatePayload: UserProfileUpdatePayload): Response<UserResponseBody>

    @PUT("user-backup-update")
    suspend fun updateUserProfileBackupData(@Body userBackupDataUpdatePayload: UserBackupDataUpdatePayload): Response<UserResponseBody>

    @GET("user/uid/{id}")
    suspend fun getUserByUserId(@Path("id") id: String): Response<UserResponseBody>

    @GET("user/phone/{phoneNumber}")
    suspend fun getUserByPhoneNumber(@Path("phoneNumber") phoneNumber: String): Response<UserResponseBody>

    @POST("lipa")
    suspend fun lipa(@Body paymentPayload: IntasendPaymentPayload): Response<IntasendPaymentResponseBody>

    @POST("lipa-status")
    suspend fun lipaStatus(@Body paymentStatusPayload: IntasendPaymentStatusPayload): Response<IntasendPaymentStatusResponseBody>

    @POST("lipa-save")
    suspend fun savePayment(@Body paymentSavePayload: PaymentSavePayload): Response<PaymentSaveResponseBody>

    @GET("payments/{userId}")
    suspend fun getUserPayments(@Path("userId") userId: String): Response<PaymentsResponseBody>

    @Multipart
    @POST("storage/files/upload")
    suspend fun uploadFiles(@Part files: List<MultipartBody.Part>): Response<FilesUploadResponseBody>

    @GET("storage/file/{fileName}")
    suspend fun getFile(
        @Path("fileName") fileName: String
    ): Response<ResponseBody>
}