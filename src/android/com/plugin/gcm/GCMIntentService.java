package com.plugin.gcm;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.res.Resources;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Html;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

@SuppressLint("NewApi")
public class GCMIntentService extends GCMBaseIntentService {

	private static final String TAG = "GCMIntentService";

	public GCMIntentService() {
		super("GCMIntentService");
	}

	@Override
	public void onRegistered(Context context, String regId) {

		Log.v(TAG, "onRegistered: "+ regId);

		JSONObject json;

		try
		{
			json = new JSONObject().put("event", "registered");
			json.put("regid", regId);

			Log.v(TAG, "onRegistered: " + json.toString());

			// Send this JSON data to the JavaScript application above EVENT should be set to the msg type
			// In this case this is the registration ID
			PushPlugin.sendJavascript( json );

		}
		catch( JSONException e)
		{
			// No message to the user is sent, JSON failed
			Log.e(TAG, "onRegistered: JSON exception");
		}
	}

	@Override
	public void onUnregistered(Context context, String regId) {
		Log.d(TAG, "onUnregistered - regId: " + regId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.d(TAG, "onMessage - context: " + context);

		// Extract the payload from the message
		Bundle extras = intent.getExtras();
		if (extras != null)
		{
			// if we are in the foreground, just surface the payload, else post it to the statusbar
            if (PushPlugin.isInForeground()) {
				extras.putBoolean("foreground", true);
                PushPlugin.sendExtras(extras);
			}
			else {
				extras.putBoolean("foreground", false);

                // Send a notification if there is a message
                if (extras.getString("message") != null && extras.getString("message").length() != 0) {
                    createNotification(context, extras);
                }
            }
        }
	}

	public void createNotification(Context context, Bundle extras)
	{
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String appName = getAppName(this);

		Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("pushBundle", extras);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		int defaults = Notification.DEFAULT_ALL;

    Log.d(TAG, "Bundle: " + extras);

		if (extras.getString("defaults") != null) {
			try {
				defaults = Integer.parseInt(extras.getString("defaults"));
			} catch (NumberFormatException e) {}
		}

    Bitmap iconBitmap = BitmapFactory.decodeResource(getResources(), context.getApplicationInfo().icon);

		final Resources res = context.getResources();
		int smallIcon = context.getApplicationInfo().icon;

		// Get small icon and hardcoded large substitute
		final String iconName = extras.getString("icon");
		if (iconName != null) {
			smallIcon = res.getIdentifier(iconName, "drawable", context.getPackageName());
    }

		NotificationCompat.Builder mBuilder =
			new NotificationCompat.Builder(context)
				.setDefaults(defaults)
				.setSmallIcon(smallIcon)
        .setLargeIcon(iconBitmap)
				.setWhen(System.currentTimeMillis())
				.setContentTitle(Html.fromHtml(extras.getString("title")))
				.setTicker(Html.fromHtml(extras.getString("title")))
				.setContentIntent(contentIntent)
				.setAutoCancel(true);

    if (extras.getString("color") != null) {
      try {
        int color = Integer.parseInt(extras.getString("color"));
        mBuilder.setColor(color);
      } catch (NumberFormatException e) {}
    }

		String message = extras.getString("message");
    mBuilder.setContentText(Html.fromHtml(message));

		String bigView = extras.getString("bigview");
		if (bigView != null && bigView.equals("true")) {
			mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(Html.fromHtml(message)));
		}

    String linesStr = extras.getString("inboxLines");
    if (linesStr != null) {
      try {
        JSONArray array = new JSONArray(linesStr);
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle()
          .setBigContentTitle(Html.fromHtml(extras.getString("inboxTitle")))
          .setSummaryText(Html.fromHtml(extras.getString("inboxSummary")));

        for (int i = 0; i < array.length(); i++) {
          Log.d(TAG, array.getString(i));
          style.addLine(Html.fromHtml(array.getString(i)));
        }

        mBuilder.setStyle(style);
      } catch (JSONException e) {
        Log.e(TAG, "Error parsing JSON for inbox.", e);
      }
    }

		String msgcnt = extras.getString("msgcnt");
		if (msgcnt != null) {
			mBuilder.setNumber(Integer.parseInt(msgcnt));
		}

		int notId = 0;

		try {
			notId = Integer.parseInt(extras.getString("notId"));
		}
		catch(NumberFormatException e) {
			Log.e(TAG, "Number format exception - Error parsing Notification ID: " + e.getMessage());
		}
		catch(Exception e) {
			Log.e(TAG, "Number format exception - Error parsing Notification ID" + e.getMessage());
		}

		mNotificationManager.notify((String) appName, notId, mBuilder.build());
	}

	private static String getAppName(Context context)
	{
		CharSequence appName =
				context
					.getPackageManager()
					.getApplicationLabel(context.getApplicationInfo());

		return (String)appName;
	}

	@Override
	public void onError(Context context, String errorId) {
		Log.e(TAG, "onError - errorId: " + errorId);
	}

}
