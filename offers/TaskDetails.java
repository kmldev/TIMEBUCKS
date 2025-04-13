package org.mintsoft.mintly.offers;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.Tasks;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.PushSurf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class TaskDetails extends BaseAppCompat {
    private String oid;
    private Dialog loadingDiag, conDiag, proofDiag;
    private TextView titleView, timeView, startBtn, nameText;
    private WebView webView;
    private EditText editText;
    private boolean reqProof, attached;
    private HashMap<String, Object> attachment;
    private ActivityResultLauncher<PickVisualMediaRequest> activityForResult;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (!Home.tasks) {
            finish();
            return;
        }
        oid = getIntent().getExtras().getString("id", null);
        if (oid == null) {
            finish();
            return;
        }
        loadingDiag = Misc.loadingDiag(this);
        loadingDiag.show();
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setNavigationBarColor(ContextCompat.getColor(this, R.color.gray));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        setContentView(R.layout.offers_task_details);
        titleView = findViewById(R.id.offers_task_details_titleView);
        timeView = findViewById(R.id.offers_task_details_timeView);
        startBtn = findViewById(R.id.offers_task_details_start);
        startBtn.setText(DataParse.getStr(this, "start_task", Home.spf));
        webView = findViewById(R.id.offers_task_details_webView);
        webView.getSettings().setJavaScriptEnabled(true);
        netCall();
        findViewById(R.id.offers_task_details_back).setOnClickListener(view -> finish());
        TextView prfBtn = findViewById(R.id.offers_task_details_proof);
        prfBtn.setText(DataParse.getStr(this, "submit_proof", Home.spf));
        prfBtn.setOnClickListener(view -> popupProof());
        activityForResult = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (getInfo(uri)) attached = true;
        });
    }

    private void netCall() {
        if (!loadingDiag.isShowing()) loadingDiag.show();
        Tasks.getInfo(this, oid, new onResponse() {
            @Override
            public void onSuccessHashMap(HashMap<String, String> data) {
                loadingDiag.dismiss();
                int status = Integer.parseInt(data.get("status"));
                if (status < 1) {
                    String stRes = data.get("staff");
                    if (stRes != null && !stRes.isEmpty()) {
                        Misc.showMessage(TaskDetails.this, stRes, false);
                    }
                    webView.loadData(data.get("desc"), "text/html", "UTF-8");
                    titleView.setText(data.get("title"));
                    timeView.setText(Misc.html(DataParse.getStr(TaskDetails.this, "proof_time", Home.spf).replace("AZA", "<b>" + data.get("time") + "</b>")));
                    startBtn.setOnClickListener(view -> visit());
                    reqProof = data.get("proof").equals("1");
                } else if (status == 1) {
                    Misc.showMessage(TaskDetails.this, DataParse.getStr(TaskDetails.this, "wait_for_approval", Home.spf), true);
                } else {
                    Toast.makeText(TaskDetails.this, DataParse.getStr(TaskDetails.this, "task_contact_support", Home.spf), Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onError(int errorCode, String error) {
                loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, TaskDetails.this, () -> {
                        netCall();
                        conDiag.dismiss();
                    });
                } else {
                    Toast.makeText(TaskDetails.this, error, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
    }

    private void visit() {
        if (!loadingDiag.isShowing()) loadingDiag.show();
        Tasks.visit(this, oid, new onResponse() {
            @Override
            public void onSuccess(String response) {
                loadingDiag.dismiss();
                Intent intent = new Intent(TaskDetails.this, PushSurf.class);
                intent.putExtra("url", response);
                intent.putExtra("only", true);
                startActivity(intent);
            }

            @Override
            public void onError(int errorCode, String error) {
                loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, TaskDetails.this, () -> {
                        netCall();
                        conDiag.dismiss();
                    });
                } else {
                    Toast.makeText(TaskDetails.this, error, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
    }

    private void popupProof() {
        if (proofDiag == null) {
            proofDiag = Misc.decoratedDiag(this, R.layout.dialog_proof, 0.8f);
            proofDiag.setCancelable(false);
            TextView spTv = proofDiag.findViewById(R.id.dialog_proof_submitProof);
            spTv.setText(DataParse.getStr(TaskDetails.this, "submit_proof", Home.spf));
            TextView msgTv = proofDiag.findViewById(R.id.dialog_proof_messageTitle);
            msgTv.setText(DataParse.getStr(TaskDetails.this, "message", Home.spf));
            editText = proofDiag.findViewById(R.id.dialog_proof_editText);
            TextView scrTitle = proofDiag.findViewById(R.id.dialog_proof_scrTitle);
            scrTitle.setText(DataParse.getStr(TaskDetails.this, "screenshot", Home.spf));
            LinearLayout scrHolder = proofDiag.findViewById(R.id.dialog_proof_scrHolder);
            if (reqProof) {
                nameText = proofDiag.findViewById(R.id.dialog_proof_fileName);
                TextView attTv = proofDiag.findViewById(R.id.dialog_proof_attach);
                attTv.setText(DataParse.getStr(TaskDetails.this, "browse", Home.spf));
                attTv.setOnClickListener(view ->
                        Misc.showReminder(TaskDetails.this, "task", () ->
                                activityForResult.launch(new PickVisualMediaRequest.Builder()
                                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                        .build())));
            } else {
                scrTitle.setVisibility(View.GONE);
                scrHolder.setVisibility(View.GONE);
            }
            Button submitBtn = proofDiag.findViewById(R.id.dialog_proof_submit);
            submitBtn.setText(DataParse.getStr(TaskDetails.this, "submit", Home.spf));
            submitBtn.setOnClickListener(view -> {
                String msg = editText.getText().toString();
                if (msg.length() > 10) {
                    if (reqProof && !attached) {
                        Toast.makeText(TaskDetails.this, DataParse.getStr(TaskDetails.this, "attach_scr", Home.spf), Toast.LENGTH_LONG).show();
                        return;
                    }
                    proofDiag.dismiss();
                    submitProof(msg);
                } else {
                    editText.setError(DataParse.getStr(TaskDetails.this, "msg_limit", Home.spf));
                }
            });
            proofDiag.findViewById(R.id.dialog_proof_close).setOnClickListener(view -> proofDiag.dismiss());
        }
        proofDiag.show();
    }

    private void submitProof(String msg) {
        if (!loadingDiag.isShowing()) loadingDiag.show();
        Tasks.postAttachment(this, oid, msg, attachment, new onResponse() {
            @Override
            public void onSuccess(String response) {
                TaskOffers.reload = oid;
                loadingDiag.dismiss();
                Misc.showMessage(TaskDetails.this, response, true);
            }

            @Override
            public void onError(int errorCode, String error) {
                loadingDiag.dismiss();
                loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, TaskDetails.this, () -> {
                        netCall();
                        conDiag.dismiss();
                    });
                } else {
                    Misc.showMessage(TaskDetails.this, error, false);
                }
            }
        });
    }

    private boolean getInfo(Uri uri) {
        if (uri == null) return false;
        try {
            String[] reqs = {MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.MIME_TYPE};
            ContentResolver cr = getContentResolver();
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
                    File file = new File(getFilesDir(), fn);
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
                        Misc.showMessage(TaskDetails.this, DataParse.getStr(TaskDetails.this, "file_size_limit", Home.spf), false);
                        return false;
                    } else {
                        attachment = new HashMap<>();
                        if (ft == null) return false;
                        if (ft.equals("image/jpeg") || ft.equals("image/jpg") || ft.equals("image/png") || ft.equals("image/gif")) {
                            attachment.put("file", BitmapFactory.decodeFile(file.getPath()));
                        } else {
                            Misc.showMessage(TaskDetails.this, DataParse.getStr(TaskDetails.this, "unsupported_content", Home.spf), false);
                            return false;
                        }
                        attachment.put("name", fn);
                        nameText.setText(fn);
                        return true;
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
