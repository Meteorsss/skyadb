package com.sky22333.skyadb.helper;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Looper;
import java.io.PrintStream;

/** Runs as ADB shell on the target, not on the controlling phone. No APK installation or root. */
public final class TvAppLabels {
    public static void main(String[] args) throws Exception {
        Looper.prepareMainLooper();
        Class<?> activityThread = Class.forName("android.app.ActivityThread");
        Object thread = activityThread.getMethod("systemMain").invoke(null);
        Context system = (Context) activityThread.getMethod("getSystemContext").invoke(thread);
        Context shell = system.createPackageContext("com.android.shell", Context.CONTEXT_IGNORE_SECURITY);
        PackageManager pm = shell.getPackageManager();
        PrintStream out = new PrintStream(System.out, true, "UTF-8");
        for (ApplicationInfo app : pm.getInstalledApplications(0)) {
            try {
                String label = pm.getApplicationLabel(app).toString()
                    .replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim();
                if (!label.isEmpty()) out.println("SKYAPP\t" + app.packageName + "\t" + label);
            } catch (RuntimeException ignored) {
                // A disappearing/broken package must not abort the remaining labels.
            }
        }
        out.println("SKYEND");
        System.exit(0);
    }
}
