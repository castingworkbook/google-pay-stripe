package com.example.googlepaystripe;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.CardInfo;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.stripe.android.GooglePayConfig;
import com.stripe.android.model.Token;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class PayWithGoogleActivity extends AppCompatActivity {

    private PaymentsClient paymentsClient = null;
    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 53;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_with_google);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Test Google Pay + Stripe
        try {
            InitPaymentClient();
            isReadyToPay();

            //PaymentDataRequest paymentDataRequest = createPaymentDataRequest();

            // this should instead be hooked up to the “Buy” button’s click handler
            //payWithGoogle();

            String t = "";
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        //.setAction("Action", null).show();
                payWithGoogle();
            }
        });
    }

    private void InitPaymentClient(){
        this.paymentsClient = Wallet.getPaymentsClient(this,
                new Wallet.WalletOptions.Builder()
                        .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                        .build());
    }

    private void isReadyToPay() {
        final IsReadyToPayRequest request = IsReadyToPayRequest.newBuilder()
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                .build();
        paymentsClient.isReadyToPay(request).addOnCompleteListener(
                new OnCompleteListener<Boolean>() {
                    public void onComplete(Task<Boolean> task) {
                        try {
                            final boolean result =
                                    task.getResult(ApiException.class);
                            if (result == true) {
                                System.out.println("Google Pay READY");
                            } else {
                                System.out.println("Google Pay NOT ready to use");
                            }
                        } catch (ApiException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    @NonNull
    private PaymentDataRequest createPaymentDataRequest() {
        // error -> https://stripe.dev/stripe-android/com/stripe/android/GooglePayConfig.html#getTokenizationSpecification

        JSONObject cardPaymentMethod = null;
        try {
            JSONObject tokenizationSpec =
                    new GooglePayConfig("pk_test_kyxxSBjKbFizfgxlqA3lHABq")
                            .getTokenizationSpecification();
            cardPaymentMethod = new JSONObject()
                    .put("type", "CARD")
                    .put(
                            "parameters",
                            new JSONObject()
                                    .put("allowedAuthMethods", new JSONArray()
                                            .put("PAN_ONLY")
                                            .put("CRYPTOGRAM_3DS"))
                                    .put("allowedCardNetworks",
                                            new JSONArray()
                                                    .put("AMEX")
                                                    .put("DISCOVER")
                                                    .put("JCB")
                                                    .put("MASTERCARD")
                                                    .put("VISA"))

                                    // require billing address
                                    .put("billingAddressRequired", true)
                                    .put(
                                            "billingAddressParameters",
                                            new JSONObject()
                                                    // require full billing address
                                                    .put("format", "FULL")

                                                    // require phone number
                                                    .put("phoneNumberRequired", true)
                                    )
                    )
                    .put("tokenizationSpecification", tokenizationSpec);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // create PaymentDataRequest
        JSONObject paymentDataRequest = new JSONObject();
        try {
            paymentDataRequest.put("apiVersion", 2);
            paymentDataRequest.put("apiVersionMinor", 0);
            paymentDataRequest.put("allowedPaymentMethods",
                    new JSONArray()
                            .put(cardPaymentMethod));
            paymentDataRequest.put("transactionInfo",
                    new JSONObject()
                            .put("totalPrice", "0.01")
                            .put("totalPriceStatus", "FINAL")
                            .put("currencyCode", "USD")
            );
            paymentDataRequest.put("merchantInfo",
                    new JSONObject()
                            .put("merchantName", "Example Merchant")
                            .put("emailRequired", true) // require email address
            );
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return PaymentDataRequest.fromJson(paymentDataRequest.toString());
    }

    private void payWithGoogle() {
        try {
            PaymentDataRequest pmtDataReq = createPaymentDataRequest();
            AutoResolveHelper.resolveTask(
                    paymentsClient.loadPaymentData(pmtDataReq),
                    this,
                    LOAD_PAYMENT_DATA_REQUEST_CODE
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            switch (requestCode) {
                case LOAD_PAYMENT_DATA_REQUEST_CODE: {
                    switch (resultCode) {
                        case Activity.RESULT_OK: {
                            PaymentData paymentData = PaymentData.getFromIntent(data);
                            // You can get some data on the user's card, such as the
                            // brand and last 4 digits
                            CardInfo info = paymentData.getCardInfo();
                            // You can also pull the user address from the
                            // PaymentData object.
                            UserAddress address = paymentData.getShippingAddress();
                            // This is the raw JSON string version of your Stripe token.
                            String rawToken = paymentData.getPaymentMethodToken()
                                    .getToken();

                            // Now that you have a Stripe token object,
                            // charge that by using the id
                            Token stripeToken = Token.fromString(rawToken);


                            if (stripeToken != null) {
                                /*
                                 * Nick Gaudreau - 2019-10-02
                                 * We need the chargeId, then with the actorClientSignUpId and
                                 * the CWB serviceID. We can call our Registration API...
                                 * /api/stripe/create-subscription
                                 * */

                                // This chargeToken function is a call to your own
                                // server, which should then connect to Stripe's
                                // API to finish the charge.
                                // chargeToken(stripeToken.getId());
                                String tokenId = stripeToken.getId();

                            }
                            break;
                        }
                        case Activity.RESULT_CANCELED: {
                            break;
                        }
                        case AutoResolveHelper.RESULT_ERROR: {
                            // Log the status for debugging
                            // Generally there is no need to show an error to
                            // the user as the Google Payment API will do that
                            final Status status =
                                    AutoResolveHelper.getStatusFromIntent(data);
                            break;
                        }
                        default: {
                            // Do nothing.
                        }
                    }
                    break;
                }
                default: {
                    // Handle any other startActivityForResult calls you may have made.
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
