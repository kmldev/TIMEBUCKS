package org.mintsoft.mintly.account;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetAuth;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.Tos;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Surf;

import java.util.Objects;

public class Register extends BaseAppCompat {
    private String rb, em, pass, name;
    private Dialog conDiag, loadingDiag;
    private TextInputEditText nameInput, emailInput, passInput1, passInput2, refInput;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.register);
        setResult(9);
        TextView titleView = findViewById(R.id.register_titleView);
        titleView.setText(DataParse.getStr(this, "app_name", Login.spf));
        Misc.setLogo(this, titleView);
        TextView header = findViewById(R.id.register_header);
        header.setText(DataParse.getStr(this, "register_account", Login.spf));
        TextInputLayout nameInputT = findViewById(R.id.register_nameInputT);
        nameInputT.setHint(DataParse.getStr(this, "reg_name", Login.spf));
        TextInputLayout emailInputT = findViewById(R.id.register_emailInputT);
        emailInputT.setHint(DataParse.getStr(this, "reg_email", Login.spf));
        TextInputLayout passInputT = findViewById(R.id.register_passInputT);
        passInputT.setHint(DataParse.getStr(this, "reg_pass", Login.spf));
        TextInputLayout passInputT2 = findViewById(R.id.register_passInputT2);
        passInputT2.setHint(DataParse.getStr(this, "reg_pass_confirm", Login.spf));
        TextInputLayout refInputT2 = findViewById(R.id.register_refInputT);
        refInputT2.setHint(DataParse.getStr(this, "reg_ref_code", Login.spf));
        TextView tosT = findViewById(R.id.register_tos_title);
        tosT.setText(DataParse.getStr(this, "tos_link_1", Login.spf));
        nameInput = findViewById(R.id.register_nameInput);
        emailInput = findViewById(R.id.register_emailInput);
        passInput1 = findViewById(R.id.register_passInput);
        passInput2 = findViewById(R.id.register_pass2Input);
        refInput = findViewById(R.id.register_refInput);
        loadingDiag = Misc.loadingDiag(this);
        rb = Login.spf.getString("rfb", null);
        if (rb != null) {
            refInput.setText(rb);
            refInput.setEnabled(false);
            refInput.setFocusable(false);
            passInput2.setImeOptions(EditorInfo.IME_ACTION_DONE);
        }
        Button submitBtn = findViewById(R.id.register_submit);
        submitBtn.setText(DataParse.getStr(this, "register", Login.spf));
        submitBtn.setOnClickListener(view -> {
            name = Objects.requireNonNull(nameInput.getText()).toString();
            em = Objects.requireNonNull(emailInput.getText()).toString();
            pass = Objects.requireNonNull(passInput1.getText()).toString();
            String regP2 = Objects.requireNonNull(passInput2.getText()).toString();
            if (!pass.equals(regP2)) {
                passInput2.setError(DataParse.getStr(this, "pass_not_match", Login.spf));
                return;
            }
            if (rb == null) {
                String rfb = Objects.requireNonNull(refInput.getText()).toString();
                if (rfb.length() > 0 && rfb.length() != 13) {
                    refInput.setError(DataParse.getStr(this, "invalid_ref_code", Login.spf));
                    return;
                } else if (rfb.length() == 13) {
                    rb = rfb;
                }
            }
            if (validate(em, pass)) return;
            register();
        });
        TextView tosBtn = findViewById(R.id.register_tos_btn);
        tosBtn.setText(DataParse.getStr(this, "tos_link_2", Login.spf));
        tosBtn.setOnClickListener(view ->
                startActivity(new Intent(this, Surf.class)
                        .putExtra("url", "https://" + getString(R.string.domain_name) + "/terms"))
        );
        findViewById(R.id.register_back).setOnClickListener(view -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (conDiag != null && conDiag.isShowing()) conDiag.dismiss();
        if (loadingDiag != null && loadingDiag.isShowing()) loadingDiag.dismiss();
    }

    private void register() {
        if (!loadingDiag.isShowing()) loadingDiag.show();
        GetAuth.register(this, name, em, pass, rb, Login.spf, new onResponse() {
            @Override
            public void onSuccess(String response) {
                setResult(8);
                if (loadingDiag.isShowing()) loadingDiag.dismiss();
                if (Login.spf.getBoolean("tos", false)) {
                    startActivity(new Intent(Register.this, Home.class));
                } else {
                    startActivity(new Intent(Register.this, Tos.class));
                }
                finish();
            }

            @Override
            public void onError(int errorCode, String error) {
                if (loadingDiag.isShowing()) loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, Register.this, () -> {
                        conDiag.dismiss();
                        register();
                    });
                } else Misc.showMessage(Register.this, error, errorCode == -2);
            }
        });
    }

    private boolean validate(String email, String password) {
        if (name.isEmpty()) {
            nameInput.setError(DataParse.getStr(this, "enter_name", Login.spf));
            return true;
        } else if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError(DataParse.getStr(this, "invalid_email", Login.spf));
            return true;
        } else if (password.isEmpty()) {
            passInput1.setError(DataParse.getStr(this, "enter_pass", Login.spf));
            return true;
        } else if (password.length() < 8) {
            passInput1.setError(DataParse.getStr(this, "pass_min", Login.spf));
            return true;
        } else if (password.length() > 20) {
            passInput1.setError(DataParse.getStr(this, "pass_max", Login.spf));
            return true;
        } else {
            return false;
        }
    }
}