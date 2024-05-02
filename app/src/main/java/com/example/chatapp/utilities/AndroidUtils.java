package com.example.chatapp.utilities;

import android.content.Context;
import android.widget.Toast;

public class AndroidUtils {
    public static void showToast(Context context,String message){
        Toast.makeText(context,message,Toast.LENGTH_LONG).show();
    }
}
