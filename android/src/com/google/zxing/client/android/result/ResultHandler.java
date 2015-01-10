/*
 * Copyright (C) 2008 ZXing authors
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

package com.google.zxing.client.android.result;

import com.google.zxing.Result;
import com.google.zxing.client.android.R;
import com.google.zxing.client.result.ParsedResult;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * A base class for the Android-specific barcode handlers. These allow the app to polymorphically
 * suggest the appropriate actions for each data type.
 * <p/>
 * This class also contains a bunch of utility methods to take common actions like opening a URL.
 * They could easily be moved into a helper object, but it can't be static because the Activity
 * instance is needed to launch an intent.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class ResultHandler {

    private static final String TAG = ResultHandler.class.getSimpleName();

    private final ParsedResult result;
    private final Activity activity;

    ResultHandler(Activity activity, ParsedResult result) {
        this(activity, result, null);
    }

    ResultHandler(Activity activity, ParsedResult result, Result rawResult) {
        this.result = result;
        this.activity = activity;
    }

    public final ParsedResult getResult() {
        return result;
    }

    final Activity getActivity() {
        return activity;
    }

    public Integer getDefaultButtonID() {
        return null;
    }

    /**
     * Some barcode contents are considered secure, and should not be saved to history, copied to
     * the clipboard, or otherwise persisted.
     *
     * @return If true, do not create any permanent record of these contents.
     */
    public boolean areContentsSecure() {
        return false;
    }

    /**
     * Create a possibly styled string for the contents of the current barcode.
     *
     * @return The text to be displayed.
     */
    public CharSequence getDisplayContents() {
        String contents = result.getDisplayResult();
        return contents.replace("\r", "");
    }

    /**
     * A string describing the kind of barcode that was found, e.g. "Found contact info".
     *
     * @return The resource ID of the string.
     */
    public int getDisplayTitle() {
        return R.string.result_text;
    }

    final void shareByEmail(String contents) {
        sendEmail(null, null, null, null, contents);
    }

    final void sendEmail(String[] to, String[] cc, String[] bcc, String subject, String body) {
        Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
        if (to != null && to.length != 0) {
            intent.putExtra(Intent.EXTRA_EMAIL, to);
        }
        if (cc != null && cc.length != 0) {
            intent.putExtra(Intent.EXTRA_CC, cc);
        }
        if (bcc != null && bcc.length != 0) {
            intent.putExtra(Intent.EXTRA_BCC, bcc);
            putExtra(intent, Intent.EXTRA_SUBJECT, subject);
            putExtra(intent, Intent.EXTRA_TEXT, body);
            intent.setType("text/plain");
            launchIntent(intent);
        }
    }

    final void shareBySMS(String contents) {
        sendSMSFromUri("smsto:", contents);
    }

    final void sendSMS(String phoneNumber, String body) {
        sendSMSFromUri("smsto:" + phoneNumber, body);
    }

    final void sendSMSFromUri(String uri, String body) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uri));
        putExtra(intent, "sms_body", body);
        // Exit the app once the SMS is sent
        intent.putExtra("compose_mode", true);
        launchIntent(intent);
    }

    final void sendMMS(String phoneNumber, String subject, String body) {
        sendMMSFromUri("mmsto:" + phoneNumber, subject, body);
    }

    final void sendMMSFromUri(String uri, String subject, String body) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uri));
        // The Messaging app needs to see a valid subject or else it will treat this an an SMS.
        if (subject == null || subject.isEmpty()) {
            putExtra(intent, "subject", activity.getString(R.string.msg_default_mms_subject));
        } else {
            putExtra(intent, "subject", subject);
        }
        putExtra(intent, "sms_body", body);
        intent.putExtra("compose_mode", true);
        launchIntent(intent);
    }

    final void dialPhone(String phoneNumber) {
        launchIntent(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber)));
    }

    final void dialPhoneFromUri(String uri) {
        launchIntent(new Intent(Intent.ACTION_DIAL, Uri.parse(uri)));
    }

    final void openMap(String geoURI) {
        launchIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(geoURI)));
    }

    final void openURL(String url) {
        // Strangely, some Android browsers don't seem to register to handle HTTP:// or HTTPS://.
        // Lower-case these as it should always be OK to lower-case these schemes.
        if (url.startsWith("HTTP://")) {
            url = "http" + url.substring(4);
        } else if (url.startsWith("HTTPS://")) {
            url = "https" + url.substring(5);
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            launchIntent(intent);
        } catch (ActivityNotFoundException ignored) {
            Log.w(TAG, "Nothing available to handle " + intent);
        }
    }

    final void webSearch(String query) {
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra("query", query);
        launchIntent(intent);
    }

    /**
     * Like {@link #launchIntent(Intent)} but will tell you if it is not handle-able
     * via {@link ActivityNotFoundException}.
     *
     * @throws ActivityNotFoundException
     */
    final void rawLaunchIntent(Intent intent) {
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            Log.d(TAG, "Launching intent: " + intent + " with extras: " + intent.getExtras());
            activity.startActivity(intent);
        }
    }

    /**
     * Like {@link #rawLaunchIntent(Intent)} but will show a user dialog if nothing is available to handle.
     */
    final void launchIntent(Intent intent) {
        try {
            rawLaunchIntent(intent);
        } catch (ActivityNotFoundException ignored) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.app_name);
            builder.setMessage(R.string.msg_intent_failed);
            builder.setPositiveButton(R.string.button_ok, null);
            builder.show();
        }
    }

    private static void putExtra(Intent intent, String key, String value) {
        if (value != null && !value.isEmpty()) {
            intent.putExtra(key, value);
        }
    }
}
