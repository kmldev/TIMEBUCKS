package org.mintsoft.mintly.account;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.LoggingBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetAuth;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.Tos;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Surf;
import org.mintsoft.mintly.helper.Variables;

import java.util.Arrays;

public class Login extends BaseAppCompat {
    public static SharedPreferences spf;
    private boolean isLoading;
    private TextView errorView, fErrorView;
    private static final int RC_SIGN_IN = 235;
    private CallbackManager callbackManager;
    private Dialog conDiag, loginDiag, fpassDiag, loadingDiag;
    private ActivityResultLauncher<Intent> activityForResult;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.login);
        spf = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        loadingDiag = Misc.loadingDiag(this);
        TextView textView = findViewById(R.id.login_logo_text);
        textView.setText(DataParse.getStr(this, "app_name", spf));
        Misc.setLogo(this, textView);
        TextView withTitle = findViewById(R.id.login_withTitle);
        withTitle.setText(DataParse.getStr(this, "login_with_email", spf));
        TextView regAcc = findViewById(R.id.login_regacc_title);
        regAcc.setText(DataParse.getStr(this, "register_account", spf));
        TextView tosTitle = findViewById(R.id.login_tos_title);
        tosTitle.setText(DataParse.getStr(this, "tos_link_1", spf));
        TextView tosBtn = findViewById(R.id.login_tos_btn);
        tosBtn.setText(DataParse.getStr(this, "tos_link_2", spf));
        tosBtn.setOnClickListener(v -> showTos());
        activityForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == 8) {
                        finish();
                    }
                });
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        View fbBtn = findViewById(R.id.login_fb_btn);
        if (getResources().getBoolean(R.bool.enable_facebook_login)) {
            fbBtn.setOnClickListener(v -> {
                if (!FacebookSdk.isInitialized()) {
                    FacebookSdk.sdkInitialize(getApplicationContext(), () -> {
                        if (!isFinishing() && !isDestroyed()) fbLogin();
                    });
                } else {
                    fbLogin();
                }
            });
        } else {
            fbBtn.setVisibility(View.GONE);
        }
        View googBtn = findViewById(R.id.login_goog_btn);
        if (getResources().getBoolean(R.bool.enable_google_login)) {
            googBtn.setOnClickListener(view -> gooLogin());
        } else {
            googBtn.setVisibility(View.GONE);
        }
        View phBtn = findViewById(R.id.login_ph_btn);
        if (getResources().getBoolean(R.bool.enable_phone_login)) {
            phBtn.setOnClickListener(view ->
                    activityForResult.launch(new Intent(Login.this, LoginPhone.class))
            );
        } else {
            phBtn.setVisibility(View.GONE);
        }
        findViewById(R.id.login_go_register).setOnClickListener(view ->
                activityForResult.launch(new Intent(Login.this, Register.class))
        );
        findViewById(R.id.login_go_login).setOnClickListener(view -> loginDiag());
    }

    @Override
    protected void onDestroy() {
        callbackManager = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            try {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) cLoginCall("g", account.getIdToken());
            } catch (ApiException e) {
                loadingDiag.dismiss();
                Toast.makeText(Login.this, DataParse.getStr(this, "login_error", spf) + ": " + e.getStatusCode(), Toast.LENGTH_LONG).show();
            }
        } else if (data != null && callbackManager != null) {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void fbLogin() {
        LoginManager.getInstance().logInWithReadPermissions(Login.this, Arrays.asList("email", "public_profile"));
        if (Variables.getPHash("debug").equals("1")) {
            FacebookSdk.setIsDebugEnabled(true);
            FacebookSdk.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);
        }
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        String fbToken = loginResult.getAccessToken().getToken();
                        if (fbToken != null) cLoginCall("f", fbToken);
                    }

                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Toast.makeText(Login.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void gooLogin() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.g_server_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(Login.this, gso);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void showTos() {
        startActivity(new Intent(this, Surf.class).putExtra("url", "https://" + getString(R.string.domain_name) + "/terms"));
    }

    private void cLoginCall(String type, String tok) {
        loadingDiag.show();
        GetAuth.socialLogin(Login.this, type, tok, spf, new onResponse() {
            @Override
            public void onSuccess(String response) {
                goHome(loadingDiag);
            }

            @Override
            public void onError(int errorCode, String error) {
                loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, Login.this, () -> {
                        conDiag.dismiss();
                        cLoginCall(type, tok);
                    });
                } else {
                    Toast.makeText(Login.this, error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void loginDiag() {
        if (loginDiag == null) {
            loginDiag = Misc.decoratedDiag(this, R.layout.dialog_login, 0.5f);
            Window w = loginDiag.getWindow();
            if (w != null) {
                w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            }
            TextView titleV = loginDiag.findViewById(R.id.dialog_login_title);
            titleV.setText(DataParse.getStr(this, "account_login", spf));
            TextView emailT = loginDiag.findViewById(R.id.dialog_login_emailT);
            emailT.setText(DataParse.getStr(this, "reg_email", spf));
            TextView passT = loginDiag.findViewById(R.id.dialog_login_passT);
            passT.setText(DataParse.getStr(this, "reg_pass", spf));
            EditText emailInput = loginDiag.findViewById(R.id.dialog_login_emailView);
            EditText passInput = loginDiag.findViewById(R.id.dialog_login_passView);
            errorView = loginDiag.findViewById(R.id.dialog_login_errorView);
            Button submitBtn = loginDiag.findViewById(R.id.dialog_login_submit);
            submitBtn.setText(DataParse.getStr(this, "login", spf));
            submitBtn.setOnClickListener(view -> {
                String em = emailInput.getText().toString();
                String pass = passInput.getText().toString();
                String check = validate(em, pass);
                if (check == null) {
                    isLoading = true;
                    loginDiag.setCancelable(false);
                    errorView.setVisibility(View.GONE);
                    submitBtn.setText(DataParse.getStr(this, "please_wait", spf));
                    GetAuth.login(Login.this, em, pass, spf, new onResponse() {
                        @Override
                        public void onSuccess(String response) {
                            goHome(loginDiag);
                        }

                        @Override
                        public void onError(int errorCode, String error) {
                            loginDiag.setCancelable(true);
                            if (errorCode == -9) {
                                loginDiag.dismiss();
                                conDiag = Misc.noConnection(conDiag, Login.this, () -> {
                                    conDiag.dismiss();
                                    isLoading = false;
                                    submitBtn.setText(DataParse.getStr(Login.this, "login", spf));
                                    errorView.setVisibility(View.GONE);
                                    loginDiag();
                                });
                            } else {
                                isLoading = false;
                                submitBtn.setText(DataParse.getStr(Login.this, "login", spf));
                                errorView.setText(error);
                                errorView.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                } else {
                    errorView.setVisibility(View.VISIBLE);
                    errorView.setText(check);
                }
            });
            TextView fPassV = loginDiag.findViewById(R.id.dialog_login_fpassView);
            fPassV.setText(DataParse.getStr(this, "forget_password_ask", spf));
            fPassV.setOnClickListener(view -> {
                if (!isLoading) {
                    errorView.setVisibility(View.GONE);
                    loginDiag.dismiss();
                    retrieveDiag();
                } else {
                    Toast.makeText(Login.this, DataParse.getStr(this, "please_wait", spf), Toast.LENGTH_LONG).show();
                }
            });
            Button cancelBtn = loginDiag.findViewById(R.id.dialog_login_cancel);
            cancelBtn.setText(DataParse.getStr(this, "cancl", spf));
            cancelBtn.setOnClickListener(view -> {
                if (!isLoading) {
                    errorView.setVisibility(View.GONE);
                    loginDiag.dismiss();
                }
            });
        }
        loginDiag.show();
    }

    private void retrieveDiag() {
        if (fpassDiag == null) {
            fpassDiag = Misc.decoratedDiag(this, R.layout.dialog_forget, 0.5f);
            Window w = fpassDiag.getWindow();
            if (w != null) {
                w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            }
            TextView retrieveT = fpassDiag.findViewById(R.id.dialog_retrieve_title);
            retrieveT.setText(DataParse.getStr(this, "retrieve_password", spf));
            EditText fpassInput = fpassDiag.findViewById(R.id.dialog_retrieve_emailView);
            fpassInput.setHint(DataParse.getStr(this, "email_address", spf));
            fErrorView = fpassDiag.findViewById(R.id.dialog_retrieve_errorView);
            Button fpassSubmit = fpassDiag.findViewById(R.id.dialog_retrieve_submit);
            fpassSubmit.setText(DataParse.getStr(this, "retrieve", spf));
            fpassSubmit.setOnClickListener(view -> {
                String em = fpassInput.getText().toString();
                String check = validate(em, "--------");
                if (check == null) {
                    isLoading = true;
                    fpassDiag.setCancelable(false);
                    fErrorView.setVisibility(View.GONE);
                    fpassSubmit.setText(DataParse.getStr(this, "please_wait", spf));
                    GetAuth.reset(Login.this, em, new onResponse() {
                        @Override
                        public void onSuccess(String response) {
                            fpassDiag.setCancelable(true);
                            fpassDiag.dismiss();
                            new AlertDialog.Builder(Login.this)
                                    .setMessage(response)
                                    .setCancelable(false)
                                    .setPositiveButton(DataParse.getStr(Login.this, "ok", spf), (dialog, id) -> {
                                        dialog.dismiss();
                                        finish();
                                    })
                                    .show();
                        }

                        @Override
                        public void onError(int errorCode, String error) {
                            fpassDiag.setCancelable(true);
                            if (errorCode == -9) {
                                fpassDiag.dismiss();
                                conDiag = Misc.noConnection(conDiag, Login.this, () -> {
                                    conDiag.dismiss();
                                    isLoading = false;
                                    fpassSubmit.setText(DataParse.getStr(Login.this, "retrieve", spf));
                                    fErrorView.setVisibility(View.GONE);
                                    retrieveDiag();
                                });
                            } else {
                                isLoading = false;
                                fpassSubmit.setText(DataParse.getStr(Login.this, "retrieve", spf));
                                fErrorView.setText(error);
                                fErrorView.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                } else {
                    fErrorView.setText(check);
                    fErrorView.setVisibility(View.VISIBLE);
                }
            });
            Button cBtn = fpassDiag.findViewById(R.id.dialog_retrieve_cancel);
            cBtn.setText(DataParse.getStr(this, "cancl", spf));
            cBtn.setOnClickListener(view -> {
                if (!isLoading) {
                    fErrorView.setVisibility(View.GONE);
                    fpassDiag.dismiss();
                    loginDiag();
                }
            });
        }
        fpassDiag.show();
    }

    private String validate(String email, String password) {
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return DataParse.getStr(this, "invalid_email", spf);
        } else if (password.isEmpty()) {
            return DataParse.getStr(this, "enter_pass", spf);
        } else if (password.length() < 8) {
            return DataParse.getStr(this, "pass_min", spf);
        } else if (password.length() > 20) {
            return DataParse.getStr(this, "pass_max", spf);
        } else {
            return null;
        }
    }

    private void goHome(Dialog d) {
        if (spf.getBoolean("tos", false)) {
            startActivity(new Intent(this, Home.class));
        } else {
            startActivity(new Intent(this, Tos.class));
        }
        new Handler().postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            if (d != null && d.isShowing()) d.dismiss();
            finish();
        }, 2000);
    }
}