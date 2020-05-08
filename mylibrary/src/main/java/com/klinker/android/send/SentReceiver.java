/*
 * Copyright 2013 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klinker.android.send;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;

import com.klinker.android.logger.Log;

public class SentReceiver extends StatusUpdatedReceiver {

    @Override
    public void updateInInternalDatabase(Context context, Intent intent, int resultCode) {
        Log.v("sent_receiver", "marking message as sent");
        final Uri uri = getUri(intent);
        if (uri != null) {
            long messageId = getMessageId(uri.toString());
            long time = getTime(context, uri);
            int type = 4;
            try {
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        try {
                            Log.v("sent_receiver", "using supplied uri");
                            ContentValues values = new ContentValues();
                            values.put("type", 2);
                            values.put("read", 1);
                            context.getContentResolver().update(uri, values, null, null);
                            type = 2;
                        } catch (NullPointerException e) {
                            markFirstAsSent(context);
                        }
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        if (uri != null) {
                            Log.v("sent_receiver", "using supplied uri");
                            ContentValues values = new ContentValues();
                            values.put("type", 5);
                            values.put("read", true);
                            values.put("error_code", resultCode);
                            context.getContentResolver().update(uri, values, null, null);
                        } else {
                            Log.v("sent_receiver", "using first message");
                            Cursor query = context.getContentResolver().query(Uri.parse("content://sms/outbox"), null, null, null, null);

                            // mark message failed
                            if (query != null && query.moveToFirst()) {
                                String id = query.getString(query.getColumnIndex("_id"));
                                ContentValues values = new ContentValues();
                                values.put("type", 5);
                                values.put("read", 1);
                                values.put("error_code", resultCode);
                                context.getContentResolver().update(Uri.parse("content://sms/outbox"), values, "_id=" + id, null);

                                query.close();
                            }
                        }
                        type = 5;
                        BroadcastUtils.sendExplicitBroadcast(
                                context, new Intent(), Transaction.NOTIFY_SMS_FAILURE);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            BroadcastUtils.sendUpdateMessageStatus(context, messageId, type, time);
            BroadcastUtils.sendExplicitBroadcast(context, new Intent(), Transaction.REFRESH);
        }
    }

    private long getTime(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, new String[]{"date"}, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long d = cursor.getLong(0);
                if (d != 0) {
                    return d;
                }
            }
        }
        return 0;
    }

    @Override
    public void onMessageStatusUpdated(Context context, Intent intent, int receiverResultCode) {
    }

    private Uri getUri(Intent intent) {
        Uri uri;

        try {
            uri = Uri.parse(intent.getStringExtra("message_uri"));
            if (uri.equals("")) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }

        return uri;
    }

    private long getMessageId(String uri) {
        try {
            return Long.parseLong(uri.substring(uri.indexOf("/sms/") + "/sms/".length()));
        } catch (Exception ignore) {
            return 0;
        }
    }

    private void markFirstAsSent(Context context) {
        Log.v("sent_receiver", "using first message");
        Cursor query = context.getContentResolver().query(Uri.parse("content://sms/outbox"), null, null, null, null);

        // mark message as sent successfully
        if (query != null && query.moveToFirst()) {
            String id = query.getString(query.getColumnIndex("_id"));
            ContentValues values = new ContentValues();
            values.put("type", 2);
            values.put("read", 1);
            context.getContentResolver().update(Uri.parse("content://sms/outbox"), values, "_id=" + id, null);

            query.close();
        }
    }
}
