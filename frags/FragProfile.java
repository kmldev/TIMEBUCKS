package org.mintsoft.mintly.frags;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.imageview.ShapeableImageView;
import com.squareup.picasso.Picasso;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetAuth;
import org.mintsoft.mintlib.GetURL;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.account.Login;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class FragProfile extends Fragment {
    private Context context;
    private Activity activity;
    private View v;
    private int type;
    private ShapeableImageView avatarView;
    private EditText codeInput, passInput, pass2Input, currInput;
    private LinearLayout codeHolder;
    private Dialog inputDiag, conDiag, passDiag, ynDiag;
    private ActivityResultLauncher<PickVisualMediaRequest> activityForResult;
    private TextView nameView, emailView, codeView, invitedByView, countryView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getContext();
        activity = getActivity();
        v = inflater.inflate(R.layout.frag_profile, container, false);
        if (context == null || activity == null) return v;
        TextView dataT = v.findViewById(R.id.frag_profile_dataT);
        dataT.setText(DataParse.getStr(context, "profile_data", Home.spf));
        TextView codeT = v.findViewById(R.id.frag_profile_codeT);
        codeT.setText(DataParse.getStr(context, "invitation_code", Home.spf));
        TextView invT = v.findViewById(R.id.frag_profile_invT);
        invT.setText(DataParse.getStr(context, "invited_by", Home.spf));
        avatarView = v.findViewById(R.id.frag_profile_avatarView);
        nameView = v.findViewById(R.id.frag_profile_nameView);
        countryView = v.findViewById(R.id.frag_profile_countryView);
        emailView = v.findViewById(R.id.frag_profile_emailView);
        codeView = v.findViewById(R.id.frag_profile_codeView);
        codeHolder = v.findViewById(R.id.frag_profile_codeHolder);
        invitedByView = v.findViewById(R.id.frag_profile_invitedByView);
        invitedByView.setText(DataParse.getStr(context, "invited_name", Home.spf));
        codeInput = v.findViewById(R.id.frag_profile_codeInput);
        codeInput.setHint(DataParse.getStr(context, "enter_invitation_code", Home.spf));
        passInput = v.findViewById(R.id.frag_profile_new_passInput);
        passInput.setHint(DataParse.getStr(context, "enter_new_pass", Home.spf));
        TextView emT = v.findViewById(R.id.frag_profile_emT);
        emT.setText(DataParse.getStr(context, "email_address", Home.spf));
        TextView ctryT = v.findViewById(R.id.frag_profile_ctryT);
        ctryT.setText(DataParse.getStr(context, "your_country", Home.spf));
        Button passBtn = v.findViewById(R.id.frag_profile_new_passBtn);
        passBtn.setText(DataParse.getStr(context, "change", Home.spf));
        passBtn.setOnClickListener(view -> {
            String nPass = passInput.getText().toString();
            if (nPass.isEmpty()) {
                Toast.makeText(context, DataParse.getStr(context, "enter_pass", Home.spf), Toast.LENGTH_LONG).show();
            } else if (nPass.length() < 8) {
                Toast.makeText(context, DataParse.getStr(context, "pass_min", Home.spf), Toast.LENGTH_LONG).show();
            } else if (nPass.length() > 20) {
                Toast.makeText(context, DataParse.getStr(context, "pass_max", Home.spf), Toast.LENGTH_LONG).show();
            } else {
                passDiag();
            }
        });
        activityForResult = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (getInfo(uri)) {
                Toast.makeText(context, DataParse.getStr(context, "please_wait", Home.spf), Toast.LENGTH_LONG).show();
            }
        });
        v.findViewById(R.id.frag_profile_avatarNew).setOnClickListener(view ->
                Misc.showReminder(activity, "avatar", () ->
                        activityForResult.launch(new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .build())));
        nameView.setOnClickListener(view -> {
            type = 2;
            inputDiag();
        });
        TextView delBtn = v.findViewById(R.id.frag_profile_go_delete);
        delBtn.setText(DataParse.getStr(context, "delete_acc", Home.spf));
        delBtn.setOnClickListener(view -> {
            if (ynDiag == null) {
                ynDiag = Misc.decoratedDiag(context, R.layout.dialog_quit, 0.8f);
                TextView titleV = ynDiag.findViewById(R.id.dialog_quit_title);
                titleV.setText(DataParse.getStr(context, "are_you_sure", Home.spf));
                TextView ynDesc = ynDiag.findViewById(R.id.dialog_quit_desc);
                ynDesc.setText(DataParse.getStr(context, "delete_acc_desc", Home.spf));
                ynDiag.findViewById(R.id.dialog_quit_no).setOnClickListener(vw -> ynDiag.dismiss());
                Button yBtn = ynDiag.findViewById(R.id.dialog_quit_yes);
                yBtn.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                yBtn.setTextColor(Color.WHITE);
                yBtn.setOnClickListener(vw -> {
                    ynDiag.dismiss();
                    Home.loadingDiag.show();
                    GetURL.info(context, "me/del", true, new onResponse() {
                        @Override
                        public void onSuccess(String s) {
                            Home.loadingDiag.dismiss();
                            Variables.reset();
                            GetAuth.removeCred(context);
                            startActivity(new Intent(context, Login.class));
                            activity.finish();
                        }

                        @Override
                        public void onError(int i, String s) {
                            Home.loadingDiag.dismiss();
                            Misc.showMessage(context, s, false);
                        }
                    });
                });
            }
            ynDiag.show();
        });
        TextView logout = v.findViewById(R.id.frag_profile_go_logout);
        logout.setText(DataParse.getStr(context, "logout", Home.spf));
        logout.setOnClickListener(view -> {
            GetAuth.removeCred(context);
            startActivity(new Intent(context, Login.class));
            activity.finish();
        });
        HashMap<String, String> data = Variables.getHashData("frag_profile");
        if (data == null) {
            callNet();
        } else {
            initData(data);
        }
        return v;
    }

    private void callNet() {
        if (!Home.loadingDiag.isShowing()) Home.loadingDiag.show();
        GetAuth.profile(context, new onResponse() {
            @Override
            public void onSuccessHashMap(HashMap<String, String> data) {
                Variables.setHashData("frag_profile", data);
                Home.loadingDiag.dismiss();
                initData(data);
            }

            @Override
            public void onError(int errorCode, String error) {
                Home.loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, context, () -> {
                        callNet();
                        conDiag.dismiss();
                    });
                } else {
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void initData(HashMap<String, String> data) {
        Picasso.get().load(data.get("avatar")).placeholder(R.drawable.anim_loading).error(R.drawable.avatar).into(avatarView);
        nameView.setText(data.get("name"));
        emailView.setText(data.get("email"));
        codeView.setText(data.get("code"));
        countryView.setText(data.get("cc"));
        codeView.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("Referral Link", codeView.getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, DataParse.getStr(context, "ref_code_copied", Home.spf), Toast.LENGTH_SHORT).show();
            }
        });
        String inv = data.get("inv");
        if (inv == null || inv.equals("-none-")) {
            Button codeBtn = v.findViewById(R.id.frag_profile_codeBtn);
            codeBtn.setText(DataParse.getStr(context, "submit", Home.spf));
            codeBtn.setOnClickListener(view -> {
                String d = codeInput.getText().toString();
                if (d.isEmpty()) return;
                type = 3;
                change(d);
            });
            invitedByView.setText("none");
        } else {
            invitedByView.setText(inv);
            codeHolder.setVisibility(View.GONE);
        }
    }

    private void inputDiag() {
        if (inputDiag == null) {
            inputDiag = Misc.decoratedDiag(context, R.layout.dialog_profile, 0.6f);
            inputDiag.setCancelable(true);
            inputDiag.setCanceledOnTouchOutside(true);
            TextView titleView = inputDiag.findViewById(R.id.dialog_profile_titleView);
            TextView inputView = inputDiag.findViewById(R.id.dialog_profile_inputView);
            if (type == 2) {
                titleView.setText(DataParse.getStr(context, "change_name", Home.spf));
                inputView.setText(DataParse.getStr(context, "enter_name", Home.spf));
            } else {
                titleView.setText(DataParse.getStr(context, "change_avatar", Home.spf));
                inputView.setText(DataParse.getStr(context, "enter_avatar_url", Home.spf));
            }
            EditText inputEdit = inputDiag.findViewById(R.id.dialog_profile_inputEdit);
            Button btn = inputDiag.findViewById(R.id.dialog_profile_btn);
            btn.setText(DataParse.getStr(context, "update", Home.spf));
            btn.setOnClickListener(view -> {
                String text = inputEdit.getText().toString();
                if (type == 2) {
                    if (text.isEmpty()) {
                        btn.setText(DataParse.getStr(context, "update", Home.spf));
                        Toast.makeText(context, "Enter your name", Toast.LENGTH_LONG).show();
                        return;
                    }
                    inputDiag.dismiss();
                    btn.setText(DataParse.getStr(context, "update", Home.spf));
                    change(text);
                }
            });
        } else {
            TextView titleView = inputDiag.findViewById(R.id.dialog_profile_titleView);
            TextView inputView = inputDiag.findViewById(R.id.dialog_profile_inputView);
            EditText e = inputDiag.findViewById(R.id.dialog_profile_inputEdit);
            e.setText("");
            if (type == 2) {
                titleView.setText(DataParse.getStr(context, "change_name", Home.spf));
                inputView.setText(DataParse.getStr(context, "enter_name", Home.spf));
            }
        }
        inputDiag.show();
    }

    private void change(String inputData) {
        if (!Home.loadingDiag.isShowing()) Home.loadingDiag.show();
        GetAuth.updateProfile(context, inputData, type, new onResponse() {
            @Override
            public void onSuccess(String response) {
                Variables.setHashData("frag_profile", null);
                Home.loadingDiag.dismiss();
                if (type == 2) {
                    nameView.setText(inputData);
                    Toast.makeText(context, DataParse.getStr(context, "ok", Home.spf), Toast.LENGTH_LONG).show();
                } else if (type == 3) {
                    codeHolder.setVisibility(View.GONE);
                    invitedByView.setText(response);
                    Toast.makeText(context, DataParse.getStr(context, "ok", Home.spf), Toast.LENGTH_LONG).show();
                } else if (type == 4) {
                    Variables.reset();
                    GetAuth.removeCred(context);
                    startActivity(new Intent(context, Login.class));
                    activity.finish();
                }
            }

            @Override
            public void onError(int errorCode, String error) {
                Home.loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, context, () -> {
                        conDiag.dismiss();
                        change(inputData);
                    });
                } else if (errorCode == -1) {
                    if (type == 3) {
                        codeInput.setText("");
                    } else if (type == 4) {
                        passInput.setText("");
                    }
                    Misc.showMessage(context, error, false);
                } else {
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show();
                    if (type == 3) {
                        codeInput.setText("");
                    } else if (type == 4) {
                        passInput.setText("");
                    }
                }
            }
        });
    }

    private void passDiag() {
        if (passDiag == null) {
            passDiag = Misc.decoratedDiag(context, R.layout.dialog_change_pass, 0.6f);
            passDiag.setCancelable(false);
            TextView titleView = passDiag.findViewById(R.id.dialog_change_pass_titleView);
            titleView.setText(DataParse.getStr(context, "enter_new_pass", Home.spf));
            TextView currT = passDiag.findViewById(R.id.dialog_change_pass_currT);
            currT.setText(DataParse.getStr(context, "enter_current_pass", Home.spf));
            TextView newT = passDiag.findViewById(R.id.dialog_change_pass_newT);
            newT.setText(DataParse.getStr(context, "enter_new_pass_confirm", Home.spf));
            pass2Input = passDiag.findViewById(R.id.dialog_change_pass_newEdit);
            currInput = passDiag.findViewById(R.id.dialog_change_pass_currEdit);
            Button cBtn = passDiag.findViewById(R.id.dialog_change_pass_cancel);
            cBtn.setText(DataParse.getStr(context, "cancl", Home.spf));
            cBtn.setOnClickListener(view -> passDiag.dismiss());
            Button uBtn = passDiag.findViewById(R.id.dialog_change_pass_update);
            uBtn.setText(DataParse.getStr(context, "change", Home.spf));
            uBtn.setOnClickListener(view -> {
                if (currInput.getText().toString().isEmpty()) return;
                passDiag.dismiss();
                if (passInput.getText().toString().equals(pass2Input.getText().toString())) {
                    type = 4;
                    change(currInput.getText().toString() + "||" + passInput.getText().toString());
                } else {
                    Misc.showMessage(context, DataParse.getStr(context, "pass_not_match", Home.spf), false);
                }
            });
        }
        pass2Input.setText("");
        currInput.setText("");
        passDiag.show();
    }

    private boolean getInfo(Uri uri) {
        if (uri == null) return false;
        try {
            String[] reqs = {MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.MIME_TYPE};
            ContentResolver cr = context.getContentResolver();
            Cursor cursor = cr.query(uri, reqs, null, null, null, null);
            if (cursor != null) {
                String fn = null;
                String ft = null;
                if (cursor.moveToFirst()) {
                    int fileTypeIndex = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE);
                    if (!cursor.isNull(fileTypeIndex)) {
                        ft = cursor.getString(fileTypeIndex);
                    }
                    int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (!cursor.isNull(displayNameIndex)) {
                        fn = cursor.getString(displayNameIndex);
                    }
                }
                if (fn != null) {
                    File file = new File(context.getFilesDir(), fn);
                    InputStream inStream = cr.openInputStream(uri);
                    OutputStream outStream = new FileOutputStream(file);
                    int read;
                    byte[] buffers = new byte[1024];
                    while ((read = inStream.read(buffers)) != -1) {
                        outStream.write(buffers, 0, read);
                    }
                    inStream.close();
                    outStream.close();
                    long maxFileSize = 1024 * 1024; // 1mb;
                    long fs = file.length();
                    if (fs > maxFileSize) {
                        Misc.showMessage(context, DataParse.getStr(context, "file_size_limit", Home.spf), false);
                        return false;
                    } else {
                        if (ft == null) return false;
                        if (ft.equals("image/jpeg") || ft.equals("image/jpg") || ft.equals("image/png") || ft.equals("image/gif")) {
                            Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
                            Bitmap bp = ThumbnailUtils.extractThumbnail(bitmap, 200, 200);
                            bitmap.recycle();
                            Home.loadingDiag.show();
                            GetAuth.uploadAvatar(context, fn, bp, new onResponse() {
                                @Override
                                public void onSuccess(String response) {
                                    Variables.setHashData("frag_profile", null);
                                    Picasso.get().load(response).placeholder(R.drawable.loading).error(R.drawable.avatar).into(avatarView);
                                    Picasso.get().load(response).error(R.drawable.avatar).into(((Home) activity).avatarView);
                                    Home.loadingDiag.dismiss();
                                }

                                @Override
                                public void onError(int errorCode, String error) {
                                    Home.loadingDiag.dismiss();
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show();
                                }
                            });
                            return true;
                        } else {
                            Misc.showMessage(context, DataParse.getStr(context, "unsupported_content", Home.spf), false);
                            return false;
                        }
                    }
                }
                cursor.close();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}