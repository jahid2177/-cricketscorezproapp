package com.cricketscorez.proapp;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import androidx.appcompat.app.AlertDialog;

/**
 * ExitDialogHelper
 * Unified exit confirmation dialog utility that can be invoked from any
 * Activity's onBackPressed() or OnBackPressedCallback / handleOnBackPressed() method.
 * Uses standard AlertDialog with 'Yes' and 'No' buttons to exit the application properly.
 */
public class ExitDialogHelper {

    private static final String DEFAULT_TITLE = "Exit App";
    private static final String DEFAULT_MESSAGE = "Are you sure you want to exit?";
    private static final String DEFAULT_POSITIVE = "Yes";
    private static final String DEFAULT_NEGATIVE = "No";

    /**
     * Shows a standard exit confirmation dialog.
     * When the user taps "Yes", finishAffinity() is called to exit the app.
     *
     * @param activity The host activity context.
     */
    public static void show(Activity activity) {
        show(activity, DEFAULT_TITLE, DEFAULT_MESSAGE, DEFAULT_POSITIVE, DEFAULT_NEGATIVE, null);
    }

    /**
     * Shows a standard exit confirmation dialog with a custom exit action.
     *
     * @param activity The host activity context.
     * @param onConfirmExit Custom runnable to execute on exit, or null to finishAffinity().
     */
    public static void show(Activity activity, Runnable onConfirmExit) {
        show(activity, DEFAULT_TITLE, DEFAULT_MESSAGE, DEFAULT_POSITIVE, DEFAULT_NEGATIVE, onConfirmExit);
    }

    /**
     * Full configuration method to display the exit dialog.
     *
     * @param activity The host activity context.
     * @param title Title of the dialog.
     * @param message Message body.
     * @param positiveText Positive button text (e.g., "Yes").
     * @param negativeText Negative button text (e.g., "No").
     * @param onConfirmExit Callback when positive button is clicked (if null, calls finishAffinity()).
     */
    public static void show(
            final Activity activity,
            String title,
            String message,
            String positiveText,
            String negativeText,
            final Runnable onConfirmExit) {

        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title != null ? title : DEFAULT_TITLE);
        builder.setMessage(message != null ? message : DEFAULT_MESSAGE);
        builder.setCancelable(true);

        builder.setPositiveButton(positiveText != null ? positiveText : DEFAULT_POSITIVE, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (onConfirmExit != null) {
                    onConfirmExit.run();
                } else {
                    activity.finishAffinity();
                }
            }
        });

        builder.setNegativeButton(negativeText != null ? negativeText : DEFAULT_NEGATIVE, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
