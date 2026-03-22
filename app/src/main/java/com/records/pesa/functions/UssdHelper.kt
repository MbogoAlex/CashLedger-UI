package com.records.pesa.functions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * Dials the M-PESA USSD code (*334#)
 * Uses ACTION_CALL if permission is granted, otherwise falls back to ACTION_DIAL
 */
fun Context.dialUssd(ussdCode: String = "*334#") {
    try {
        val hasCallPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission) {
            // Direct call - opens USSD immediately
            Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${Uri.encode(ussdCode)}")
            }
        } else {
            // Fallback - opens dialer with code pre-filled
            Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${Uri.encode(ussdCode)}")
            }
        }

        startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(
            this,
            "Could not open USSD dialer. Please dial $ussdCode manually.",
            Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * Checks if CALL_PHONE permission is granted
 */
fun Context.hasCallPhonePermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CALL_PHONE
    ) == PackageManager.PERMISSION_GRANTED
}
