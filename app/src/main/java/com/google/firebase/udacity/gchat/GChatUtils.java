package com.google.firebase.udacity.gchat;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.View;

/**
 * Created by Rajeev on 2/26/2017.
 */

public class GChatUtils {
    public static final String FIREBASE_ROOT_MESSAGES = "messages";

    public static void displayAlertMessage(String message,View view){
        if( TextUtils.isEmpty(message) && view!=null){
            Snackbar.make(view,message,Snackbar.LENGTH_SHORT).show();
        }
    }
}
