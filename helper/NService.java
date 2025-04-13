package org.mintsoft.mintly.helper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.preference.PreferenceManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.mintsoft.mintlib.GetAuth;
import org.mintsoft.mintlib.GetURL;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.Splash;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NService extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String s) {
        SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        try {
            if (spf.getInt("p_msgs", -1) == -1) {
                FirebaseMessaging.getInstance().subscribeToTopic("misc")
                        .addOnCompleteListener(task -> spf.edit().putInt("p_msgs", 1).apply());
            }
            if (GetAuth.cred(getApplicationContext()) != null) {
                GetURL.info(getApplicationContext(), "me/fid?f=" + s, true, new onResponse() {
                });
            }
        } catch (Exception ignored) {
        }
        spf.edit().putString("fid", s).apply();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (spf.getInt("p_msgs", 1) == 1) {
            if (Build.VERSION.SDK_INT >= 33 && ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                return;
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Map<String, String> data = remoteMessage.getData();
            if (notificationManager != null && data.size() > 0) {
                Set<String> strings = new HashSet<>();
                int requestCode = 1410;
                PendingIntent pendingIntent = null;
                String desc = data.get("desc");
                String link = firstUrl(desc);
                Intent intent;
                if (link == null) {
                    intent = new Intent(this, Splash.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    if (Build.VERSION.SDK_INT >= 23) {
                        pendingIntent = PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE);
                    } else {
                        pendingIntent = PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
                    }
                } else {
                    String fCc = null;
                    try {
                        Uri uri = Uri.parse(link);
                        fCc = uri.getQueryParameter("cc");
                    } catch (Exception ignored) {
                    }
                    String cc = spf.getString("cc", "us").toLowerCase();
                    if (fCc == null || fCc.toLowerCase().equals(cc)) {
                        intent = new Intent(this, PushSurf.class);
                        intent.putExtra("url", link
                                .replace("[app_country]", cc)
                                .replace("[app_uid]", GetAuth.user(this))
                                .replace("[app_gaid]", spf.getString("gid", "none"))
                        );
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        if (desc != null) {
                            if (desc.contains(link + " ")) {
                                desc = desc.replace(link + " ", "");
                            } else {
                                desc = desc.replace(link, "");
                            }
                        }
                        if (Build.VERSION.SDK_INT >= 23) {
                            pendingIntent = PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                        } else {
                            pendingIntent = PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
                        }
                    }
                }
                if (pendingIntent == null) return;
                Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                String channelId = getString(R.string.channel_id);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setAutoCancel(true)
                        .setSound(sound)
                        .setContentIntent(pendingIntent);
                String title = data.get("title");
                if (title != null && !title.isEmpty()) {
                    builder.setContentTitle(title);
                    strings.add("title###" + title);
                }
                if (desc != null && !desc.isEmpty()) {
                    builder.setContentText(desc);
                    strings.add("desc###" + desc);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(channelId, "Messaging", NotificationManager.IMPORTANCE_HIGH);
                    channel.setDescription("Handles push massages.");
                    notificationManager.createNotificationChannel(channel);
                    builder.setChannelId(channelId);
                }
                if (data.containsKey("image")) {
                    try {
                        String imgUrl = data.get("image");
                        strings.add("image###" + imgUrl);
                        URL url = new URL(imgUrl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(input);
                        if (data.containsKey("small")) {
                            strings.add("small###");
                            builder.setLargeIcon(bitmap);
                        } else {
                            builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap));
                        }
                    } catch (IOException ignored) {
                    }
                }
                notificationManager.notify(requestCode, builder.build());
                spf.edit().putStringSet("push_msg", strings).apply();
            }
        }
    }

    public static String firstUrl(String text) {
        if (!TextUtils.isEmpty(text)) {
            String[] split = text.split(" ");
            for (String ul : split) {
                if (ul.startsWith("http://") || ul.startsWith("https://")) return ul;
            }
        }
        return null;
    }
}