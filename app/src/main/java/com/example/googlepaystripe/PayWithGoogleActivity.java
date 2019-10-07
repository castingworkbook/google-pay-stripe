package com.example.googlepaystripe;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.wallet.CardRequirements;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.model.Token;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;


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
        } catch (Exception e) {
            e.printStackTrace();
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                payWithGoogle();
            }
        });
    }

    private void InitPaymentClient() {
        paymentsClient = Wallet.getPaymentsClient(this, new Wallet.WalletOptions.Builder()
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

    private PaymentDataRequest createPaymentDataRequestNew() {


//        amount = amount.replace(".", "");

        PaymentDataRequest.Builder request =
                PaymentDataRequest.newBuilder()
                        .setTransactionInfo(
                                TransactionInfo.newBuilder()
                                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                                        .setTotalPrice("0.50")
                                        .setCurrencyCode("USD")
                                        .build())
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                        .setCardRequirements(
                                CardRequirements.newBuilder()
                                        .addAllowedCardNetworks(Arrays.asList(
                                                WalletConstants.CARD_NETWORK_AMEX,
                                                WalletConstants.CARD_NETWORK_DISCOVER,
                                                WalletConstants.CARD_NETWORK_VISA,
                                                WalletConstants.CARD_NETWORK_MASTERCARD))
                                        .build());

        request.setPaymentMethodTokenizationParameters(createTokenizationParametersNew());
        return request.build();
    }

    private PaymentMethodTokenizationParameters createTokenizationParametersNew() {
        return PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
                .addParameter("gateway", "stripe")
                .addParameter("stripe:publishableKey", "pk_test_kyxxSBjKbFizfgxlqA3lHABq")
                .addParameter("gatewayMerchantId", "16153203921301195529")
                .addParameter("stripe:version", "2019-09-09")
                .build();
    }

    private void payWithGoogle() {
        PaymentDataRequest request = createPaymentDataRequestNew();
        if (request != null) {
            AutoResolveHelper.resolveTask(
                    paymentsClient.loadPaymentData(request),
                    this,
                    LOAD_PAYMENT_DATA_REQUEST_CODE);
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

                                new AlertDialog.Builder(this).setTitle("Google Pay token is: ").setMessage(tokenId).setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();

                                    }
                                }).show();

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
