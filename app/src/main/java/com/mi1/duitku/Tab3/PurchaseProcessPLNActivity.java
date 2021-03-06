package com.mi1.duitku.Tab3;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.appsflyer.AFInAppEventParameterName;
import com.appsflyer.AFInAppEventType;
import com.appsflyer.AppsFlyerLib;
import com.mi1.duitku.BaseActivity;
import com.mi1.duitku.Common.AppGlobal;
import com.mi1.duitku.Common.Constant;
import com.mi1.duitku.LoginActivity;
import com.mi1.duitku.Main.MainActivity;
import com.mi1.duitku.R;
import com.mi1.duitku.Tab3.Common.CPaymentInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class PurchaseProcessPLNActivity extends BaseActivity {

    private EditText etAmount;
    private EditText etCustomer;
    private String amount;
    private String customer;
    private ProgressDialog progress;
    private final static String PRODUCT_CODE = "PLNPRAH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_process_pln);

        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.wait));
        progress.setCanceledOnTouchOutside(false);

        etAmount = (EditText) findViewById(R.id.edt_amount);
        etCustomer = (EditText) findViewById(R.id.edt_customer);

        Button btnPay = (Button) findViewById(R.id.btn_pay);
        btnPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validate()) {
                    new callInquiryBill().execute();
                }
            }
        });

    }

    public boolean validate() {

        amount = etAmount.getText().toString();
        customer = etCustomer.getText().toString();

        if (amount.isEmpty()) {
            etAmount.setError("please fill payment amount");
            etAmount.requestFocus();
            return false;
        }
        else if (Double.parseDouble(amount) < (double)10000) {
            etAmount.setError("Pembayaran minimum RP 10.000,00");
            etAmount.requestFocus();
            return false;
        }
        else if (customer.isEmpty()) {
            etCustomer.setError("Mohon masukkan nomor pelanggan");
            etCustomer.requestFocus();
            return false;
        }

        hideKeyboard();
        return true;
    }

    private void hideKeyboard(){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        ActionBar actionBar = getSupportActionBar();

        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.actionbar_bg));

        getMenuInflater().inflate(R.menu.menu_pay, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            PurchaseProcessPLNActivity.this.finish();

        } else if(id == R.id.action_payment) {
            if (validate()) {
                new callInquiryBill().execute();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public class callInquiryBill extends AsyncTask<String, Void, String> {

        CPaymentInfo paymentInfo = new CPaymentInfo();

        @Override
        protected void onPreExecute() {
            progress.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {

            String result = null;

            try {

                URL url = new URL(Constant.INQUIRY_BILL_PAGE);

                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("idpelanggan1", customer);
                    jsonObject.put("idpelanggan2", customer);
                    jsonObject.put("idpelanggan3", AppGlobal._userInfo.phoneNumber);
                    jsonObject.put("kodeProduk", PRODUCT_CODE);
                    jsonObject.put("token", AppGlobal._userInfo.token);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(50000); /* milliseconds */
                conn.setConnectTimeout(50000); /* milliseconds */
                conn.setUseCaches(false);
                conn.setRequestProperty("content-type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
                wr.write(jsonObject.toString());
                wr.flush();

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK){
                    InputStreamReader tmp = new InputStreamReader(conn.getInputStream(), "UTF-8");
                    BufferedReader reader = new BufferedReader(tmp);
                    StringBuilder builder = new StringBuilder();
                    String str;

                    while((str = reader.readLine()) != null){
                        builder.append(str+"\n");
                    }

                    result = builder.toString();
                } else {
                    result = String.valueOf(conn.getResponseCode());
                }

            } catch (MalformedURLException e){
                //Log.e("oasis", e.toString());
            } catch (IOException e) {
                //Log.e("oasis", e.toString());
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {

            progress.dismiss();

            if (result == null){
                showAlert("Proses gagal", "inquiry failed, please try again later.");
                return;
            } else if(result.equals("401")) {
                Toast.makeText(PurchaseProcessPLNActivity.this, "Sesi anda telah habis", Toast.LENGTH_SHORT).show();
                logout();
                return;
            }

            try {
                JSONObject obj = new JSONObject(result);
                paymentInfo.idPelanggan1 = obj.getString("idPelanggan1");
                paymentInfo.idPelanggan2 = obj.getString("idPelanggan2");
                paymentInfo.idPelanggan3 = obj.getString("idPelanggan3");
                if (obj.getString("biayaAdmin") != "null")
                    paymentInfo.biayaAdmin = Double.parseDouble(obj.getString("biayaAdmin"));
                if (obj.getString("nominal") != "null")
                    paymentInfo.nominal = Double.parseDouble(obj.getString("nominal"));
                paymentInfo.namaPelanggan = obj.getString("namaPelanggan");
                paymentInfo.namaOperator = obj.getString("namaOperator");
                paymentInfo.dayaPln = obj.getString("dayaPln");
                paymentInfo.ref1 = obj.getString("ref1");
                paymentInfo.ref2 = obj.getString("ref2");

            } catch (JSONException e) {
                Log.e("JSON Parser", "Error parsing data " + e.toString());
                return;
            }

            Intent intent = new Intent(PurchaseProcessPLNActivity.this, ConfirmationPaymentActivity.class);
            intent.putExtra(ConfirmationPaymentActivity.TAG_SUBSCRIBER, customer);
            paymentInfo.nominal = Double.parseDouble(amount);
            intent.putExtra(ConfirmationPaymentActivity.TAG_PAYMENT_PRODUCT, paymentInfo );
            intent.putExtra(ConfirmationPaymentActivity.TAG_ACTIVITYTITLE, "PLN PRABAYAR");
            intent.putExtra(ConfirmationPaymentActivity.TAG_PRODUCT_CODE, PRODUCT_CODE);

            Map<String, Object> eventValue = new HashMap<String, Object>();
            eventValue.put(AFInAppEventParameterName.PRICE,paymentInfo.nominal);
            eventValue.put(AFInAppEventParameterName.DESCRIPTION,paymentInfo);
            AppsFlyerLib.getInstance().trackEvent(PurchaseProcessPLNActivity.this, AFInAppEventType.SPENT_CREDIT,eventValue);

            startActivity(intent);
            PurchaseProcessPLNActivity.this.finish();
        }
    }

    private void showAlert(String paramString1, String paramString2)
    {
        new MaterialDialog.Builder(PurchaseProcessPLNActivity.this)
            .title(paramString1)
            .content(paramString2)
            .positiveText("OK")
            .positiveColorRes(R.color.colorPrimary)
            .show();

    }

    private void logout() {
        AppGlobal._userInfo = null;
        AppGlobal._userDetailInfo = null;
        Intent intent = new Intent(PurchaseProcessPLNActivity.this, LoginActivity.class);
        startActivity(intent);
        PurchaseProcessPLNActivity.this.finish();
        MainActivity._instance.finish();
    }
}
