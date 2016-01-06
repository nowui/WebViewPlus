package com.nowui.webviewplus.activity;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshWebView;
import com.jockeyjs.Jockey;
import com.jockeyjs.JockeyHandler;
import com.jockeyjs.JockeyImpl;
import com.nowui.webviewplus.R;
import com.nowui.webviewplus.utility.Helper;
import com.nowui.webviewplus.utility.HttpDownloader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class BaseActivity extends Activity implements OnRefreshListener<WebView> {

	private Jockey jockey;
    private Button leftButton;
    private Button rightButton;
    private Button rightButton2;
    private TextView titleTextView;
    private PullToRefreshWebView pullRefreshWebView;
    private WebView webView;
    private String urlString;
    private TextView tipsTextView;
    private Boolean isLocal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_base);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);

        leftButton = (Button) findViewById(R.id.leftButton);
        Typeface font = Typeface.createFromAsset(getAssets(), getResources().getString(R.string.icon_font));
        leftButton.setTypeface(font);
        leftButton.setOnClickListener(new BackButtonListener());

        rightButton = (Button) findViewById(R.id.rightButton);
        rightButton2 = (Button) findViewById(R.id.rightButton2);

        titleTextView = (TextView) findViewById(R.id.titleTextView);

        pullRefreshWebView = (PullToRefreshWebView) findViewById(R.id.pull_refresh_webview);
        pullRefreshWebView.setMode(PullToRefreshBase.Mode.PULL_FROM_START);
        pullRefreshWebView.getLoadingLayoutProxy().setPullLabel(getResources().getString(R.string.pull));
        pullRefreshWebView.getLoadingLayoutProxy().setRefreshingLabel(getResources().getString(R.string.refreshing));
        pullRefreshWebView.getLoadingLayoutProxy().setReleaseLabel(getResources().getString(R.string.release));
        pullRefreshWebView.getLoadingLayoutProxy().setLastUpdatedLabel(getResources().getString(R.string.update) + Helper.formatDateTime());
        pullRefreshWebView.setOnRefreshListener(this);
        webView = pullRefreshWebView.getRefreshableView();
        //webView = (WebView) findViewById(R.id.webView);
        //webView.addJavascriptInterface(new InJavaScriptLocalObj(), "local_obj");
        webView.setWebChromeClient(new BaseWebChromeClient());
        //webView.setDownloadListener(new BaseDownloadListener());

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setSaveFormData(false);
        webSettings.setSavePassword(false);

        jockey = JockeyImpl.getDefault();
        jockey.configure(webView);
        jockey.setWebViewClient(new BaseWebViewClient());

        String url = getIntent().getStringExtra(Helper.KeyUrl);
        isLocal = getIntent().getBooleanExtra(Helper.KeyIsLocal, true);

        if(!Helper.isNullOrEmpty(url)) {
            loadUrl(url);

            if (url.equals("index.html")) {

            } else {
                leftButton.setVisibility(View.VISIBLE);
            }
        }

        tipsTextView = (TextView) findViewById(R.id.tipsTextView);
    }

    private void setListenerEvent() {

        jockey.on(Helper.EventPull + "-" + urlString, new JockeyHandler() {

            @Override
            public void doPerform(Map<Object, Object> payload) {
                System.out.println("jockey.on:" + Helper.EventPull + " - " + urlString);

                if((Boolean) payload.get(Helper.KeyIsOpen)) {
                    pullRefreshWebView.setMode(PullToRefreshBase.Mode.PULL_FROM_START);
                } else {
                    pullRefreshWebView.setMode(PullToRefreshBase.Mode.DISABLED);
                }
            }

        });

        jockey.on(Helper.EventTitle + "-" + urlString, new JockeyHandler() {

            @Override
            public void doPerform(Map<Object, Object> payload) {
                System.out.println("jockey.on:" + Helper.EventTitle + " - " + urlString);


                titleTextView.setText((String) payload.get(Helper.KeyText));
            }

        });

        jockey.on(Helper.EventBack + "-" + urlString, new JockeyHandler() {

            @Override
            public void doPerform(Map<Object, Object> payload) {
                System.out.println("jockey.on:" + Helper.EventBack + " - " + urlString);

                finish();
            }

        });

        jockey.on(Helper.EventBackCallback + "-" + urlString, new JockeyHandler() {

            @Override
            public void doPerform(Map<Object, Object> payload) {
                System.out.println("jockey.on:" + Helper.EventBackCallback + " - " + urlString);

                Intent intent = new Intent();
                intent.putExtra(Helper.KeyParameter, (Serializable) payload);
                setResult(Helper.CodeResult, intent);
                finish();
            }

        });

        jockey.on(Helper.EventNormal + "-" + urlString, new JockeyHandler() {

            @Override
            public void doPerform(final Map<Object, Object> payload) {
                System.out.println("jockey.on:" + Helper.EventNormal + " - " + urlString);

                rightButton.setVisibility(View.VISIBLE);
                rightButton.setText((String) payload.get(Helper.KeyText));
                rightButton.setOnClickListener(new Button.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Map<Object, Object> map = new HashMap<Object, Object>();
                        map.putAll(payload);
                        jockey.send(Helper.EventNormalCallback + "-" + urlString, webView, map);
                        System.out.println("jockey.send:" + Helper.EventNormalCallback + " - " + urlString);
                    }
                });
            }

        });

        jockey.on(Helper.EventNormal2 + "-" + urlString, new JockeyHandler() {

            @Override
            public void doPerform(final Map<Object, Object> payload) {
                System.out.println("jockey.on:" + Helper.EventNormal2 + " - " + urlString);

                rightButton2.setVisibility(View.VISIBLE);
                rightButton2.setText((String) payload.get(Helper.KeyText));
                rightButton2.setOnClickListener(new Button.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Map<Object, Object> map = new HashMap<Object, Object>();
                        map.putAll(payload);
                        jockey.send(Helper.EventNormalCallback2 + "-" + urlString, webView, map);
                        System.out.println("jockey.send:" + Helper.EventNormalCallback2 + " - " + urlString);
                    }
                });
            }

        });

        jockey.on(Helper.EventDownload + "-" + urlString, new JockeyHandler() {

            @Override
            public void doPerform(Map<Object, Object> payload) {
                System.out.println("jockey.on:" + Helper.EventDownload + " - " + urlString);

                final String path = payload.get(Helper.KeyPath).toString().replaceAll("/", "_");
                final String url = Helper.WebUrl + "attachment/download?path=" + payload.get(Helper.KeyPath);
                //final String url = "http://61.191.16.150:8081/System-HFCDC-Web/attachment/download?path=ZTovQk9FUkRvd25sb2Fkcy9pbWFnZS9jb21wMTUvcGVyc29uMTQ4LzgxusWjqM34wuew5qOpXzIwMTUwNzE0MTY1MjM1MjYwLmRvYw@3@3";

                try {
                    final String name = URLDecoder.decode(payload.get(Helper.KeyName).toString(), "UTF-8");

                    File hfcdcFile = new File(Environment.getExternalStorageDirectory() + "/hfcdc/");
                    if(!hfcdcFile.exists()) {
                        hfcdcFile.mkdir();
                    }

                    File pathFile = new File(Environment.getExternalStorageDirectory() + "/hfcdc/" + path +"/");
                    if(!pathFile.exists()) {
                        pathFile.mkdir();
                    }

                    File file = new File(Environment.getExternalStorageDirectory() + "/hfcdc/" + path +"/" + name);
                    if(file.exists()) {
                        startFileViewActivity(path, url, name);
                    } else {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                HttpDownloader downloader = new HttpDownloader();
                                int result = downloader.downFile(url, "/hfcdc/" + path +"/", name);

                                startFileViewActivity(path, url, name);
                            }
                        }).start();
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

        });

    }

    private void startFileViewActivity(String path, String url, String name) {
        File file = new File(Environment.getExternalStorageDirectory() + "/hfcdc/" + path +"/" + name);

        if(file.exists()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Uri uri = Uri.fromFile(file);
                intent.setDataAndType(uri, getMIMEType(file));
                startActivity(intent);

                finish();
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
    }

    private String getMIMEType(File file) {

        String type="*/*";
        String fName = file.getName();
        //获取后缀名前的分隔符"."在fName中的位置。
        int dotIndex = fName.lastIndexOf(".");
        if(dotIndex < 0){
            return type;
        }
    /* 获取文件的后缀名*/
        String end=fName.substring(dotIndex,fName.length()).toLowerCase();
        if(end=="")return type;
        //在MIME和文件类型的匹配表中找到对应的MIME类型。
        for(int i=0;i<MIME_MapTable.length;i++){ //MIME_MapTable??在这里你一定有疑问，这个MIME_MapTable是什么？
            if(end.equals(MIME_MapTable[i][0]))
                type = MIME_MapTable[i][1];
        }
        return type;
    }

    @Override
    protected void onDestroy() {
        System.out.println("onStop:" + urlString);

        super.onDestroy();

        jockey.off(Helper.EventBack + "-" + urlString);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println("onActivityResult:" + urlString);
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Helper.CodeRequest) {
            if (resultCode == Helper.CodeResult) {
                Map<Object, Object> map = (Map<Object, Object>) data.getSerializableExtra(Helper.KeyParameter);
                jockey.send(Helper.EventBackCallback + "-" + urlString, webView, map);
                System.out.println("jockey.send:" + Helper.EventBackCallback + " - " + urlString);
            }
        }
    }

    public void loadUrl(String url) {
        System.out.println("loadUrl:" + url);

        urlString = url;

        setListenerEvent();

        if(isLocal) {
            webView.loadUrl(Helper.WebUrl + url);
        } else {
            titleTextView.setText("在线预览");

            webView.loadUrl(url);
        }
    }

    private class InJavaScriptLocalObj {

        @JavascriptInterface
        public void showSource(String html) {
            System.out.println("showSource:" + urlString);

            Document document = Jsoup.parse(html);

            titleTextView.setText(document.title());
        }

    }

    private class BaseWebViewClient extends WebViewClient {

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            //System.out.println("shouldInterceptRequest:" + url);

            List<Map<String, String>> list = new ArrayList<Map<String, String>>();

            Map<String, String> map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "text/html");
            map.put(Helper.KeyName, ".html");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "text/javascript");
            map.put(Helper.KeyName, ".js");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "text/css");
            map.put(Helper.KeyName, ".css");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "image/png");
            map.put(Helper.KeyName, ".png");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "application/x-font-ttf");
            map.put(Helper.KeyName, ".ttf");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "application/x-font-woff");
            map.put(Helper.KeyName, ".woff");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "application/x-font-woff2");
            map.put(Helper.KeyName, ".woff2");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "application/x-font-eot");
            map.put(Helper.KeyName, ".eot");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "application/x-font-svg");
            map.put(Helper.KeyName, ".svg");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "application/x-font-otf");
            map.put(Helper.KeyName, ".otf");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "image/x-icon");
            map.put(Helper.KeyName, ".ico");
            list.add(map);

            if(isLocal) {
                Boolean isNotExit = true;
                for (Map<String, String> item : list) {
                    if (url.contains(item.get(Helper.KeyName))) {
                        WebResourceResponse response = null;

                        int index = url.indexOf("?");
                        String tempUrl = url;
                        if(index > -1) {
                            tempUrl = url.substring(0, url.indexOf("?"));
                        }

                        try {
                            response = new WebResourceResponse(item.get(Helper.KeyMimeType), "utf-8", getAssets().open(tempUrl.replace(Helper.WebUrl, "")));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (response != null) {
                            isNotExit = false;
                        }

                        return response;
                    }
                }

                if (isNotExit) {
                    System.out.println("--------------------------------------------:" + url);
                }
            }

            return super.shouldInterceptRequest(view, url);

            /*List<Map<String, String>> list = new ArrayList<Map<String, String>>();

            Map<String, String> map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "text/html");
            map.put(Helper.KeyUrl, Helper.WebUrl + "index");
            map.put(Helper.KeyName, "index.html");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "text/html");
            map.put(Helper.KeyUrl, Helper.WebUrl + "login");
            map.put(Helper.KeyName, "login.html");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "text/javascript");
            map.put(Helper.KeyUrl, "amazeui.min.js");
            map.put(Helper.KeyName, "js/amazeui.min.js");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "text/javascript");
            map.put(Helper.KeyUrl, "jockey.js");
            map.put(Helper.KeyName, "js/jockey.js");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "text/javascript");
            map.put(Helper.KeyUrl, "jquery.cookie.js");
            map.put(Helper.KeyName, "js/jquery.cookie.js");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "text/javascript");
            map.put(Helper.KeyUrl, "jquery.min.js");
            map.put(Helper.KeyName, "js/jquery.min.js");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "text/javascript");
            map.put(Helper.KeyUrl, "amazeui.min.js");
            map.put(Helper.KeyName, "js/amazeui.min.js");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "text/javascript");
            map.put(Helper.KeyUrl, "app.js");
            map.put(Helper.KeyName, "js/app.js");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "text/css");
            map.put(Helper.KeyUrl, "amazeui.min.css");
            map.put(Helper.KeyName, "css/amazeui.min.css");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "text/css");
            map.put(Helper.KeyUrl, "app.css");
            map.put(Helper.KeyName, "css/app.css");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "image/png");
            map.put(Helper.KeyUrl, "00.png");
            map.put(Helper.KeyName, "image/00.png");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "image/png");
            map.put(Helper.KeyUrl, "01.png");
            map.put(Helper.KeyName, "image/01.png");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "image/png");
            map.put(Helper.KeyUrl, "02.png");
            map.put(Helper.KeyName, "image/02.png");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "image/png");
            map.put(Helper.KeyUrl, "03.png");
            map.put(Helper.KeyName, "image/03.png");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "image/png");
            map.put(Helper.KeyUrl, "04.png");
            map.put(Helper.KeyName, "image/04.png");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "image/png");
            map.put(Helper.KeyUrl, "05.png");
            map.put(Helper.KeyName, "image/05.png");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "image/png");
            map.put(Helper.KeyUrl, "banner.png");
            map.put(Helper.KeyName, "image/banner.png");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "image/png");
            map.put(Helper.KeyUrl, "head.png");
            map.put(Helper.KeyName, "image/head.png");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "application/x-font-ttf");
            map.put(Helper.KeyUrl, "fontawesome-webfont.ttf");
            map.put(Helper.KeyName, "fonts/fontawesome-webfont.ttf");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "application/x-font-woff");
            map.put(Helper.KeyUrl, "fontawesome-webfont.woff");
            map.put(Helper.KeyName, "fonts/fontawesome-webfont.woff");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "application/x-font-woff2");
            map.put(Helper.KeyUrl, "fontawesome-webfont.woff2");
            map.put(Helper.KeyName, "fonts/fontawesome-webfont.woff2");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "application/x-font-eot");
            map.put(Helper.KeyUrl, "fontawesome-webfont.eot");
            map.put(Helper.KeyName, "fonts/fontawesome-webfont.eot");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "application/x-font-svg");
            map.put(Helper.KeyUrl, "fontawesome-webfont.svg");
            map.put(Helper.KeyName, "fonts/fontawesome-webfont.svg");
            list.add(map);

            map = new HashMap<String, String>();
            map.put(Helper.KeyMimeType, "application/x-font-otf");
            map.put(Helper.KeyUrl, "FontAwesome.otf");
            map.put(Helper.KeyName, "fonts/FontAwesome.otf");
            list.add(map);

            if(isLocal) {
                Boolean isNotExit = true;
                for (Map<String, String> item : list) {
                    if (url.contains(item.get(Helper.KeyUrl))) {
                        WebResourceResponse response = null;

                        try {
                            response = new WebResourceResponse(item.get(Helper.KeyMimeType), "utf-8", getAssets().open(item.get(Helper.KeyName)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (response != null) {
                            isNotExit = false;
                        }

                        return response;
                    }
                }

                if (isNotExit) {
                    //System.out.println("shouldInterceptRequest:" + url);
                }
            }

            return super.shouldInterceptRequest(view, url);*/
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.contains("jockey://")) {
            	return super.shouldOverrideUrlLoading(view, url);
            }

            if(!isLocal) {
                return false;
            }

            System.out.println("shouldOverrideUrlLoading:" + url);

            /*if(url.contains("attachment/download")) {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);

                return true;
            }*/

            if(url.indexOf(Helper.WebUrl) == 0) {
                url = url.replace(Helper.WebUrl, "");
            }

            if (url.equals(urlString)) {

            } else {
                Intent intent = new Intent();
                intent.putExtra(Helper.KeyUrl, url);
                intent.putExtra(Helper.KeyIsLocal, !url.contains("http://"));
                intent.setClass(BaseActivity.this, BaseActivity.class);
                startActivityForResult(intent, Helper.CodeRequest);
            }

            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            System.out.println("onPageStarted:" + urlString);

            super.onPageStarted(view, url, favicon);

            //titleTextView.setText(getResources().getString(R.string.refreshing));
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            pullRefreshWebView.getLoadingLayoutProxy().setLastUpdatedLabel(getResources().getString(R.string.update) + Helper.formatDateTime());

            //view.loadUrl("javascript:window.local_obj.showSource('<head>'+" + "document.getElementsByTagName('html')[0].innerHTML+'</head>');");

            pullRefreshWebView.onRefreshComplete();
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            System.out.println("onReceivedError:" + urlString);

            super.onReceivedError(view, errorCode, description, failingUrl);

            titleTextView.setText(getResources().getString(R.string.load_error));

            tipsTextView.setVisibility(View.VISIBLE);
        }
    }

    private  class BaseWebChromeClient extends WebChromeClient {

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            Toast.makeText(BaseActivity.this, message, Toast.LENGTH_SHORT).show();
            result.confirm();
            return true;
        }

    }

    private class BackButtonListener implements Button.OnClickListener {

        @Override
        public void onClick(View v) {
            finish();
        }
    }

    @Override
    public void onRefresh(final PullToRefreshBase<WebView> refreshView) {
        //webView.reload();

        loadUrl(urlString);
    }

    private class BaseDownloadListener implements DownloadListener {

        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
    }

    private static final String[][] MIME_MapTable = {
            //{后缀名，MIME类型}
            {"3gp", "video/3gpp"},
            {"aab", "application/x-authoware-bin"},
            {"aam", "application/x-authoware-map"},
            {"aas", "application/x-authoware-seg"},
            {"ai", "application/postscript"},
            {"aif", "audio/x-aiff"},
            {"aifc", "audio/x-aiff"},
            {"aiff", "audio/x-aiff"},
            {"als", "audio/X-Alpha5"},
            {"amc", "application/x-mpeg"},
            {"ani", "application/octet-stream"},
            {"apk", "application/vnd.android.package-archive"},
            {"asc", "text/plain"},
            {"asd", "application/astound"},
            {"asf", "video/x-ms-asf"},
            {"asn", "application/astound"},
            {"asp", "application/x-asap"},
            {"asx", "video/x-ms-asf"},
            {"au", "audio/basic"},
            {"avb", "application/octet-stream"},
            {"avi", "video/x-msvideo"},
            {"awb", "audio/amr-wb"},
            {"bcpio", "application/x-bcpio"},
            {"bin", "application/octet-stream"},
            {"bld", "application/bld"},
            {"bld2", "application/bld2"},
            {"bmp", "image/bmp"},
            {"bpk", "application/octet-stream"},
            {"bz2", "application/x-bzip2"},
            {"c", "text/plain"},
            {"cal", "image/x-cals"},
            {"ccn", "application/x-cnc"},
            {"cco", "application/x-cocoa"},
            {"cdf", "application/x-netcdf"},
            {"cgi", "magnus-internal/cgi"},
            {"chat", "application/x-chat"},
            {"class", "application/octet-stream"},
            {"clp", "application/x-msclip"},
            {"cmx", "application/x-cmx"},
            {"co", "application/x-cult3d-object"},
            {"cod", "image/cis-cod"},
            {"conf", "text/plain"},
            {"cpio", "application/x-cpio"},
            {"cpp", "text/plain"},
            {"cpt", "application/mac-compactpro"},
            {"crd", "application/x-mscardfile"},
            {"csh", "application/x-csh"},
            {"csm", "chemical/x-csml"},
            {"csml", "chemical/x-csml"},
            {"css", "text/css"},
            {"cur", "application/octet-stream"},
            {"dcm", "x-lml/x-evm"},
            {"dcr", "application/x-director"},
            {"dcx", "image/x-dcx"},
            {"dhtml", "text/html"},
            {"dir", "application/x-director"},
            {"dll", "application/octet-stream"},
            {"dmg", "application/octet-stream"},
            {"dms", "application/octet-stream"},
            {"doc", "application/msword"},
            {"docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
            {"dot", "application/x-dot"},
            {"dvi", "application/x-dvi"},
            {"dwf", "drawing/x-dwf"},
            {"dwg", "application/x-autocad"},
            {"dxf", "application/x-autocad"},
            {"dxr", "application/x-director"},
            {"ebk", "application/x-expandedbook"},
            {"emb", "chemical/x-embl-dl-nucleotide"},
            {"embl", "chemical/x-embl-dl-nucleotide"},
            {"eps", "application/postscript"},
            {"eri", "image/x-eri"},
            {"es", "audio/echospeech"},
            {"esl", "audio/echospeech"},
            {"etc", "application/x-earthtime"},
            {"etx", "text/x-setext"},
            {"evm", "x-lml/x-evm"},
            {"evy", "application/x-envoy"},
            {"exe", "application/octet-stream"},
            {"fh4", "image/x-freehand"},
            {"fh5", "image/x-freehand"},
            {"fhc", "image/x-freehand"},
            {"fif", "image/fif"},
            {"fm", "application/x-maker"},
            {"fpx", "image/x-fpx"},
            {"fvi", "video/isivideo"},
            {"gau", "chemical/x-gaussian-input"},
            {"gca", "application/x-gca-compressed"},
            {"gdb", "x-lml/x-gdb"},
            {"gif", "image/gif"},
            {"gps", "application/x-gps"},
            {"gtar", "application/x-gtar"},
            {"gz", "application/x-gzip"},
            {"h", "text/plain"},
            {"hdf", "application/x-hdf"},
            {"hdm", "text/x-hdml"},
            {"hdml", "text/x-hdml"},
            {"hlp", "application/winhlp"},
            {"hqx", "application/mac-binhex40"},
            {"htm", "text/html"},
            {"html", "text/html"},
            {"hts", "text/html"},
            {"ice", "x-conference/x-cooltalk"},
            {"ico", "application/octet-stream"},
            {"ief", "image/ief"},
            {"ifm", "image/gif"},
            {"ifs", "image/ifs"},
            {"imy", "audio/melody"},
            {"ins", "application/x-NET-Install"},
            {"ips", "application/x-ipscript"},
            {"ipx", "application/x-ipix"},
            {"it", "audio/x-mod"},
            {"itz", "audio/x-mod"},
            {"ivr", "i-world/i-vrml"},
            {"j2k", "image/j2k"},
            {"jad", "text/vnd.sun.j2me.app-descriptor"},
            {"jam", "application/x-jam"},
            {"jar", "application/java-archive"},
            {"java", "text/plain"},
            {"jnlp", "application/x-java-jnlp-file"},
            {"jpe", "image/jpeg"},
            {"jpeg", "image/jpeg"},
            {"jpg", "image/jpeg"},
            {"jpz", "image/jpeg"},
            {"js", "application/x-javascript"},
            {"jwc", "application/jwc"},
            {"kjx", "application/x-kjx"},
            {"lak", "x-lml/x-lak"},
            {"latex", "application/x-latex"},
            {"lcc", "application/fastman"},
            {"lcl", "application/x-digitalloca"},
            {"lcr", "application/x-digitalloca"},
            {"lgh", "application/lgh"},
            {"lha", "application/octet-stream"},
            {"lml", "x-lml/x-lml"},
            {"lmlpack", "x-lml/x-lmlpack"},
            {"log", "text/plain"},
            {"lsf", "video/x-ms-asf"},
            {"lsx", "video/x-ms-asf"},
            {"lzh", "application/x-lzh"},
            {"m13", "application/x-msmediaview"},
            {"m14", "application/x-msmediaview"},
            {"m15", "audio/x-mod"},
            {"m3u", "audio/x-mpegurl"},
            {"m3url", "audio/x-mpegurl"},
            {"m4a",    "audio/mp4a-latm"},
            {"m4b",    "audio/mp4a-latm"},
            {"m4p",    "audio/mp4a-latm"},
            {"m4u",    "video/vnd.mpegurl"},
            {"m4v",    "video/x-m4v"},
            {"ma1", "audio/ma1"},
            {"ma2", "audio/ma2"},
            {"ma3", "audio/ma3"},
            {"ma5", "audio/ma5"},
            {"man", "application/x-troff-man"},
            {"map", "magnus-internal/imagemap"},
            {"mbd", "application/mbedlet"},
            {"mct", "application/x-mascot"},
            {"mdb", "application/x-msaccess"},
            {"mdz", "audio/x-mod"},
            {"me", "application/x-troff-me"},
            {"mel", "text/x-vmel"},
            {"mi", "application/x-mif"},
            {"mid", "audio/midi"},
            {"midi", "audio/midi"},
            {"mif", "application/x-mif"},
            {"mil", "image/x-cals"},
            {"mio", "audio/x-mio"},
            {"mmf", "application/x-skt-lbs"},
            {"mng", "video/x-mng"},
            {"mny", "application/x-msmoney"},
            {"moc", "application/x-mocha"},
            {"mocha", "application/x-mocha"},
            {"mod", "audio/x-mod"},
            {"mof", "application/x-yumekara"},
            {"mol", "chemical/x-mdl-molfile"},
            {"mop", "chemical/x-mopac-input"},
            {"mov", "video/quicktime"},
            {"movie", "video/x-sgi-movie"},
            {"mp2", "audio/x-mpeg"},
            {"mp3", "audio/x-mpeg"},
            {"mp4", "video/mp4"},
            {"mpc", "application/vnd.mpohun.certificate"},
            {"mpe", "video/mpeg"},
            {"mpeg", "video/mpeg"},
            {"mpg video/mpeg"},
            {"mpg4", "video/mp4"},
            {"mpga", "audio/mpeg"},
            {"mpn", "application/vnd.mophun.application"},
            {"mpp", "application/vnd.ms-project"},
            {"mps", "application/x-mapserver"},
            {"mrl", "text/x-mrml"},
            {"mrm", "application/x-mrm"},
            {"ms", "application/x-troff-ms"},
            {"msg",     "application/vnd.ms-outlook"},
            {"mts", "application/metastream"},
            {"mtx", "application/metastream"},
            {"mtz", "application/metastream"},
            {"mzv", "application/metastream"},
            {"nar", "application/zip"},
            {"nbmp", "image/nbmp"},
            {"nc", "application/x-netcdf"},
            {"ndb", "x-lml/x-ndb"},
            {"ndwn", "application/ndwn"},
            {"nif", "application/x-nif"},
            {"nmz", "application/x-scream"},
            {"nokia-op-logo", "image/vnd.nok-oplogo-color"},
            {"npx", "application/x-netfpx"},
            {"nsnd", "audio/nsnd"},
            {"nva", "application/x-neva1"},
            {"oda", "application/oda"},
            {"ogg", "audio/ogg"},
            {"oom", "application/x-AtlasMate-Plugin"},
            {"pac", "audio/x-pac"},
            {"pae", "audio/x-epac"},
            {"pan", "application/x-pan"},
            {"pbm", "image/x-portable-bitmap"},
            {"pcx", "image/x-pcx"},
            {"pda", "image/x-pda"},
            {"pdb", "chemical/x-pdb"},
            {"pdf", "application/pdf"},
            {"pfr", "application/font-tdpfr"},
            {"pgm", "image/x-portable-graymap"},
            {"pict", "image/x-pict"},
            {"pm", "application/x-perl"},
            {"pmd", "application/x-pmd"},
            {"png", "image/png"},
            {"pnm", "image/x-portable-anymap"},
            {"pnz", "image/png"},
            {"pot", "application/vnd.ms-powerpoint"},
            {"ppm", "image/x-portable-pixmap"},
            {"pps", "application/vnd.ms-powerpoint"},
            {"ppt", "application/vnd.ms-powerpoint"},
            {"pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"},
            {"pqf", "application/x-cprplayer"},
            {"pqi", "application/cprplayer"},
            {"prc", "application/x-prc"},
            {"prop", "text/plain"},
            {"proxy", "application/x-ns-proxy-autoconfig"},
            {"ps", "application/postscript"},
            {"ptlk", "application/listenup"},
            {"pub", "application/x-mspublisher"},
            {"pvx", "video/x-pv-pvx"},
            {"qcp", "audio/vnd.qcelp"},
            {"qt", "video/quicktime"},
            {"qti", "image/x-quicktime"},
            {"qtif", "image/x-quicktime"},
            {"r3t", "text/vnd.rn-realtext3d"},
            {"ra", "audio/x-pn-realaudio"},
            {"ram", "audio/x-pn-realaudio"},
            {"rar", "application/x-rar-compressed"},
            {"ras", "image/x-cmu-raster"},
            {"rc", "text/plain"},
            {"rdf", "application/rdf+xml"},
            {"rf", "image/vnd.rn-realflash"},
            {"rgb", "image/x-rgb"},
            {"rlf", "application/x-richlink"},
            {"rm", "audio/x-pn-realaudio"},
            {"rmf", "audio/x-rmf"},
            {"rmm", "audio/x-pn-realaudio"},
            {"rmvb", "audio/x-pn-realaudio"},
            {"rnx", "application/vnd.rn-realplayer"},
            {"roff", "application/x-troff"},
            {"rp", "image/vnd.rn-realpix"},
            {"rpm", "audio/x-pn-realaudio-plugin"},
            {"rt", "text/vnd.rn-realtext"},
            {"rte", "x-lml/x-gps"},
            {"rtf", "application/rtf"},
            {"rtg", "application/metastream"},
            {"rtx", "text/richtext"},
            {"rv", "video/vnd.rn-realvideo"},
            {"rwc", "application/x-rogerwilco"},
            {"s3m", "audio/x-mod"},
            {"s3z", "audio/x-mod"},
            {"sca", "application/x-supercard"},
            {"scd", "application/x-msschedule"},
            {"sdf", "application/e-score"},
            {"sea", "application/x-stuffit"},
            {"sgm", "text/x-sgml"},
            {"sgml", "text/x-sgml"},
            {"sh", "application/x-sh"},
            {"shar", "application/x-shar"},
            {"shtml", "magnus-internal/parsed-html"},
            {"shw", "application/presentations"},
            {"si6", "image/si6"},
            {"si7", "image/vnd.stiwap.sis"},
            {"si9", "image/vnd.lgtwap.sis"},
            {"sis", "application/vnd.symbian.install"},
            {"sit", "application/x-stuffit"},
            {"skd", "application/x-Koan"},
            {"skm", "application/x-Koan"},
            {"skp", "application/x-Koan"},
            {"skt", "application/x-Koan"},
            {"slc", "application/x-salsa"},
            {"smd", "audio/x-smd"},
            {"smi", "application/smil"},
            {"smil", "application/smil"},
            {"smp", "application/studiom"},
            {"smz", "audio/x-smd"},
            {"snd", "audio/basic"},
            {"spc", "text/x-speech"},
            {"spl", "application/futuresplash"},
            {"spr", "application/x-sprite"},
            {"sprite", "application/x-sprite"},
            {"spt", "application/x-spt"},
            {"src", "application/x-wais-source"},
            {"stk", "application/hyperstudio"},
            {"stm", "audio/x-mod"},
            {"sv4cpio", "application/x-sv4cpio"},
            {"sv4crc", "application/x-sv4crc"},
            {"svf", "image/vnd"},
            {"svg", "image/svg-xml"},
            {"svh", "image/svh"},
            {"svr", "x-world/x-svr"},
            {"swf", "application/x-shockwave-flash"},
            {"swfl", "application/x-shockwave-flash"},
            {"t", "application/x-troff"},
            {"tad", "application/octet-stream"},
            {"talk", "text/x-speech"},
            {"tar", "application/x-tar"},
            {"taz", "application/x-tar"},
            {"tbp", "application/x-timbuktu"},
            {"tbt", "application/x-timbuktu"},
            {"tcl", "application/x-tcl"},
            {"tex", "application/x-tex"},
            {"texi", "application/x-texinfo"},
            {"texinfo", "application/x-texinfo"},
            {"tgz", "application/x-tar"},
            {"thm", "application/vnd.eri.thm"},
            {"tif", "image/tiff"},
            {"tiff", "image/tiff"},
            {"tki", "application/x-tkined"},
            {"tkined", "application/x-tkined"},
            {"toc", "application/toc"},
            {"toy", "image/toy"},
            {"tr", "application/x-troff"},
            {"trk", "x-lml/x-gps"},
            {"trm", "application/x-msterminal"},
            {"tsi", "audio/tsplayer"},
            {"tsp", "application/dsptype"},
            {"tsv", "text/tab-separated-values"},
            {"tsv", "text/tab-separated-values"},
            {"ttf", "application/octet-stream"},
            {"ttz", "application/t-time"},
            {"txt", "text/plain"},
            {"ult", "audio/x-mod"},
            {"ustar", "application/x-ustar"},
            {"uu", "application/x-uuencode"},
            {"uue", "application/x-uuencode"},
            {"vcd", "application/x-cdlink"},
            {"vcf", "text/x-vcard"},
            {"vdo", "video/vdo"},
            {"vib", "audio/vib"},
            {"viv", "video/vivo"},
            {"vivo", "video/vivo"},
            {"vmd", "application/vocaltec-media-desc"},
            {"vmf", "application/vocaltec-media-file"},
            {"vmi", "application/x-dreamcast-vms-info"},
            {"vms", "application/x-dreamcast-vms"},
            {"vox", "audio/voxware"},
            {"vqe", "audio/x-twinvq-plugin"},
            {"vqf", "audio/x-twinvq"},
            {"vql", "audio/x-twinvq"},
            {"vre", "x-world/x-vream"},
            {"vrml", "x-world/x-vrml"},
            {"vrt", "x-world/x-vrt"},
            {"vrw", "x-world/x-vream"},
            {"vts", "workbook/formulaone"},
            {"wav", "audio/x-wav"},
            {"wax", "audio/x-ms-wax"},
            {"wbmp", "image/vnd.wap.wbmp"},
            {"web", "application/vnd.xara"},
            {"wi", "image/wavelet"},
            {"wis", "application/x-InstallShield"},
            {"wm", "video/x-ms-wm"},
            {"wma", "audio/x-ms-wma"},
            {"wmd", "application/x-ms-wmd"},
            {"wmf", "application/x-msmetafile"},
            {"wml", "text/vnd.wap.wml"},
            {"wmlc", "application/vnd.wap.wmlc"},
            {"wmls", "text/vnd.wap.wmlscript"},
            {"wmlsc", "application/vnd.wap.wmlscriptc"},
            {"wmlscript", "text/vnd.wap.wmlscript"},
            {"wmv", "audio/x-ms-wmv"},
            {"wmx", "video/x-ms-wmx"},
            {"wmz", "application/x-ms-wmz"},
            {"wpng", "image/x-up-wpng"},
            {"wps", "application/vnd.ms-works"},
            {"wpt", "x-lml/x-gps"},
            {"wri", "application/x-mswrite"},
            {"wrl", "x-world/x-vrml"},
            {"wrz", "x-world/x-vrml"},
            {"ws", "text/vnd.wap.wmlscript"},
            {"wsc", "application/vnd.wap.wmlscriptc"},
            {"wv", "video/wavelet"},
            {"wvx", "video/x-ms-wvx"},
            {"wxl", "application/x-wxl"},
            {"x-gzip", "application/x-gzip"},
            {"xar", "application/vnd.xara"},
            {"xbm", "image/x-xbitmap"},
            {"xdm", "application/x-xdma"},
            {"xdma", "application/x-xdma"},
            {"xdw", "application/vnd.fujixerox.docuworks"},
            {"xht", "application/xhtml+xml"},
            {"xhtm", "application/xhtml+xml"},
            {"xhtml", "application/xhtml+xml"},
            {"xla", "application/vnd.ms-excel"},
            {"xlc", "application/vnd.ms-excel"},
            {"xll", "application/x-excel"},
            {"xlm", "application/vnd.ms-excel"},
            {"xls", "application/vnd.ms-excel"},
            {"xlt", "application/vnd.ms-excel"},
            {"xlw", "application/vnd.ms-excel"},
            {"xlsx",      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
            {"xm", "audio/x-mod"},
            {"xml", "text/xml"},
            {"xmz", "audio/x-mod"},
            {"xpi", "application/x-xpinstall"},
            {"xpm", "image/x-xpixmap"},
            {"xsit", "text/xml"},
            {"xsl", "text/xml"},
            {"xul", "text/xul"},
            {"xwd", "image/x-xwindowdump"},
            {"xyz", "chemical/x-pdb"},
            {"yz1", "application/x-yz1"},
            {"z", "application/x-compress"},
            {"zac", "application/x-zaurus-zac"},
            {"zip", "application/zip"},
            {"", "*/*"}
    };

}
