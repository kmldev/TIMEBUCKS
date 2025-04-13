package org.mintsoft.mintly.chatsupp;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.aghajari.emojiview.AXEmojiManager;
import com.aghajari.emojiview.AXEmojiTheme;
import com.aghajari.emojiview.view.AXEmojiEditText;
import com.aghajari.emojiview.view.AXEmojiPopupLayout;
import com.aghajari.emojiview.view.AXEmojiView;
import com.aghajari.emojiview.whatsappprovider.AXWhatsAppEmojiProvider;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.chatCall;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

public class Chat extends BaseAppCompat {
    private AXEmojiEditText editText;
    private boolean preload, block, activityRunning;
    private chAdapter adapter;
    private String lastId, mFileName;
    private Dialog loadingDiag;
    private long sizeKb = 2048;
    private static Handler handler, bH;
    private static Runnable reload, bR;
    private ImageView statusView, attachBtn;
    private ImageView emojiBtn;
    private AXEmojiPopupLayout emojiPopupLayout;
    private final ArrayList<String> msgs = new ArrayList<>();
    private final ArrayList<HashMap<String, Object>> attachment = new ArrayList<>();
    private ActivityResultLauncher<PickVisualMediaRequest> activityForResult;
    private RecordButton recordButton;
    private MediaRecorder mRecorder;
    private TextWatcher watcher;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setResult(11);
        loadingDiag = Misc.loadingDiag(this);
        loadingDiag.show();
        setContentView(R.layout.chat);
        TextView titleView = findViewById(R.id.chat_title);
        titleView.setText(DataParse.getStr(this, "chat_room", Home.spf));
        activityRunning = true;
        findViewById(R.id.chat_back).setOnClickListener(view -> finish());
        statusView = findViewById(R.id.chat_statusView);
        editText = findViewById(R.id.chat_inputView);
        editText.setHint(DataParse.getStr(this, "write_here", Home.spf));
        RecyclerView recyclerView = findViewById(R.id.chat_recyclerView);
        adapter = new chAdapter(this, recyclerView, new ArrayList<>(), editText);
        emojiBtn = findViewById(R.id.chat_emojiBtn);
        attachBtn = findViewById(R.id.chat_attachment);
        RecordView recordView = findViewById(R.id.chat_recordView);
        recordView.setRecordPermissionHandler(() -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
            boolean recordPermissionAvailable = ContextCompat.checkSelfPermission(Chat.this,
                    Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
            if (recordPermissionAvailable) return true;
            ActivityCompat.requestPermissions(Chat.this, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
            return false;

        });
        recordView.setOnRecordListener(new Orl() {
            @Override
            public void onStart() {
                emojiBtn.setVisibility(View.GONE);
                editText.setVisibility(View.GONE);
                attachBtn.setVisibility(View.GONE);
                startRecording();
            }

            @Override
            public void onCancel() {
                stopRecording(true);

            }

            @Override
            public void onFinish(long recordTime, boolean limitReached) {
                emojiBtn.setVisibility(View.VISIBLE);
                editText.setVisibility(View.VISIBLE);
                attachBtn.setVisibility(View.VISIBLE);
                if (limitReached) {
                    Toast.makeText(Chat.this, "Record time limit reached!", Toast.LENGTH_LONG).show();
                }
                stopRecording(false);
            }

            @Override
            public void onLessThanSecond() {
                emojiBtn.setVisibility(View.VISIBLE);
                editText.setVisibility(View.VISIBLE);
                attachBtn.setVisibility(View.VISIBLE);
                stopRecording(true);
            }
        });
        recordView.setOnBasketAnimationEndListener(() -> {
            emojiBtn.setVisibility(View.VISIBLE);
            editText.setVisibility(View.VISIBLE);
            attachBtn.setVisibility(View.VISIBLE);
        });
        recordView.setTimeLimit(30000);
        recordButton = findViewById(R.id.chat_sendBtn);
        recordButton.setRecordView(recordView);
        watcher = new TextWatcher() {
            boolean lock;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                int length = charSequence.length();
                if (!lock && length > 0) {
                    lock = true;
                    recordButton.setListenForRecord(false);
                    recordButton.setMicIcon(R.drawable.ic_send);
                } else if (length == 0) {
                    lock = false;
                    recordButton.setListenForRecord(true);
                    recordButton.setMicIcon(R.drawable.recv_ic_mic_white);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };
        editText.addTextChangedListener(watcher);
        recordButton.setOnRecordClickListener(v -> {
            if (!preload) {
                Toast.makeText(Chat.this, DataParse.getStr(this, "please_wait", Home.spf), Toast.LENGTH_LONG).show();
                return;
            }
            String msg = Objects.requireNonNull(editText.getText()).toString()
                    .replace("\u200B", " ").replace(" ", " ")
                    .replace(" ", " ").replace(" ", " ")
                    .replace(" ", " ").replace(" ", " ")
                    .replace(" ", " ").replace(" ", " ")
                    .replace(" ", " ").replace("⠀", " ");
            if (msg.replace(" ", "").isEmpty()) return;
            if (msgs.size() > 2) {
                Toast.makeText(Chat.this, DataParse.getStr(this, "please_wait", Home.spf), Toast.LENGTH_LONG).show();
                return;
            }
            editText.setText("");
            if (block || msgs.size() > 0) {
                msgs.add(msg);
                Toast.makeText(Chat.this, DataParse.getStr(this, "please_wait", Home.spf), Toast.LENGTH_LONG).show();
            } else {
                msgs.add(msg);
                postMessage();
            }
        });
        bH = new Handler();
        bR = new Runnable() {
            boolean gr;

            @Override
            public void run() {
                if (gr) {
                    gr = false;
                    statusView.setImageResource(R.drawable.chat_yellow);
                } else {
                    gr = true;
                    statusView.setImageResource(R.drawable.chat_green);
                }
                bH.postDelayed(this, 500);
            }
        };
        callNet();
        editText.setOnClickListener(view -> emojiClose());
        emojiPopupLayout = findViewById(R.id.chat_emoji_layout);
        emojiPopupLayout.setVisibility(View.GONE);
        AXEmojiManager.install(this, new AXWhatsAppEmojiProvider(this));
        new Handler().postDelayed(() -> runOnUiThread(() -> {
            AXEmojiTheme emojiTheme = AXEmojiManager.getEmojiViewTheme();
            emojiTheme.setSelectionColor(Color.TRANSPARENT);
            emojiTheme.setBackgroundColor(ContextCompat.getColor(Chat.this, R.color.colorPrimaryDark));
            emojiTheme.setCategoryColor(ContextCompat.getColor(Chat.this, R.color.colorPrimaryDark));
            AXEmojiManager.setEmojiViewTheme(emojiTheme);
            AXEmojiView emojiView = new AXEmojiView(Chat.this);
            emojiView.setEditText(editText);
            emojiPopupLayout.initPopupView(emojiView);
            emojiPopupLayout.dismiss();
            emojiBtn.setOnClickListener(view -> {
                if (emojiPopupLayout.isShowing()) {
                    emojiClose();
                } else {
                    emojiPopupLayout.setVisibility(View.VISIBLE);
                    emojiPopupLayout.show();
                    emojiBtn.setColorFilter(ContextCompat.getColor(Chat.this, R.color.yellow_2), PorterDuff.Mode.SRC_IN);
                }
            });
        }), 1500);
        activityForResult = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (getInfo(uri) && !block) postMessage();
        });
    }

    @Override
    public void onBackPressed() {
        if (emojiPopupLayout != null && emojiPopupLayout.isShowing()) {
            emojiClose();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityRunning = false;
        msgs.clear();
        if (handler != null) handler.removeCallbacks(reload);
        if (bH != null) bH.removeCallbacks(bR);
        adapter.playerRelease();
    }

    private void setReload() {
        handler = new Handler();
        reload = () -> {
            if (!block) {
                block = true;
                getMessage();
            }
            handler.postDelayed(reload, new Random().nextInt(10000 - 5000) + 5000);
        };
        handler.postDelayed(reload, new Random().nextInt(10000 - 5000) + 5000);
    }

    private void callNet() {
        block = true;
        setLoading(true);
        chatCall.readChat(this, new onResponse() {
            @Override
            public void onSuccessHashMap(HashMap<String, String> data) {
                if (!activityRunning) return;
                if (!Objects.requireNonNull(data.get("warn")).isEmpty())
                    showWarning(data.get("warn"));
                if (Objects.equals(data.get("attachment"), "1")) {
                    attachBtn.setVisibility(View.VISIBLE);
                    sizeKb = Long.parseLong(Objects.requireNonNull(data.get("attach_size")));
                    attachBtn.setOnClickListener(view -> Misc.showReminder(Chat.this, "chat", () -> {
                        if (msgs.size() > 2) {
                            Toast.makeText(Chat.this, DataParse.getStr(Chat.this, "please_wait", Home.spf), Toast.LENGTH_LONG).show();
                        } else {
                            activityForResult.launch(new PickVisualMediaRequest.Builder()
                                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                                    .build());
                        }
                    }));
                } else {
                    attachBtn.setVisibility(View.GONE);
                    editText.removeTextChangedListener(watcher);
                    recordButton.setListenForRecord(false);
                    recordButton.setMicIcon(R.drawable.ic_send);
                }
            }

            @Override
            public void onSuccessListHashMap(ArrayList<HashMap<String, String>> arrayList) {
                if (!activityRunning) return;
                addToList(arrayList);
                setReload();
                block = false;
                setLoading(false);
                preload = true;
                if (loadingDiag.isShowing()) loadingDiag.dismiss();
            }

            @Override
            public void onError(int i, String s) {
                if (!activityRunning) return;
                if (loadingDiag.isShowing()) loadingDiag.dismiss();
                block = false;
                setLoading(false);
                if (i == -1) {
                    Misc.showMessage(Chat.this, s, true);
                } else if (i == -2) {
                    Home.chatDisabled = true;
                    Toast.makeText(Chat.this, s, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(Chat.this, s, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
    }

    private void getMessage() {
        block = true;
        setLoading(true);
        chatCall.loadChat(this, lastId, new onResponse() {
            @Override
            public void onSuccessListHashMap(ArrayList<HashMap<String, String>> arrayList) {
                if (!activityRunning) return;
                addToList(arrayList);
                block = false;
                setLoading(false);
                if (msgs.size() > 0) postMessage();
            }

            @Override
            public void onError(int i, String s) {
                if (!activityRunning) return;
                block = false;
                setLoading(false);
            }
        });
    }

    private void postMessage() {
        if (msgs.size() == 0) return;
        block = true;
        setLoading(true);
        String msg = msgs.get(0);
        HashMap<String, Object> data = null;
        if (msg.equals("-@attach@-") && attachment.size() > 0) {
            data = new HashMap<>(attachment.get(0));
        }
        chatCall.postChat(this, lastId, msg, data, new onResponse() {
            @Override
            public void onSuccessListHashMap(ArrayList<HashMap<String, String>> arrayList) {
                if (!activityRunning) return;
                addToList(arrayList);
                block = false;
                setLoading(false);
                if (msgs.size() > 0) postMessage();
            }

            @Override
            public void onError(int i, String s) {
                if (!activityRunning) return;
                block = false;
                setLoading(false);
                if (i == -1) {
                    Misc.showMessage(Chat.this, s, false);
                } else if (i == -2) {
                    Misc.showMessage(Chat.this, s, false);
                    if (handler != null) handler.removeCallbacks(reload);
                } else {
                    Toast.makeText(Chat.this, s, Toast.LENGTH_LONG).show();
                }
            }
        });
        if (data != null) attachment.remove(0);
        msgs.remove(0);
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            bH.postDelayed(bR, 10);
        } else {
            bH.removeCallbacks(bR);
            statusView.setImageResource(R.drawable.chat_green);
        }
    }

    private void addToList(ArrayList<HashMap<String, String>> aL) {
        if (aL.size() > 0) {
            String lUid = adapter.getLastUid();
            for (int i = 0; i < aL.size(); i++) {
                if (Objects.equals(aL.get(i).get("uid"), lUid)) {
                    aL.get(i).put("avatar", "hide");
                }
                lUid = aL.get(i).get("uid");
            }
            lastId = aL.get(aL.size() - 1).get("id");
            adapter.addItems(aL);
        }
    }

    private void showWarning(String msg) {
        if (loadingDiag.isShowing()) loadingDiag.dismiss();
        final Dialog warnDiag = Misc.decoratedDiag(this, R.layout.dialog_warn, 0.7f);
        TextView titleView = warnDiag.findViewById(R.id.dialog_warn_titleView);
        titleView.setText(DataParse.getStr(this, "warning", Home.spf));
        TextView closeView = warnDiag.findViewById(R.id.dialog_warn_close);
        closeView.setText(DataParse.getStr(this, "ok", Home.spf));
        closeView.setOnClickListener(view -> warnDiag.dismiss());
        TextView cancelView = warnDiag.findViewById(R.id.dialog_warn_cancel);
        cancelView.setText(DataParse.getStr(this, "cancl", Home.spf));
        cancelView.setOnClickListener(view -> {
            warnDiag.dismiss();
            finish();
        });
        TextView warnView = warnDiag.findViewById(R.id.dialog_warn_textView);
        warnView.setText(Misc.html(msg));
        warnDiag.show();
    }

    private void emojiClose() {
        emojiPopupLayout.dismiss();
        emojiPopupLayout.setVisibility(View.GONE);
        emojiBtn.setColorFilter(ContextCompat.getColor(Chat.this, R.color.gray), PorterDuff.Mode.SRC_IN);
    }

    private void startRecording() {
        File dir = new File(getFilesDir(), "cdir");
        if (!dir.exists()) dir.mkdir();
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
        File file = new File(dir, attachment.size() + ".mp4");
        mFileName = file.getAbsolutePath();
        mRecorder.setOutputFile(mFileName);
        mRecorder.setOnErrorListener((mediaRecorder, i, i1) -> Toast.makeText(Chat.this,
                "Recording error: " + i + " and " + i1, Toast.LENGTH_LONG).show());
        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            Toast.makeText(this, "Preparing failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopRecording(boolean isCancelled) {
        if (mRecorder == null) return;
        try {
            mRecorder.stop();
            mRecorder.reset();
            mRecorder.release();
        } catch (RuntimeException ignored) {
        }
        mRecorder = null;
        if (mFileName == null) return;
        File file = new File(mFileName);
        if (isCancelled) {
            file.delete();
        } else {
            if (isValidFileSize(mFileName)) {
                String fileName = mFileName.substring(mFileName.lastIndexOf("/") + 1);
                HashMap<String, Object> data = new HashMap<>();
                data.put("name", fileName);
                data.put("type", "audio");
                data.put("file", mFileName);
                attachment.add(data);
                msgs.add("-@attach@-");
                if (!block) postMessage();
            }
        }
    }

    private boolean isValidFileSize(String filePath) {
        long maxFileSize = 1024 * sizeKb;
        File file = new File(filePath);
        long fileSize = file.length();
        if (fileSize > maxFileSize) {
            Toast.makeText(this, "File size is too big. Try with small size.", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
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
                    long maxFileSize = 1024 * sizeKb; // 1mb;
                    long fs = file.length();
                    if (fs > maxFileSize) {
                        Misc.showMessage(Chat.this, "File size is too big. Try with small size.", false);
                        return false;
                    } else {
                        HashMap<String, Object> data = new HashMap<>();
                        if (ft == null) return false;
                        if (ft.equals("image/jpeg") || ft.equals("image/jpg") || ft.equals("image/png") || ft.equals("image/gif")) {
                            data.put("type", "image");
                            data.put("file", BitmapFactory.decodeFile(file.getPath()));
                        } else if (ft.equals("audio/mp3") || ft.equals("audio/mp4")) {
                            data.put("type", "audio");
                            data.put("file", file.getPath());
                        } else {
                            Toast.makeText(Chat.this, DataParse.getStr(this, "unsupported_content", Home.spf), Toast.LENGTH_LONG).show();
                            return false;
                        }
                        data.put("name", fn);
                        attachment.add(data);
                        msgs.add("-@attach@-");
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
