package cf.playhi.freezeyou;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.os.UserHandle;

import static cf.playhi.freezeyou.utils.ToastUtils.showToast;

import androidx.core.app.NotificationCompat;

import com.catchingnow.delegatedscopesmanager.centerApp.CenterApp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import cf.playhi.freezeyou.utils.DevicePolicyManagerUtils;

public class DeviceAdminReceiver extends android.app.admin.DeviceAdminReceiver {
    private static ArrayList<Date> previousFailedAttempts = null;

    private static final int PASSWORD_FAILED_NOTIFICATION_ID = 102;
    private static final String PASSWORD_FAILED_NOTIFICATION_CHANNEL = "DeviceAdminWrongPasswordNotifications";

    @Override
    public void onEnabled(Context context, Intent intent) {
//        showToast(context, R.string.activated);
        CenterApp.getInstance(context).refreshState();
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return context.getString(R.string.disableConfirmation);
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        showToast(context, R.string.disabled);
    }

    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context.getApplicationContext(), DeviceAdminReceiver.class);
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent, UserHandle user) {
        if (!Process.myUserHandle().equals(user)) {
            // This password failure was on another user, for example a parent profile. Ignore it.
            return;
        }
        DevicePolicyManager devicePolicyManager = DevicePolicyManagerUtils.getDevicePolicyManager(context);
        /*
         * Post a notification to show:
         *  - how many wrong passwords have been entered
         */
        int attempts = devicePolicyManager.getCurrentFailedPasswordAttempts();

        String title = context.getResources().getQuantityString(
                R.plurals.password_failed_attempts_title, attempts, attempts);

        if (previousFailedAttempts == null) {
            previousFailedAttempts = new ArrayList<Date>();
        } else {
            if (previousFailedAttempts.size() > 30)
                previousFailedAttempts.clear();
        }
        Date date = new Date();
        previousFailedAttempts.add(date);
        Collections.sort(previousFailedAttempts, Collections.<Date>reverseOrder());

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel =
                new NotificationChannel(
                        PASSWORD_FAILED_NOTIFICATION_CHANNEL,
                        "Wrong unlock password entered",
                        NotificationManager.IMPORTANCE_LOW
                );
        notificationManager.createNotificationChannel(channel);
        NotificationCompat.Builder warn = new NotificationCompat.Builder(context, PASSWORD_FAILED_NOTIFICATION_CHANNEL);
        warn.setSmallIcon(R.drawable.ic_notification)
                .setTicker(title)
                .setContentTitle(title)
                .setContentIntent(PendingIntent.getActivity(context, -1,
                        new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD), PendingIntent.FLAG_IMMUTABLE));

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(title);

        final DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance();
        for(Date d : previousFailedAttempts) {
            inboxStyle.addLine(dateFormat.format(d));
        }
        warn.setStyle(inboxStyle);

        notificationManager.notify(PASSWORD_FAILED_NOTIFICATION_ID, warn.build());
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent, UserHandle user) {
        if (previousFailedAttempts != null && Process.myUserHandle().equals(user)) {
            previousFailedAttempts.clear();
        }
    }

}
