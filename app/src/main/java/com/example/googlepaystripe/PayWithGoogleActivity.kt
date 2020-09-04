package com.example.googlepaystripe

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.*
import com.google.android.gms.wallet.Wallet.WalletOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stripe.android.*
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.Token
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class PayWithGoogleActivity : AppCompatActivity() {


    private val stripe: Stripe by lazy {
        StripeFactory(this).create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_with_google)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Test Google Pay + Stripe
        try {
//            InitPaymentClient()
            PaymentConfiguration.init(this, Settings(this).publishableKey)

            isReadyToPay()

            //PaymentDataRequest paymentDataRequest = createPaymentDataRequest();

            // this should instead be hooked up to the “Buy” button’s click handler
            //payWithGoogle();
            val t = ""
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            //.setAction("Action", null).show();
            payWithGoogle()
        }
    }
    private val paymentsClient: PaymentsClient by lazy {
        Wallet.getPaymentsClient(
                this,
                Wallet.WalletOptions.Builder()
                        .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                        .build()
        )
    }
    private val googlePayJsonFactory: GooglePayJsonFactory by lazy {
        GooglePayJsonFactory(this)
    }


    /**
     * Check that Google Pay is available and ready
     */
    private fun isReadyToPay() {
        val request = IsReadyToPayRequest.fromJson(
                googlePayJsonFactory.createIsReadyToPayRequest().toString()
        )

        paymentsClient.isReadyToPay(request)
                .addOnCompleteListener { task ->
                    try {
                        if (task.isSuccessful) {
                            println("PayWithGoogleActivity.isReadyToPay")
                        } else {
                            println("PayWithGoogleActivity.unavailable")
//                            showSnackbar("Google Pay is unavailable")
                        }
                    } catch (exception: ApiException) {
                        Log.e("StripeExample", "Exception in isReadyToPay", exception)
//                        showSnackbar("Exception: ${exception.localizedMessage}")
                    }
                }
    }

    /**
     * Launch the Google Pay sheet
     */
    private fun payWithGoogle() {
        AutoResolveHelper.resolveTask(
                paymentsClient.loadPaymentData(
                        PaymentDataRequest.fromJson(
                                googlePayJsonFactory.createPaymentDataRequest(
                                        transactionInfo = GooglePayJsonFactory.TransactionInfo(
                                                currencyCode = "USD",
                                                totalPrice = 10,
                                                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final
                                        ),
                                        merchantInfo = GooglePayJsonFactory.MerchantInfo(
                                                merchantName = "Widget Store"
                                        ),
                                        shippingAddressParameters = GooglePayJsonFactory.ShippingAddressParameters(
                                                isRequired = true,
                                                allowedCountryCodes = setOf("US", "IN"),
                                                phoneNumberRequired = true
                                        ),
                                        billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(
                                                isRequired = true,
                                                format = GooglePayJsonFactory.BillingAddressParameters.Format.Full,
                                                isPhoneNumberRequired = true
                                        )
                                ).toString()
                        )
                ),
                this@PayWithGoogleActivity,
                LOAD_PAYMENT_DATA_REQUEST_CODE
        )
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    if (data != null) {
                        handleGooglePayResult(data)
                    }
                }
                Activity.RESULT_CANCELED -> {

                    println("PayWithGoogleActivity.onActivityResult : Canceled")
                }
                AutoResolveHelper.RESULT_ERROR -> {
                    val status = AutoResolveHelper.getStatusFromIntent(data)
                    val statusMessage = status?.statusMessage ?: "unknown"
                    Toast.makeText(
                            this@PayWithGoogleActivity,
                            "Got error: $statusMessage",
                            Toast.LENGTH_SHORT
                    ).show()
                }

                // Log the status for debugging
                // Generally there is no need to show an error to
                // the user as the Google Payment API will do that
                else -> {
                }
            }
        }
    }

    private fun handleGooglePayResult(data: Intent) {
        val paymentData = PaymentData.getFromIntent(data) ?: return
        val paymentDataJson = JSONObject(paymentData.toJson())


        val paymentMethodCreateParams =
                PaymentMethodCreateParams.createFromGooglePay(paymentDataJson)

        stripe.createPaymentMethod(
                paymentMethodCreateParams,
                callback = object : ApiResultCallback<PaymentMethod> {
                    override fun onSuccess(result: PaymentMethod) {
//                        showSnackbar("Created PaymentMethod ${result.id}")
                        println("Created PaymentMethod result: $result")
                        println("Created PaymentMethod id: ${result.id}")
                        println("Created PaymentMethod card: ${result.card}")
                        println("Created PaymentMethod ${result.card?.brand}")
                        println("Created PaymentMethod ${result.card?.last4}")
                        println("Created PaymentMethod ${result.card?.expiryMonth}")
                        println("Created PaymentMethod ${result.card?.country}")
                        println("Created PaymentMethod ${result.card?.expiryYear}")
                        println("Created PaymentMethod ${result.card?.funding}")

                    }

                    override fun onError(e: Exception) {
                        e.printStackTrace()
                        Log.e("StripeExample", "Exception while creating PaymentMethod", e)
                        println("Exception while creating PaymentMethod")
//                        showSnackbar("Exception while creating PaymentMethod")
                    }
                }
        )
    }

    private companion object {
        private const val LOAD_PAYMENT_DATA_REQUEST_CODE = 5000
    }

    internal class StripeFactory(
            private val context: Context,
            private val stripeAccountId: String? = null,
            private val enableLogging: Boolean = true
    ) {
        fun create(): Stripe {
            return Stripe(
                    context,
                    PaymentConfiguration.getInstance(context).publishableKey,
                    stripeAccountId,
                    enableLogging
            )
        }
    }
}