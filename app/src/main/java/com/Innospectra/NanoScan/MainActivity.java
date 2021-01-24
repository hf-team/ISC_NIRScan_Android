package com.Innospectra.NanoScan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity  {
    private static Context mContext;
    private EditText username;
    private EditText password;
    private Button login;

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
                String name=username.getText().toString();
                String pass=password.getText().toString();
                Log.i("TAG",name+"_"+pass);
                if("admin".equals(name)){
                    System.out.println("true");
                    initComponent();
                }
//                String url="http://211.82.95.146:5005/login?username=admin&password=admin";
//                String res = OkHttpUtils.getJsonByInternet(url);
//                Log.i("TAG", res );
            }
        });

    }


    private void initComponent(){
        login = (Button)findViewById(R.id.login);
        login.setOnClickListener(btn_listenser);
    }

    private Button.OnClickListener btn_listenser = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent main = new Intent(MainActivity.this, HomeViewActivity.class);
            main.putExtra("main","main");
            startActivity(main);
        }
    };

}
