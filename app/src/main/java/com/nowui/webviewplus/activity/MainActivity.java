package com.nowui.webviewplus.activity;

import android.os.Bundle;
import android.os.Looper;

import com.nowui.webviewplus.utility.UpdateManager;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Thread(){
            @Override
            public void run() {
                Looper.prepare();
                UpdateManager um = new UpdateManager(MainActivity.this);
                um.checkUpdate();
                Looper.loop();
            }
        }.start();

        loadUrl("login.html");
    }

}
