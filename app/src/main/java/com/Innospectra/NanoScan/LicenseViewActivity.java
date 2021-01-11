package com.Innospectra.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * Created by iris.lin on 2018/2/5.
 */

public class LicenseViewActivity extends Activity {
    private static Context mContext;
    TextView tv_nirsdk;
    TextView tv_charts;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.license_view);
        mContext = this;
        //Set up the action bar title, and enable the back button
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        initComponts();
    }
    private void initComponts()
    {
        tv_nirsdk = (TextView)findViewById(R.id.tv_nirsdk);
        tv_charts = (TextView)findViewById(R.id.tv_charts);

        String sdk_license = "  \n" +"    Software License Agreement (BSD License)\n" +
                "\n" +
                "   Copyright (c) 2015, KS Technologies, LLC All rights reserved.\n" +
                "\n" +
                "   Redistribution and use of this software in source and binary forms, with or without modification, are permitted provided that the following conditions are met:\n" +
                "\n" +
                "   •   Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.\n" +
                "\n" +
                "   •   Neither the name of KS Technologies, LLC nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission of KS Technologies, LLC.\n" +
                "\n" +
                "   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n" ;
        tv_nirsdk.setText(sdk_license);

        String charts_license = "   Copyright (c) 2014 Philipp Jahoda <philjay.librarysup@gmail.com>\n" +
                "\n" +
                "   Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                "   you may not use this file except in compliance with the License.\n" +
                "   You may obtain a copy of the License at\n" +
                "\n" +
                "    http://www.apache.org/licenses/LICENSE-2.0\n" +
                "\n" +
                "   Unless required by applicable law or agreed to in writing, software\n" +
                "   distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                "   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                "    See the License for the specific language governing permissions and\n" +
                "   limitations under the License.";
        tv_charts.setText(charts_license);
    }


    /*
       * On resume, make a call to the super class.
       * Nothing else is needed here besides calling
       * the super method.
       */
    @Override
    public void onResume() {
        super.onResume();
    }

    /*
     * When the activity is destroyed, unregister the BroadcastReceivers
     * handling receiving scan configurations, disconnect events, the # of configurations,
     * and the active configuration
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /*
     * Inflate the options menu
     * In this case, there is no menu and only an up indicator,
     * so the function should always return true.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    /*
     * Handle the selection of a menu item.
     * In this case, there is only the up indicator. If selected, this activity should finish.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
