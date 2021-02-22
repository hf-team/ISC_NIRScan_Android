package com.Innospectra.NanoScan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Response;

public class MainActivity extends Activity  {
    private static Context mContext;
    private EditText username;
    private EditText password;
    private Button login;

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
    }
    private void findViews() {
        username=(EditText) findViewById(R.id.username);
        password=(EditText) findViewById(R.id.password);
        login=(Button) findViewById(R.id.login);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent();
//                intent.setClass(MainActivity.this,HomeViewActivity.class);
//                // 存储当前用户名，用于工具界面显示信息
//                startActivity(intent);

                String url = "http://211.82.95.146:5005/login";
                User user = new User();
                user.setUsername(username.getText().toString());
                user.setPassword(password.getText().toString());
                String json =  JSON.toJSONString(user);
                Call call = OkHttpClientUtil.login(url,json);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.d("TAG", "onFailure: ");
                        Looper.prepare();
                        Toast.makeText(MainActivity.this,"连接服务器失败!",Toast.LENGTH_SHORT).show();
                        Looper.loop();
                    }
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String json =response.body().string();
                        if(json.indexOf("AccessToken") != -1) {
                            JSONObject jsonObject = (JSONObject) JSONObject.parse(json);
                            // 实现页面跳转
                            Intent intent = new Intent();
                            // 存储当前用户名，用于工具界面显示信息
                            intent.putExtra("username",username.getText().toString());
                            intent.putExtra("token",jsonObject.getString("AccessToken"));
                            intent.setClass(MainActivity.this,HomeViewActivity.class);
                            startActivity(intent);
                            Log.d("TAG", "onResponse: " + json);
                        } else if (username.getText().toString().equals("") || password.getText().toString().equals("")){
                            // 用户名、密码不能为空
                            Looper.prepare();
                            Toast.makeText(MainActivity.this,"用户名/密码不能为空!",Toast.LENGTH_SHORT).show();
                            Looper.loop();
                        } else {
                            // 用户名、密码不正确
                            Looper.prepare();
                            Toast.makeText(MainActivity.this,"登录失败，密码或用户名错误!",Toast.LENGTH_SHORT).show();
                            Looper.loop();
                        }

                    }
                });
            }
        });

    }


    // 2秒内点击两次返回键退出
    long exittime; // 设定退出时间间隔
    public boolean onKeyDown(int keyCode, KeyEvent event){ //参数：按的键；按键事件
        //  判断事件触发
        if (keyCode == KeyEvent.KEYCODE_BACK){
            // 判断两次点击间隔时间
            if((System.currentTimeMillis()-exittime)>2000){
                Toast.makeText(MainActivity.this,"再次返回程序退出！",Toast.LENGTH_SHORT).show();
                exittime = System.currentTimeMillis(); // 设置第一次点击时间
            }else{
                //finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode,event);
    }

}
