package com.records.pesa.ui.screens.payment

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.delay

object SubscriptionScreenDestination: AppNavigation {
    override val title: String = "Subscription screen"
    override val route: String = "subscription-screen"

}
@Composable
fun SubscriptionScreenComposable(
    navigateToHomeScreen: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: SubscriptionScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    if(uiState.loadingStatus == LoadingStatus.SUCCESS) {
        Toast.makeText(context, uiState.paymentMessage, Toast.LENGTH_SHORT).show()
        navigateToHomeScreen()
        viewModel.resetPaymentStatus()
    } else if(uiState.loadingStatus == LoadingStatus.FAIL) {
        Toast.makeText(context, uiState.paymentMessage, Toast.LENGTH_SHORT).show()
        viewModel.resetPaymentStatus()
    }

    var showPage by rememberSaveable {
        mutableStateOf(true)
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        SubscriptionScreen(
            showPage = showPage,
            url = uiState.redirectUrl,
            onCheckPaymentStatus = {
                Log.d("PAYMENT_STATUS", "CHECKING_PAYMENT_STATUS")
                viewModel.checkPaymentStatus()
            },
            onPageStarted = {
                showPage = false
            },
            onPageFinished = {
                showPage = true
            },
            navigateToPreviousScreen = navigateToPreviousScreen
        )
    }
}

@Composable
fun SubscriptionScreen(
    url: String,
    showPage: Boolean,
    onCheckPaymentStatus: () -> Unit,
    onPageStarted: () -> Unit,
    onPageFinished: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val isLoading = remember { mutableStateOf(false) }  // State to track loading

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = navigateToPreviousScreen) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Previous screen"
                )
            }
            Text(
                text = "App subscription",
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Don't exit this screen even when the screen is blank")
        Spacer(modifier = Modifier.height(10.dp))
        if (url.isNotEmpty()) {
            if (showPage) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            clearCache(true)
                            webViewClient = CustomWebViewClient(
                                onLoadResource = {
                                    if(isLoading.value) {
                                        isLoading.value = true
                                    }
                                    isLoading.value = true
                                },
                                checkPaymentStatus = {
//                                    onCheckPaymentStatus()
                                    isLoading.value = false
                                },
                                onPageStarted = {
//                                    onPageStarted()
                                    isLoading.value = true
                                },
                                onPageFinished = {
//                                    onPageFinished()
                                    isLoading.value = false
                                }
                            )
                        }
                    },
                    update = { webView ->
                        Log.d("LOADING_URL", url)
                        webView.settings.javaScriptEnabled = true
                        Log.d("WEBVIEW_DATA_PROGRESS", webView.progress.toString())

                        try {
                            webView.loadUrl(url)
                        } catch (e: Exception) {
                            Log.e("urlLoadException", e.toString())
                        }
                    }
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                CircularProgressIndicator()
            }
        }

        // Display loading indicator while loading
        if (isLoading.value) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

class CustomWebViewClient(
    private val checkPaymentStatus: () -> Unit,
    private val onPageStarted: () -> Unit,
    private val onPageFinished: () -> Unit,
    private val onLoadResource: () -> Unit,
) : WebViewClient() {
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Log.d("onPageStarted", "URL: $url")
        onPageStarted()
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        Log.d("onPageFinished", "URL: $url")
        onPageFinished()
    }

    override fun onLoadResource(view: WebView?, url: String?) {
        super.onLoadResource(view, url)
        Log.d("onLoadResource", "URL: $url")
        onLoadResource()
    }


    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        Log.d("shouldOverrideUrlLoading", "URL: ${request?.url}")

        return if (request?.url.toString().isNotEmpty() && !request?.url.toString().contains("github")) {
            Log.d("LOAD_URL", "Loading URL: ${request?.url}")
            view?.loadUrl(request?.url.toString())
            true
        } else {
            Log.d("LOAD_URL", "Intercepted URL: ${request?.url}")
            checkPaymentStatus()
            true
        }
    }
}





@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SubscriptionScreenPreview() {
    CashLedgerTheme {
        SubscriptionScreen(
            showPage = false,
            url = "",
            onCheckPaymentStatus = {},
            onPageStarted = {},
            onPageFinished = {},
            navigateToPreviousScreen = {}
        )
    }
}