package com.Innospectra.NanoScan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity implements View.OnClickListener {
    private static Context mContext;
    Button btn;
    EditText login_id,login_password;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        login_id = (EditText)findViewById(R.id.login_id);
        login_password = (EditText)findViewById(R.id.login_password);
        btn = (Button) findViewById(R.id.login_button);
        btn.setOnClickListener(this);
        mContext = this;
        initComponent();
    }


    private void initComponent(){
        btn = (Button)findViewById(R.id.login_button);
        btn.setOnClickListener(btn_listenser);
    }

    private Button.OnClickListener btn_listenser = new Button.OnClickListener()
    {

        @Override
        public void onClick(View view) {
            Intent newscanhIntent = new Intent(mContext, HomeViewActivity.class);
            newscanhIntent.putExtra("main","main");
            startActivity(newscanhIntent);
        }
    };

    @Override
    public void onClick(View v)
    {
        String str = login_id.getText() + " " + login_password.getText();
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }
}
