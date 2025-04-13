package org.mintsoft.mintly.account;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetAuth;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.Tos;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class LoginPhone extends BaseAppCompat implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {
    private String ct, cc, pNumber, vId;
    private int greenColor, gryColor, progress = 1;
    private boolean btnActive, isActive;
    private Button submitBtn;
    private ImageView imageView;
    private EditText numberEdit;
    private SearchView searchView;
    private Dialog ccDiag, loadingDiag, conDiag;
    private TextView titleVw, descVw, ccVw;
    private FirebaseAuth firebaseAuth;
    private ctryAdapter adapter;
    private TextWatcher textWatcher;
    private static final String[] countryNames = {"Afghanistan", "Albania",
            "Algeria", "Andorra", "Angola", "Antarctica", "Argentina",
            "Armenia", "Aruba", "Australia", "Austria", "Azerbaijan",
            "Bahrain", "Bangladesh", "Belarus", "Belgium", "Belize", "Benin",
            "Bhutan", "Bolivia", "Bosnia And Herzegovina", "Botswana",
            "Brazil", "Brunei Darussalam", "Bulgaria", "Burkina Faso",
            "Myanmar", "Burundi", "Cambodia", "Cameroon", "Canada",
            "Cape Verde", "Central African Republic", "Chad", "Chile", "China",
            "Christmas Island", "Cocos (keeling) Islands", "Colombia",
            "Comoros", "Congo", "Cook Islands", "Costa Rica", "Croatia",
            "Cuba", "Cyprus", "Czech Republic", "Denmark", "Djibouti",
            "Timor-leste", "Ecuador", "Egypt", "El Salvador",
            "Equatorial Guinea", "Eritrea", "Estonia", "Ethiopia",
            "Falkland Islands (malvinas)", "Faroe Islands", "Fiji", "Finland",
            "France", "French Polynesia", "Gabon", "Gambia", "Georgia",
            "Germany", "Ghana", "Gibraltar", "Greece", "Greenland",
            "Guatemala", "Guinea", "Guinea-bissau", "Guyana", "Haiti",
            "Honduras", "Hong Kong", "Hungary", "India", "Indonesia", "Iran",
            "Iraq", "Ireland", "Isle Of Man", "Israel", "Italy", "Ivory Coast",
            "Jamaica", "Japan", "Jordan", "Kazakhstan", "Kenya", "Kiribati",
            "Kuwait", "Kyrgyzstan", "Laos", "Latvia", "Lebanon", "Lesotho",
            "Liberia", "Libya", "Liechtenstein", "Lithuania", "Luxembourg",
            "Macao", "Macedonia", "Madagascar", "Malawi", "Malaysia",
            "Maldives", "Mali", "Malta", "Marshall Islands", "Mauritania",
            "Mauritius", "Mayotte", "Mexico", "Micronesia", "Moldova",
            "Monaco", "Mongolia", "Montenegro", "Morocco", "Mozambique",
            "Namibia", "Nauru", "Nepal", "Netherlands", "New Caledonia",
            "New Zealand", "Nicaragua", "Niger", "Nigeria", "Niue", "Korea",
            "Norway", "Oman", "Pakistan", "Palau", "Panama",
            "Papua New Guinea", "Paraguay", "Peru", "Philippines", "Pitcairn",
            "Poland", "Portugal", "Puerto Rico", "Qatar", "Romania",
            "Russian Federation", "Rwanda", "Saint Barthélemy", "Samoa",
            "San Marino", "Sao Tome And Principe", "Saudi Arabia", "Senegal",
            "Serbia", "Seychelles", "Sierra Leone", "Singapore", "Slovakia",
            "Slovenia", "Solomon Islands", "Somalia", "South Africa",
            "Korea, Republic Of", "Spain", "Sri Lanka", "Saint Helena",
            "Saint Pierre And Miquelon", "Sudan", "Suriname", "Swaziland",
            "Sweden", "Switzerland", "Syrian Arab Republic", "Taiwan",
            "Tajikistan", "Tanzania", "Thailand", "Togo", "Tokelau", "Tonga",
            "Tunisia", "Turkey", "Turkmenistan", "Tuvalu",
            "United Arab Emirates", "Uganda", "United Kingdom", "Ukraine",
            "Uruguay", "United States", "Uzbekistan", "Vanuatu",
            "Holy See (vatican City State)", "Venezuela", "Viet Nam",
            "Wallis And Futuna", "Yemen", "Zambia", "Zimbabwe"};
    private static final String[] countryAreaCodes = {"93", "355", "213",
            "376", "244", "672", "54", "374", "297", "61", "43", "994", "973",
            "880", "375", "32", "501", "229", "975", "591", "387", "267", "55",
            "673", "359", "226", "95", "257", "855", "237", "1", "238", "236",
            "235", "56", "86", "61", "61", "57", "269", "242", "682", "506",
            "385", "53", "357", "420", "45", "253", "670", "593", "20", "503",
            "240", "291", "372", "251", "500", "298", "679", "358", "33",
            "689", "241", "220", "995", "49", "233", "350", "30", "299", "502",
            "224", "245", "592", "509", "504", "852", "36", "91", "62", "98",
            "964", "353", "44", "972", "39", "225", "1876", "81", "962", "7",
            "254", "686", "965", "996", "856", "371", "961", "266", "231",
            "218", "423", "370", "352", "853", "389", "261", "265", "60",
            "960", "223", "356", "692", "222", "230", "262", "52", "691",
            "373", "377", "976", "382", "212", "258", "264", "674", "977",
            "31", "687", "64", "505", "227", "234", "683", "850", "47", "968",
            "92", "680", "507", "675", "595", "51", "63", "870", "48", "351",
            "1", "974", "40", "7", "250", "590", "685", "378", "239", "966",
            "221", "381", "248", "232", "65", "421", "386", "677", "252", "27",
            "82", "34", "94", "290", "508", "249", "597", "268", "46", "41",
            "963", "886", "992", "255", "66", "228", "690", "676", "216", "90",
            "993", "688", "971", "256", "44", "380", "598", "1", "998", "678",
            "39", "58", "84", "681", "967", "260", "263"};
    private static final String[] countryCodes = {"AF", "AL", "DZ", "AD", "AO",
            "AQ", "AR", "AM", "AW", "AU", "AT", "AZ", "BH", "BD", "BY", "BE",
            "BZ", "BJ", "BT", "BO", "BA", "BW", "BR", "BN", "BG", "BF", "MM",
            "BI", "KH", "CM", "CA", "CV", "CF", "TD", "CL", "CN", "CX", "CC",
            "CO", "KM", "CG", "CK", "CR", "HR", "CU", "CY", "CZ", "DK", "DJ",
            "TL", "EC", "EG", "SV", "GQ", "ER", "EE", "ET", "FK", "FO", "FJ",
            "FI", "FR", "PF", "GA", "GM", "GE", "DE", "GH", "GI", "GR", "GL",
            "GT", "GN", "GW", "GY", "HT", "HN", "HK", "HU", "IN", "ID", "IR",
            "IQ", "IE", "IM", "IL", "IT", "CI", "JM", "JP", "JO", "KZ", "KE",
            "KI", "KW", "KG", "LA", "LV", "LB", "LS", "LR", "LY", "LI", "LT",
            "LU", "MO", "MK", "MG", "MW", "MY", "MV", "ML", "MT", "MH", "MR",
            "MU", "YT", "MX", "FM", "MD", "MC", "MN", "ME", "MA", "MZ", "NA",
            "NR", "NP", "NL", "NC", "NZ", "NI", "NE", "NG", "NU", "KP", "NO",
            "OM", "PK", "PW", "PA", "PG", "PY", "PE", "PH", "PN", "PL", "PT",
            "PR", "QA", "RO", "RU", "RW", "BL", "WS", "SM", "ST", "SA", "SN",
            "RS", "SC", "SL", "SG", "SK", "SI", "SB", "SO", "ZA", "KR", "ES",
            "LK", "SH", "PM", "SD", "SR", "SZ", "SE", "CH", "SY", "TW", "TJ",
            "TZ", "TH", "TG", "TK", "TO", "TN", "TR", "TM", "TV", "AE", "UG",
            "GB", "UA", "UY", "US", "UZ", "VU", "VA", "VE", "VN", "WF", "YE",
            "ZM", "ZW",
    };

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        isActive = getResources().getBoolean(R.bool.enable_phone_login);
        if (isActive) {
            setContentView(R.layout.login_phone);
            setResult(9);
            TextView titleView = findViewById(R.id.login_phone_titleView);
            titleView.setText(DataParse.getStr(this, "app_name", Login.spf));
            Misc.setLogo(this, titleView);
            submitBtn = findViewById(R.id.login_phone_submit);
            submitBtn.setText(DataParse.getStr(this, "send_code", Login.spf));
            imageView = findViewById(R.id.login_phone_img);
            numberEdit = findViewById(R.id.login_phone_number);
            titleVw = findViewById(R.id.login_phone_title);
            titleVw.setText(DataParse.getStr(this, "verify_your_number", Login.spf));
            descVw = findViewById(R.id.login_phone_desc);
            descVw.setText(DataParse.getStr(this, "verify_phone_number_desc", Login.spf));
            ccVw = findViewById(R.id.login_phone_cc);
            findViewById(R.id.login_phone_back).setOnClickListener(view -> finish());
            greenColor = ContextCompat.getColor(this, R.color.green_1);
            gryColor = ContextCompat.getColor(this, R.color.gray);
            numberEdit.setOnClickListener(view -> {
                if (cc == null) showCC();
            });
            ccVw.setOnClickListener(view -> showCC());
            textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    if (charSequence.length() > 7) {
                        submitBtn.getBackground().setColorFilter(greenColor, PorterDuff.Mode.SRC_ATOP);
                        submitBtn.setTextColor(Color.WHITE);
                        btnActive = true;
                    } else {
                        submitBtn.getBackground().setColorFilter(gryColor, PorterDuff.Mode.SRC_ATOP);
                        submitBtn.setTextColor(Color.BLACK);
                        btnActive = false;
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            };
            numberEdit.addTextChangedListener(textWatcher);
            loadingDiag = Misc.loadingDiag(this);
            firebaseAuth = FirebaseAuth.getInstance();
            submitBtn.setOnClickListener(view -> {
                if (btnActive) {
                    if (progress == 1) {
                        if (loadingDiag != null) loadingDiag.show();
                        pNumber = ct + numberEdit.getText().toString();
                        Login.spf.edit().putString("cc", cc).commit();
                        numberEdit.removeTextChangedListener(textWatcher);
                        phoneReg();
                    } else if (progress == 2) {
                        phoneVerify();
                    }
                }
            });
        } else {
            finish();
        }
    }

    @Override
    public boolean onClose() {
        if (isActive) adapter.filterData("");
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (isActive) adapter.filterData(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (isActive) adapter.filterData(newText);
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isActive && ct != null && ccVw != null) ccVw.setText(ct);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isActive) {
            if (loadingDiag != null && loadingDiag.isShowing()) loadingDiag.dismiss();
            if (ccDiag != null && ccDiag.isShowing()) ccDiag.dismiss();
        }
    }

    private void showCC() {
        if (ccDiag == null) {
            ccDiag = Misc.decoratedDiag(this, R.layout.dialog_cc, 0.5f);
            Window w = ccDiag.getWindow();
            if (w != null) {
                w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            }
            TextView titleT = ccDiag.findViewById(R.id.dialog_cc_title);
            titleT.setText(DataParse.getStr(this, "select_country", Home.spf));
            searchView = ccDiag.findViewById(R.id.dialog_cc_searchView);
            searchView.setQueryHint(DataParse.getStr(this, "search", Home.spf));
            ListView listView = ccDiag.findViewById(R.id.dialog_cc_listView);
            ccDiag.findViewById(R.id.dialog_cc_close).setOnClickListener(view -> ccDiag.dismiss());
            adapter = new ctryAdapter();
            listView.setAdapter(adapter);
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(false);
            searchView.setOnQueryTextListener(this);
            searchView.setOnCloseListener(this);
            listView.setOnItemClickListener((adapterView, view, i, l) -> {
                ct = "+" + adapter.cName.get(i).get("ct");
                cc = "+" + adapter.cName.get(i).get("cc");
                ccVw.setText(ct);
                ccDiag.dismiss();
            });
        } else {
            searchView.setQuery("", false);
            searchView.setIconified(true);
            adapter.filterData("");
        }
        ccDiag.show();
    }

    private void phoneReg() {
        btnActive = false;
        try {
            PhoneAuthProvider.verifyPhoneNumber(
                    PhoneAuthOptions.newBuilder(firebaseAuth)
                            .setPhoneNumber(pNumber)
                            .setTimeout(60L, TimeUnit.SECONDS)
                            .setActivity(this)
                            .setCallbacks(
                                    new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                        @Override
                                        public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                            btnActive = true;
                                            progress = 2;
                                            vId = verificationId;
                                            changeLayout();
                                            if (loadingDiag != null) loadingDiag.dismiss();
                                        }

                                        @Override
                                        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                            onVerifCompleted();
                                        }

                                        @Override
                                        public void onVerificationFailed(@NonNull FirebaseException e) {
                                            if (loadingDiag != null) loadingDiag.dismiss();
                                            Toast.makeText(LoginPhone.this, "" + e.getMessage(), Toast.LENGTH_LONG).show();
                                            btnActive = true;
                                        }
                                    })
                            .build());
        } catch (Exception e) {
            if (loadingDiag != null && loadingDiag.isShowing()) loadingDiag.dismiss();
            Toast.makeText(this, "Cannot verify the number at this time!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void phoneVerify() {
        String code = numberEdit.getText().toString();
        if (code.length() != 6) {
            Toast.makeText(this, DataParse.getStr(this, "invalid_verif_code", Login.spf), Toast.LENGTH_LONG).show();
            return;
        }
        if (loadingDiag != null) loadingDiag.show();
        btnActive = false;
        firebaseAuth.signInWithCredential(PhoneAuthProvider.getCredential(vId, code))
                .addOnCompleteListener(this, task -> {
                    if (loadingDiag != null) loadingDiag.dismiss();
                    if (task.isSuccessful()) {
                        onVerifCompleted();
                    } else {
                        Exception ex = task.getException();
                        if (ex instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(LoginPhone.this, DataParse.getStr(this, "invalid_verif_code", Login.spf), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginPhone.this, "" + Objects.requireNonNull(ex).getMessage(), Toast.LENGTH_LONG).show();
                        }
                        btnActive = true;
                    }
                });
    }

    private void onVerifCompleted() {
        if (loadingDiag != null && !loadingDiag.isShowing()) loadingDiag.show();
        GetAuth.pLogin(this, pNumber, Login.spf, new onResponse() {
            @Override
            public void onSuccess(String response) {
                setResult(8);
                if (loadingDiag != null) loadingDiag.dismiss();
                if (Login.spf.getBoolean("tos", false)) {
                    startActivity(new Intent(LoginPhone.this, Home.class));
                } else {
                    startActivity(new Intent(LoginPhone.this, Tos.class));
                }
                finish();
            }

            @Override
            public void onError(int errorCode, String error) {
                if (loadingDiag != null) loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, LoginPhone.this, () -> {
                        onVerifCompleted();
                        conDiag.dismiss();
                    });
                } else {
                    Toast.makeText(LoginPhone.this, error, Toast.LENGTH_LONG).show();
                }
            }
        });
        Login.spf.edit().putString("cc", cc).apply();
    }

    private void changeLayout() {
        numberEdit.removeTextChangedListener(textWatcher);
        ccVw.setVisibility(View.GONE);
        numberEdit.setText("");
        numberEdit.setGravity(Gravity.CENTER);
        submitBtn.setText(DataParse.getStr(this, "verify_code", Login.spf));
        imageView.setImageResource(R.drawable.phone_bg_2);
        titleVw.setText(DataParse.getStr(this, "verification_code", Login.spf));
        descVw.setText(Misc.html(
                DataParse.getStr(this, "verification_code_desc", Login.spf)
                        + " <font color='#1cc587'>" + pNumber + "</font>")
        );
    }

    private class ctryAdapter extends BaseAdapter {
        private final ArrayList<HashMap<String, String>> cName, original;

        ctryAdapter() {
            cName = new ArrayList<>();
            original = new ArrayList<>();
            HashMap<String, String> data;
            for (int i = 0; i < countryNames.length; i++) {
                data = new HashMap<>();
                data.put("c", countryNames[i]);
                data.put("ct", countryAreaCodes[i]);
                data.put("cc", countryCodes[i]);
                cName.add(data);
                original.add(data);
            }
        }

        @Override
        public int getCount() {
            return cName.size();
        }

        @Override
        public Object getItem(int i) {
            return cName.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.dialog_cc_list, viewGroup, false);
            }
            TextView nameView = view.findViewById(R.id.dialog_cc_list_name);
            nameView.setText(cName.get(i).get("c"));
            TextView numView = view.findViewById(R.id.dialog_cc_list_number);
            numView.setText(("+" + cName.get(i).get("ct")));
            return view;
        }

        public void filterData(String query) {
            query = query.toLowerCase();
            cName.clear();
            if (query.isEmpty()) {
                cName.addAll(original);
            } else {
                for (HashMap<String, String> hashMap : original) {
                    if (Objects.requireNonNull(hashMap.get("c")).toLowerCase().contains(query))
                        cName.add(hashMap);
                }
            }
            notifyDataSetChanged();
        }
    }
}