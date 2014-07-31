package co.bandicoot.ztrader;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.service.polling.PollingAccountService;

public class CreateKeyActivity extends Activity {

  private static final int REQUEST_API_QR_CODE = 0;
  private static final int REQUEST_SECRET_QR_CODE = 1;

  Button cancelButton, encryptButton;
  ImageButton scanApiButton, scanSecretButton;
  EditText apiEditText, secretEditText, userIdEditText, passwordEditText;
  RelativeLayout secretLayout;
  int exchange;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_create_key);
    exchange = getIntent().getIntExtra("exchange", 0);

    getActionBar().setTitle(ExchangeService.getName(exchange));

    scanApiButton = (ImageButton) findViewById(R.id.scanApiButton);
    scanSecretButton = (ImageButton) findViewById(R.id.scanSecretButton);
    cancelButton = (Button) findViewById(R.id.cancelButton);
    encryptButton = (Button) findViewById(R.id.encryptButton);
    apiEditText = (EditText) findViewById(R.id.apiEditText);
    secretEditText = (EditText) findViewById(R.id.secretEditText);
    passwordEditText = (EditText) findViewById(R.id.passwordEditText);
    userIdEditText = (EditText) findViewById(R.id.userIdEditText);
    secretLayout = (RelativeLayout) findViewById(R.id.secretLayout);

    if (exchange == ExchangeService.BITSTAMP) {
      userIdEditText.setVisibility(View.VISIBLE);
    }
    else if (exchange == ExchangeService.JUSTCOIN) {
      secretLayout.setVisibility(View.GONE);
    }

    if (exchange == ExchangeService.CRYPTSY) {
      apiEditText.setHint(getText(R.string.public_key));
      secretEditText.setHint(getText(R.string.private_key));
    }
    else {
      apiEditText.setHint(getText(R.string.api_key));
      secretEditText.setHint(getText(R.string.secret));
    }

    TextWatcher textWatcher = new TextWatcher() {

      @Override
      public void afterTextChanged(Editable arg0) {

        int a = passwordEditText.getText().toString().length();
        int b = apiEditText.getText().toString().length();
        int c = secretEditText.getText().toString().length();
        if ((a > 7 || a == 0) && b > 0 && c > 0) {
          encryptButton.setEnabled(true);
        }
        else {
          encryptButton.setEnabled(false);
        }
      }

      @Override
      public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

      }

      @Override
      public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

      }

    };

    apiEditText.addTextChangedListener(textWatcher);
    passwordEditText.addTextChangedListener(textWatcher);
    secretEditText.addTextChangedListener(textWatcher);

  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_API_QR_CODE) {
      if (resultCode == RESULT_OK) {
        String contents = data.getStringExtra("SCAN_RESULT");
        if (contents != null) {
          apiEditText.setText(contents);
        }
      }
    }
    else if (requestCode == REQUEST_SECRET_QR_CODE) {
      if (resultCode == RESULT_OK) {
        String contents = data.getStringExtra("SCAN_RESULT");
        if (contents != null) {
          secretEditText.setText(contents);
        }
      }
    }

  }

  public void scanApiKey(View v) {

    if (isPackageInstalled("com.google.zxing.client.android")) {
      if (isCameraAvailable()) {
        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        intent.putExtra("SAVE_HISTORY", false);
        startActivityForResult(intent, REQUEST_API_QR_CODE);
      }
      else {
        Toast.makeText(CreateKeyActivity.this, getText(R.string.camera_unavailable), Toast.LENGTH_SHORT).show();
      }
    }
    else {
      showSendToMarketDialog();
    }
  }

  public void scanSecretKey(View v) {

    if (isPackageInstalled("com.google.zxing.client.android")) {
      if (isCameraAvailable()) {
        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        intent.putExtra("SAVE_HISTORY", false);
        startActivityForResult(intent, REQUEST_SECRET_QR_CODE);
      }
      else {
        Toast.makeText(CreateKeyActivity.this, getText(R.string.camera_unavailable), Toast.LENGTH_SHORT).show();
      }
    }
    else {
      showSendToMarketDialog();
    }
  }

  public void done(View v) {

    final ProgressDialog progressDialog = new ProgressDialog(this);
    progressDialog.setIndeterminate(true);
    progressDialog.setMessage(getText(R.string.testing));
    progressDialog.show();

    new Timer().schedule(new TimerTask() {

      @Override
      public void run() {

        Exchange ex = ExchangeService.getExchange(exchange);
        ExchangeSpecification spec = ex.getDefaultExchangeSpecification();
        spec.setApiKey(apiEditText.getText().toString().trim());

        if (secretLayout.getVisibility() == View.VISIBLE) {
          spec.setSecretKey(secretEditText.getText().toString().trim());
        }

        if (userIdEditText.getVisibility() == View.VISIBLE) {
          spec.setUserName(userIdEditText.getText().toString().trim());
        }

        ex.applySpecification(spec);

        PollingAccountService pas = ex.getPollingAccountService();

        try {
          pas.getAccountInfo();
          runOnUiThread(new Runnable() {

            @Override
            public void run() {

              if (progressDialog.isShowing()) {
                progressDialog.dismiss();
              }
              Toast.makeText(CreateKeyActivity.this, R.string.success, Toast.LENGTH_SHORT).show();
            }
          });
          try {

            Intent returnIntent = new Intent();
            returnIntent.putExtra("exchange", exchange);

            HashMap<Integer, ExchangeData> exchangeContainer = Utils.getExchangeContainer(getApplicationContext());

            if (passwordEditText.getText().toString().length() == 0) {
              exchangeContainer.get(exchange).setUnencryptedApi(apiEditText.getText().toString().trim());
              if (secretLayout.getVisibility() == View.VISIBLE) {
                exchangeContainer.get(exchange).setUnencryptedSecret(secretEditText.getText().toString().trim());
              }
              if (userIdEditText.getVisibility() == View.VISIBLE) {
                exchangeContainer.get(exchange).setUnencryptedUserId(userIdEditText.getText().toString());
              }
            }
            else {
              String encryptedApi =
                  EncodeDecodeAES.encrypt(passwordEditText.getText().toString(), apiEditText.getText().toString().trim());
              exchangeContainer.get(exchange).setEncryptedApi(encryptedApi);

              if (secretLayout.getVisibility() == View.VISIBLE) {
                String encryptedSecret =
                    EncodeDecodeAES.encrypt(passwordEditText.getText().toString(), secretEditText.getText().toString()
                        .trim());
                exchangeContainer.get(exchange).setEncryptedSecret(encryptedSecret);
              }

              if (userIdEditText.getVisibility() == View.VISIBLE) {
                String encryptedUserId =
                    EncodeDecodeAES.encrypt(passwordEditText.getText().toString(), userIdEditText.getText().toString());
                exchangeContainer.get(exchange).setEncryptedUserId(encryptedUserId);
              }
            }

            Utils.saveExchangeContainer(getApplicationContext(), exchangeContainer);

            setResult(RESULT_OK, returnIntent);
            finish();
          } catch (final Exception e) {
            runOnUiThread(new Runnable() {

              @Override
              public void run() {

                if (progressDialog.isShowing()) {
                  progressDialog.dismiss();
                }
                Toast.makeText(CreateKeyActivity.this,
                    getText(R.string.encryption_failed) + " " + e.getClass().getName() + ": " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
              }
            });

            e.printStackTrace();
          }
        } catch (final Exception e) {
          e.printStackTrace();
          runOnUiThread(new Runnable() {

            @Override
            public void run() {

              if (progressDialog.isShowing()) {
                progressDialog.dismiss();
              }
              LayoutInflater inflater = getLayoutInflater();
              View v = inflater.inflate(R.layout.dialog_api_error, null);
              new AlertDialog.Builder(CreateKeyActivity.this).setView(v).setTitle(R.string.error).setNegativeButton(android.R.string.ok, null).show();
            }
          });

        }
      }
    }, 0);

  }

  public void cancel(View v) {

    setResult(RESULT_CANCELED, null);
    finish();
  }

  private boolean isCameraAvailable() {

    PackageManager pm = getPackageManager();
    return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);

  }

  private boolean isPackageInstalled(String packagename) {

    PackageManager pm = getPackageManager();
    try {
      pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
      return true;
    } catch (NameNotFoundException e) {
      return false;
    }

  }

  private void showSendToMarketDialog() {

    new AlertDialog.Builder(this).setTitle(getText(R.string.scanner_required)).setMessage(getText(R.string.zxing_msg))
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

          @Override
          public void onClick(DialogInterface arg0, int arg1) {

            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
            startActivity(marketIntent);
          }
        }).setNegativeButton(android.R.string.no, null).create().show();

  }

}
