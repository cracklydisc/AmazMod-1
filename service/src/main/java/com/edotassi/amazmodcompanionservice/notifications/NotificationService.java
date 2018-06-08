package com.edotassi.amazmodcompanionservice.notifications;


import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.VectorDrawable;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import com.edotassi.amazmodcompanionservice.ui.NotificationActivity;
import com.edotassi.amazmodcompanionservice.ui.TextInputActivity;
import com.huami.watch.notification.data.Utils;

import amazmod.com.transport.data.NotificationData;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;

/**
 * Created by edoardotassinari on 20/04/18.
 */

public class NotificationService {

    private Context context;
    private Vibrator vibrator;
    private NotificationManager notificationManager;
    public static final String EXTRA_VOICE_REPLY = "extra_voice_reply";
    public static final String KEY_QUICK_REPLY_TEXT = "quick_reply";

    public NotificationService(Context context) {
        this.context = context;
        vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);

        notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
    }

    public void post(NotificationData notificationSpec) {
        postWithStandardUI(notificationSpec);
    }

    private void postWithCustomUI(NotificationData notificationSpec) {
        Intent intent = new Intent(context, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtras(notificationSpec.toBundle());

        context.startActivity(intent);
    }


    private void postWithStandardUI(NotificationData notificationSpec) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "")
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(new NotificationCompat.InboxStyle())
                .setContentText(notificationSpec.getText())
                .setContentTitle(notificationSpec.getTitle())
                .setVibrate(new long[]{notificationSpec.getVibration()});


        Log.d("Notifiche", "postWithStandardUI: " + notificationSpec.getKey() + " " + notificationSpec.getId() + " " + notificationSpec.getPkg());

      //  Utils.BitmapExtender bitmapExtender = Utils.retrieveAppIcon(context, notificationSpec.getPkg());
       // if (bitmapExtender != null) {
      /*  int[] iconData = notificationSpec.getIcon();
        int iconWidth = notificationSpec.getIconWidth();
        int iconHeight = notificationSpec.getIconHeight();
        Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(iconData, 0, iconWidth, 0, 0, iconWidth, iconHeight);

        builder.setLargeIcon(bitmap);*/
            //builder.setSmallIcon(Icon.createWithBitmap(bitmapExtender.bitmap));
            //data.mCanRecycleBitmap = bitmapExtender.canRecycle;
       //}
        String replyLabel = "Replay";
        String[] replyChoices = new String[] { "ok", "no", "forse domani"};

        RemoteInput remoteInput = new RemoteInput.Builder(KEY_QUICK_REPLY_TEXT)
                .setLabel(replyLabel)
                .setChoices(replyChoices)
                .build();

        Intent intent = new Intent(context, NotificationsReceiver.class);
        intent.setPackage(context.getPackageName());
        intent.setAction("com.amazmod.intent.notification.reply");
        intent.putExtra("reply", "hello world!");
        PendingIntent replyIntent = PendingIntent.getBroadcast(context, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_ONE_SHOT);



        Intent actionIntent = new Intent(context, TextInputActivity.class);
        PendingIntent actionPendingIntent =
                PendingIntent.getActivity(context, 0, actionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

// Create the action
        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(android.support.v4.R.drawable.notification_action_background,
                        "action", actionPendingIntent)
                        .build();

        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if(km.isKeyguardLocked()) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.ON_AFTER_RELEASE, "MyWakeLock");
            wakeLock.acquire();
            wakeLock.release();
        }

        NotificationCompat.Action installAction = new NotificationCompat.Action.Builder(android.R.drawable.ic_input_add, "Reply", replyIntent).build();
        builder.extend(new NotificationCompat.WearableExtender().addAction(installAction).addAction(action));
        Notification notification = builder.build();
        notificationManager.notify(notificationSpec.getId(), notification);
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Utils.BitmapExtender retrieveAppIcon(Context context, String pkgName) {
        if (context == null || pkgName == null) {
            return null;
        }
        Utils.BitmapExtender bitmapExtender = new Utils.BitmapExtender();
        Bitmap bitmap = null;
        try {
            Drawable iconDrawable = context.getPackageManager().getApplicationIcon(pkgName);
            if (iconDrawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) iconDrawable).getBitmap();
                bitmapExtender.canRecycle = false;
            } else if (iconDrawable instanceof VectorDrawable) {
                bitmap = drawableToBitmap(iconDrawable);
                bitmapExtender.canRecycle = true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoClassDefFoundError e2) {
            e2.printStackTrace();
        } catch (Exception e3) {
            e3.printStackTrace();
        }
        if (bitmap != null && bitmap.getWidth() > 96) {
            bitmap = Bitmap.createScaledBitmap(bitmap, 96, 96, false);
        }
        bitmapExtender.bitmap = bitmap;
        return bitmapExtender;
    }


    /*
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    public void add(TransportDataItem transportDataItem) {
        DataBundle dataBundle = transportDataItem.getData();
        StatusBarNotificationData statusBarNotificationData = dataBundle.getParcelable("data");

        if (statusBarNotificationData == null) {
            Log.d(Constants.TAG_NOTIFICATION_MANAGER, "statsBarNotificationData == null");
        } else {
            Log.d(Constants.TAG_NOTIFICATION_MANAGER, "statusBarNotificationData:");
            Log.d(Constants.TAG_NOTIFICATION_MANAGER, "pkg: " + statusBarNotificationData.pkg);
            Log.d(Constants.TAG_NOTIFICATION_MANAGER, "id: " + statusBarNotificationData.id);
            Log.d(Constants.TAG_NOTIFICATION_MANAGER, "groupKey: " + statusBarNotificationData.groupKey);
            Log.d(Constants.TAG_NOTIFICATION_MANAGER, "key: " + statusBarNotificationData.key);
            Log.d(Constants.TAG_NOTIFICATION_MANAGER, "tag: " + statusBarNotificationData.tag);
        }

        /*
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);

        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification_test);
        contentView.setTextViewText(R.id.notification_title, statusBarNotificationData.notification.title);
        contentView.setTextViewText(R.id.notification_text, statusBarNotificationData.notification.text);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, "")
                .setLargeIcon(statusBarNotificationData.notification.smallIcon)
                .setSmallIcon(xiaofei.library.hermes.R.drawable.abc_btn_check_material)
                .setContent(contentView)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.InboxStyle())
                .setContentText(statusBarNotificationData.notification.text)
                .setContentTitle(statusBarNotificationData.notification.title)
                .setVibrate(new long[]{500})
                .addAction(android.R.drawable.ic_dialog_info, "Demo", pendingIntent);

        vibrator.vibrate(500);

        notificationManager.notify(statusBarNotificationData.id, mBuilder.build());
        */



        /*
        Bitmap background = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.lau_notify_icon_upgrade_bg)).getBitmap();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "");

        builder.setContentTitle(title).setContentText(content);

        if (statusBarNotificationData.notification.smallIcon != null) {
            builder.setLargeIcon(statusBarNotificationData.notification.smallIcon);
        }

        builder.setSmallIcon(android.R.drawable.ic_dialog_email);


        Intent displayIntent = new Intent();
        displayIntent.setAction("com.saglia.notify.culo");
        displayIntent.setPackage("com.saglia.notify");
        displayIntent.putExtra("saglia_app", "Notifica");
        builder1.setContentIntent(PendingIntent.getBroadcast(context, 1, displayIntent, PendingIntent.FLAG_ONE_SHOT));

        NotificationData.ActionData[] actionDataList = statusBarNotificationData.notification.wearableExtras.actions;

        Intent intent = new Intent(context, NotificationsReceiver.class);
        intent.setPackage(context.getPackageName());
        intent.setAction("com.amazmod.intent.notification.reply");
        intent.putExtra("reply", "hello world!");
        intent.putExtra("id", statusBarNotificationData.id);
        PendingIntent installPendingIntent = PendingIntent.getBroadcast(context, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Action installAction = new NotificationCompat.Action.Builder(android.R.drawable.ic_input_add, "Reply", installPendingIntent).build();
        builder.extend(new NotificationCompat.WearableExtender().addAction(installAction));

        Notification notification1 = builder.build();
        notification1.flags = 32;

        notificationManager.notify(statusBarNotificationData.id, notification1);

    }
    */
}
