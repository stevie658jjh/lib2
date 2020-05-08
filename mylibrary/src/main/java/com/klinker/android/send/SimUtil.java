package com.klinker.android.send;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SimUtil {
    public static String SMS_SENT = ".SMS_SENT";
    public static String SMS_DELIVERED = ".SMS_DELIVERED";
    public static final String SENT_SMS_BUNDLE = "com.klinker.android.send_message.SENT_SMS_BUNDLE";
    public static final String DELIVERED_SMS_BUNDLE = "com.klinker.android.send_message.DELIVERED_SMS_BUNDLE";

    public static boolean sendSMS(Context ctx, int simID, String toNum, String centerNum, String smsText, int messageId, Uri messageUri) {
        String name;
        try {
            // set up sent and delivered pending intents to be used with message request
            Intent sentIntent = new Intent(SMS_SENT);
                BroadcastUtils.addClassName(ctx, sentIntent, SMS_SENT);
            sentIntent.putExtra("message_uri", messageUri == null ? "" : messageUri.toString());
            PendingIntent sentPI = PendingIntent.getBroadcast(ctx, messageId, sentIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent deliveredIntent = new Intent(SMS_DELIVERED);
                BroadcastUtils.addClassName(ctx, deliveredIntent, SMS_DELIVERED);
            deliveredIntent.putExtra("message_uri", messageUri == null ? "" : messageUri.toString());
            PendingIntent deliveredPI = PendingIntent.getBroadcast(
                    ctx, messageId, deliveredIntent, PendingIntent.FLAG_UPDATE_CURRENT);


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                final ArrayList<Integer> simCardList = new ArrayList<>();
                SubscriptionManager subscriptionManager;
                subscriptionManager = SubscriptionManager.from(ctx);
                @SuppressLint("MissingPermission") final List<SubscriptionInfo> subscriptionInfoList = subscriptionManager
                        .getActiveSubscriptionInfoList();
                for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
                    int subscriptionId = subscriptionInfo.getSubscriptionId();
                    simCardList.add(subscriptionId);
                }
                int smsToSendFrom = simCardList.get(simID); //assign your desired sim to send sms, or user selected choice
                SmsManager.getSmsManagerForSubscriptionId(smsToSendFrom)
                        .sendTextMessage(toNum, null, smsText, sentPI, deliveredPI);
                return true;
            } else {
                if (simID == 0) {
                    name = "isms";
                    // for model : "Philips T939" name = "isms0"
                } else if (simID == 1) {
                    name = "isms2";
                } else {
                    throw new Exception("can not get service which for sim '" + simID + "', only 0,1 accepted as values");
                }
                Method method = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
                method.setAccessible(true);
                Object param = method.invoke(null, name);

                method = Class.forName("com.android.internal.telephony.ISms$Stub").getDeclaredMethod("asInterface", IBinder.class);
                method.setAccessible(true);
                Object stubObj = method.invoke(null, param);
                if (Build.VERSION.SDK_INT < 18) {
                    method = stubObj.getClass().getMethod("sendText", String.class, String.class, String.class, PendingIntent.class, PendingIntent.class);
                    method.invoke(stubObj, toNum, centerNum, smsText, sentPI, deliveredPI);
                } else {
                    method = stubObj.getClass().getMethod("sendText", String.class, String.class, String.class, String.class, PendingIntent.class, PendingIntent.class);
                    method.invoke(stubObj, ctx.getPackageName(), toNum, centerNum, smsText, sentPI, deliveredPI);
                }

                return true;
            }
        } catch (ClassNotFoundException e) {
            Log.e("apipas", "ClassNotFoundException:" + e.getMessage());
        } catch (NoSuchMethodException e) {
            Log.e("apipas", "NoSuchMethodException:" + e.getMessage());
        } catch (InvocationTargetException e) {
            Log.e("apipas", "InvocationTargetException:" + e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e("apipas", "IllegalAccessException:" + e.getMessage());
        } catch (Exception e) {
            Log.e("apipas", "Exception:" + e.getMessage());
        }
        return false;
    }


    public static boolean sendMultipartTextSMS(Context ctx, int simID, String toNum, String centerNum, ArrayList<String> smsTextlist) {
        String name;
        try {
            ArrayList<PendingIntent> sPI = new ArrayList<PendingIntent>();
            ArrayList<PendingIntent> dPI = new ArrayList<PendingIntent>();
            if (simID == 0) {
                name = "isms";
                // for model : "Philips T939" name = "isms0"
            } else if (simID == 1) {
                name = "isms2";
            } else {
                throw new Exception("can not get service which for sim '" + simID + "', only 0,1 accepted as values");
            }
            Method method = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
            method.setAccessible(true);
            Object param = method.invoke(null, name);

            method = Class.forName("com.android.internal.telephony.ISms$Stub").getDeclaredMethod("asInterface", IBinder.class);
            method.setAccessible(true);
            Object stubObj = method.invoke(null, param);
            if (Build.VERSION.SDK_INT < 18) {
                method = stubObj.getClass().getMethod("sendMultipartText", String.class, String.class, List.class, List.class, List.class);
                method.invoke(stubObj, toNum, centerNum, smsTextlist, sPI, dPI);
            } else {
                method = stubObj.getClass().getMethod("sendMultipartText", String.class, String.class, String.class, List.class, List.class, List.class);
                method.invoke(stubObj, ctx.getPackageName(), toNum, centerNum, smsTextlist, sPI, dPI);
            }
            return true;
        } catch (ClassNotFoundException e) {
            Log.e("apipas", "ClassNotFoundException:" + e.getMessage());
        } catch (NoSuchMethodException e) {
            Log.e("apipas", "NoSuchMethodException:" + e.getMessage());
        } catch (InvocationTargetException e) {
            Log.e("apipas", "InvocationTargetException:" + e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e("apipas", "IllegalAccessException:" + e.getMessage());
        } catch (Exception e) {
            Log.e("apipas", "Exception:" + e.getMessage());
        }
        return false;
    }


}