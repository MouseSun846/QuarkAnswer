package com.example.quarkanswer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FloatingAnswerTextViewService extends Service {
    public static boolean isStarted = false;
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private SimpleDateFormat simpleDateFormat;
    private Handler mHandler;
    private TextView qw;
    private int color;
    private Timer mtimer;
    private Timer quarktimer;

    private OkHttpClient client;
    private Request.Builder builder;
    private String rescontent;
    private String answer;
    private final String cookie="__wpkreporterwid_=ce3b9724-842c-4b0d-3284-31fab5d84d08;sm_uuid=e8a730af39bbfbb7efbddcc1958ab256%7C%7C%7C1576647486;sm_diu=e8a730af39bbfbb7efbddcc1958ab256%7C%7C11eef1ee4ce8aa2f85%7C1576647486;sm_sid=ba385821df89b86e39221650509ccba7;";


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isStarted = true;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.TOP;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        layoutParams.x = 0;
        layoutParams.y = 100;
        mHandler = new Handler(getTimeCallback);
        //获取颜色设置信息
        SharedPreferences sp = getApplicationContext().getSharedPreferences("setting", Context.MODE_PRIVATE);
        color = sp.getInt("color",0xFF000000);
        simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        mtimer = new Timer();
        mtimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(1);
            }
        },0,400);

        client = new OkHttpClient();
        builder = new Request.Builder();
        quarktimer = new Timer();
        quarktimer.schedule(new TimerTask() {
            @Override
            public void run() {
                //800ms获取一次答案
                getQuarkAnswer();
            }
        },0,800);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
        mtimer.cancel();
        quarktimer.cancel();
        windowManager.removeViewImmediate(qw);
        isStarted = false;
        Log.i("mouse","------------------dead---------------");
    }
    private Handler.Callback getTimeCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 1){

//                long msecond = System.currentTimeMillis();
//                long mmsecond = msecond%1000;
//
//                String mtime =simpleDateFormat.format(new Date(msecond))+":"+mmsecond;
//                qw.setText(mtime);

                qw.setText(answer);
            }
            return false;
        }
    };
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showFloatingWindow();
        return super.onStartCommand(intent, flags, startId);
    }

    private void showFloatingWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)){
                qw = new TextView(getApplicationContext());
                qw.setBackgroundColor(0x00000000);
                qw.setTextSize(20);
                qw.setTextColor(color);
                windowManager.addView(qw,layoutParams);
                qw.setOnTouchListener(new FloatingOnTouchListener());
            }
        }
    }


    private class FloatingOnTouchListener implements View.OnTouchListener {
        private int x;
        private int y;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    layoutParams.x = layoutParams.x + movedX;
                    layoutParams.y = layoutParams.y + movedY;
                    windowManager.updateViewLayout(view, layoutParams);
                    break;
                default:
                    break;
            }
            return false;
        }
    }


    public void getQuarkAnswer(){
        //构造 URL
        String url="https://answer-quark.sm.cn/answer/curr?format=json&_t="+System.currentTimeMillis()+"&activity=ttkx";
        Request request = builder.url(url)
                .addHeader("cookie", cookie+rescontent)
                .addHeader("path", "/answer/curr?format=json&activity=ttkx")
                .addHeader("scheme", "https")
                .addHeader("accept", "application/json=")
                .addHeader("accept-language", "zh-CN,zh;q=0.9")
                .addHeader("User-Agent", "Mozilla/5.0(Linux;U;Android6.0.1;zh-CN;Redmi3SBuild/MMB29M)AppleWebKit/537.36(KHTML,likeGecko)Version/4.0Chrome/57.0.2987.108Quark/3.8.1.125MobileSafari/537.36").build();
       Call call=client.newCall(request);
       call.enqueue(callback);
    }
    //Unicode转中文方法
    private  String ascii2native ( String asciicode )
    {
        String[] asciis = asciicode.split ("\\\\u");
        String nativeValue = asciis[0];
        try
        {
            for ( int i = 1; i < asciis.length; i++ )
            {
                String code = asciis[i];
                nativeValue += (char) Integer.parseInt (code.substring (0, 4), 16);
                if (code.length () > 4)
                {
                    nativeValue += code.substring (4, code.length ());
                }
            }
        }
        catch (NumberFormatException e)
        {
            return asciicode;
        }
        return nativeValue;
    }
    private Callback callback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            e.printStackTrace();
            Log.i("mouse","Failure");
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            String str = new String(response.body().bytes(),"utf-8");
//            rescontent = str;
            if(str.contains("html")){
                //获取cookie
                Pattern pattern = Pattern.compile("sec=\\w+");
                Matcher matcher = pattern.matcher(str);
                if (matcher.find()){
                    String result = matcher.group();
                    if (result.length()>0){
                        rescontent=result;
//                        System.out.println(result);
                        Log.i("result","result "+ result);
                    }
                }
                answer = "---互动未开启---";
            }else{
//                String temp = "{\"status\":0,\"data\":{\"title\":\"未成年人的父母没有监护能力的,第一序列的法定监护人是？\",\"options\":[{\"confidence\":\"100\",\"score\":\"33.0432245532\",\"title\":\"关系密切的其他亲属\"},{\"confidence\":\"100\",\"score\":\"32.9639035349\",\"title\":\"祖父母、外祖父母\"},{\"confidence\":\"100\",\"score\":\"33.9928719119\",\"title\":\"住所地的居委会、村委会\"}],\"correct\":\"2\",\"official\":\"1\",\"status\":1,\"sid\":\"744\",\"round\":11,\"dateline\":\"1578910944\",\"suggest\":\"2\",\"time\":1578910994}}";
                //获取答案
                try {
                    JSONObject jsonObject = new JSONObject(ascii2native(str));
//                    JSONObject jsonObject = new JSONObject(temp);
                    Object data = jsonObject.getString("data");
                    String correct = new JSONObject((String) data).getString("correct");
                    JSONArray options = new JSONArray(new JSONObject((String) data).getString("options"));
                    if (correct.length()>0){
                        int ak = Integer.parseInt(correct)+1;
                        if (ak>=0){
                            JSONObject ans = (JSONObject) options.get(ak-1);
                            answer = "选项："+ak+"\n答案："+ans.getString("title");
                            Log.i("mouse",answer);
                        }
                    }
                } catch (JSONException e) {
                    answer="正在获取答案...";
                    e.printStackTrace();
                }
            }
        }
    };
}
