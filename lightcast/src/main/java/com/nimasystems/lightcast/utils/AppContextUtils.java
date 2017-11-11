package com.nimasystems.lightcast.utils;

import android.Manifest.permission;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Point;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import java.io.File;
import java.util.List;

public class AppContextUtils {

    public static String getApplicationName(Context context) {

        PackageManager lPackageManager = context.getPackageManager();
        ApplicationInfo lApplicationInfo = null;

        try {
            lApplicationInfo = lPackageManager.getApplicationInfo(
                    context.getApplicationInfo().packageName, 0);
        } catch (final NameNotFoundException ignored) {
        }

        return (String) (lApplicationInfo != null ? lPackageManager
                .getApplicationLabel(lApplicationInfo) : "");
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    public static int getApplicationVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier(
                "status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static boolean getLocationServicesEnabled(Context context) {
        LocationManager service = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        return (service != null && (service.isProviderEnabled(LocationManager.GPS_PROVIDER) || service
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER)));
    }

    @SuppressLint("MissingPermission")
    public static Account getCurrentAccount(Context context) {
        Account[] accounts = null;

        try {
            PackageManager pm = context.getPackageManager();

            if (pm.checkPermission(permission.GET_ACCOUNTS,
                    context.getPackageName()) == PackageManager.PERMISSION_GRANTED) {
                AccountManager accountManager = AccountManager.get(context);
                //noinspection ResourceType
                accounts = accountManager.getAccountsByType("com.google");
            }
        } catch (Exception e) {
            Log.d("A", e.getMessage());
        }

        if (accounts == null || accounts.length < 1) {
            return null;
        }

        return accounts[0];
    }

    public static String getPackageName(Context context) {

        String userAgent;
        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        if (pInfo == null) {
            return null;
        }

        return pInfo.packageName;
    }

    public static String getUserAgentString(Context context, String appName) {

        String userAgent;
        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        if (pInfo == null) {
            return null;
        }

        // Dreams/[app_version] ([device brand/type/model]; [device os
        // version]; locale:[current_locale])
        userAgent = (!StringUtils.isNullOrEmpty(appName) ? appName : pInfo.packageName) + "/" + pInfo.versionName + " ( "
                + Build.MANUFACTURER + "/" + Build.MODEL + "; Android "
                + Build.VERSION.RELEASE + ", locale: "
                + I18n.getLocaleCode(context) + " )";

        return userAgent;
    }

    public static ComponentName isServiceStarted(Context context,
                                                 String className) {
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(android.content.Context.ACTIVITY_SERVICE);

        if (activityManager != null) {
            List<ActivityManager.RunningServiceInfo> serviceList = activityManager
                    .getRunningServices(Integer.MAX_VALUE);
            Log.w("", "className : " + className);

            if (!(serviceList.size() > 0)) {
                return null;
            }

            for (int i = 0; i < serviceList.size(); i++) {
                RunningServiceInfo serviceInfo = serviceList.get(i);
                ComponentName serviceName = serviceInfo.service;

                if (serviceName.getClassName().equals(className)) {
                    return serviceName;
                }
            }
        }

        return null;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint({"NewApi", "ObsoleteSdkInt"})
    public static Point getScreenSize(Context context) {
        Point size = new Point();
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);

        if (wm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                wm.getDefaultDisplay().getSize(size);
                return size;
            } else {
                Display d = wm.getDefaultDisplay();
                int w = d.getWidth();
                int h = d.getHeight();
                size.x = w;
                size.y = h;
            }
        }

        return size;
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") NetworkInfo netInfo = (cm != null ? cm.getActiveNetworkInfo() : null);
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static boolean openExternalUrlActivity(Context context, String url) {
        Uri uri = Uri.parse(url);

        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, uri));

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static long getAvailableStorageOnSDCARD() {
        boolean availableExternalStorage = Environment
                .getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (availableExternalStorage) {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory()
                    .getPath());

            long bytesAvailable;

            if (Build.VERSION.SDK_INT >= 18) {
                bytesAvailable = stat.getBlockSizeLong()
                        * stat.getAvailableBlocksLong();
            } else {
                bytesAvailable = (long) stat.getBlockSize()
                        * (long) stat.getAvailableBlocks();
            }

            return bytesAvailable / (1024 * 1024);
        }
        return 0;
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }

        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static long getAvailablePhoneStorage() {

        final File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());

        long blockSize, availableBlocks;

        if (Build.VERSION.SDK_INT >= 18) {
            blockSize = stat.getBlockSizeLong();
            availableBlocks = stat.getAvailableBlocksLong();
        } else {
            blockSize = stat.getBlockSize();
            availableBlocks = stat.getAvailableBlocks();
        }

        return availableBlocks * blockSize / (1024 * 1024);
    }

    public static void openAppMarketPage(Activity activity) {
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + activity.getPackageName())));
    }
}
