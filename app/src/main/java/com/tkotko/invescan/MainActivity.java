package com.tkotko.invescan;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.content.*;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.tkotko.invescan.ERPXmlParser.Entry;
import android.database.*;
import java.text.*;
import java.util.Locale;

import android.database.sqlite.*;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

	//int iReturn=0;
    private Activity mainactivity;
	private IntentIntegrator integrator;
	private ListView listView;
    //private String[] list = {"鉛筆","原子筆","鋼筆","毛筆","彩色筆"};
    private ArrayAdapter<String> listAdapter;

    private String url ="http://services.hanselandpetal.com/feeds/flowers.json";
    private ArrayList<String> assetsList= new ArrayList<>();
    private String ws_result;
    private TextView tvScanCode;
    private TextView tvScanCode2;
    private EditText edtScanInput;
    private TextView tvLogin;
    private TextView tvLoginID;

    private ProgressDialog waiting_dialog;
    private String strErrorMsg;

    private SharedPreferences spref;

    // 資料庫名稱
    private static final String DB_NAME = "inventory.db";
    // 資料庫版本，資料結構改變的時候要更改這個數字，通常是加一
    private static final int DB_VERSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mainactivity=this;
        edtScanInput = (EditText) findViewById(R.id.edtScanInput);
        tvScanCode = (TextView) findViewById(R.id.return_code_1);
        tvScanCode2 = (TextView) findViewById(R.id.return_code_2);
        listView = (ListView)findViewById(R.id.ws_Listview);

        SQLiteHelper helper = new SQLiteHelper(this, DB_NAME, null, DB_VERSION);
        final SQLiteDatabase db = helper.getWritableDatabase();

		//查詢 SQLite 取出資料放到 ListView ====================================================================
        /*
		//方法一:使用db.query,並搭配ArrayList
		//Cursor c = db.query("inve", new String[]{"barcode", "barcode_format"},
		Cursor c = db.query("inve", null,
							"_id >= ? and _id <= ?", new String[]{"1", "999"}, null, null, "_id desc");
		int row = c.getCount();
		int col = c.getColumnCount();
		String row_value;
		c.moveToFirst();

		for (int i = 0; i < row; i++) {
			row_value="";
			for(int j = 0; j < col; j++){
				if(j==0){
					row_value=c.getString(j);
				}else{
					row_value=row_value+" "+c.getString(j);
				}
			}
			assetsList.add(row_value);
			c.moveToNext();
		}
		c.close();
		listAdapter = new ArrayAdapter<>(MainActivity.this,android.R.layout.simple_list_item_1,assetsList);
		listView.setAdapter(listAdapter);
		*/

        //方法二:使用db.rawQuery,並搭配 SimpleCursorAdapter
        //Cursor cursor = db.rawQuery("select _id,_id||' '||barcode ibarcode, ins_date from inve",null);
        Cursor cursor = db.rawQuery("select _id,barcode,ins_date,ins_user,input_type from inve",null);
        if (cursor != null && cursor.getCount() >= 0) {
            //1.使用android內建的 ListView
            //SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, cursor,
            //        new String[]{"ibarcode", "ins_date"}, new int[]{android.R.id.text1, android.R.id.text2}, 0);
            //2.使用自訂的 ListView
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.inve_list, cursor,
                    new String[]{"_id","barcode", "ins_date", "ins_user", "input_type"},
                    new int[]{R.id.db_id, R.id.db_barcode,R.id.db_ins_date,R.id.db_ins_user,R.id.db_input_type}, 0);
            listView.setAdapter(adapter);

            //cursor.close();  //不能直接close,因為已指派給adapter
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();

                /*
				//方法1:直接呼叫zxing app
				Intent intent = new Intent("com.google.zxing.client.android.SCAN");
				intent.putExtra("SCAN_MODE","QR_CODE_MODE");
				startActivityForResult(intent,1);
                */
				
				//方法2:整合zxing
				integrator = new IntentIntegrator(mainactivity);
                initiateScanning(integrator);
				
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                File file = new File(getApplicationInfo().dataDir + "/shared_prefs", "userInfo.xml");
                if (file.exists()) {
                    spref = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
                    tvLoginID.setText(spref.getString("USER_EMAIL",""));
                    tvLogin.setText(R.string.logout);
                }else{
                    tvLogin.setText(R.string.login);
                }
            }
        };
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        tvLogin = (TextView) navigationView.getHeaderView(0).findViewById(R.id.tvLogin);
        //tvLogin = (TextView) View.inflate(this,R.layout.nav_header_main,null).findViewById(R.id.tvLogin);
        tvLoginID = (TextView) navigationView.getHeaderView(0).findViewById(R.id.tvLoginID);
        //判斷Login SharedPreferences檔是否存在
        File file = new File(getApplicationInfo().dataDir + "/shared_prefs", "userInfo.xml");
        if (file.exists()) {
            spref = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
            tvLoginID.setText(spref.getString("USER_EMAIL",""));
            tvLogin.setText(R.string.logout);
        }else{
            tvLogin.setText(R.string.login);
        }
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //spref = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
                //SharedPreferences.Editor editor =spref.edit();
                //editor.putString("USER_EMAIL", "");
                //editor.commit();
                File file = new File(getApplicationInfo().dataDir + "/shared_prefs", "userInfo.xml");
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (deleted){
                        tvLogin.setText(R.string.login);
                        tvLoginID.setText("");
                        Toast.makeText(mainactivity, "帳號已登出", Toast.LENGTH_LONG).show();
                    }
                }else{
                    Intent i = new Intent(mainactivity,LoginActivity.class);
                    startActivity(i);
                }
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);
            }
        });


        //進入app時不要自動跳出鍵盤
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        //EditText輸入後按Enter鍵,提供可手動新增的功能
        edtScanInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        edtScanInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                //當actionId == XX_SEND 或 XX_DONE 時觸發
                //或是 event.getKeyCode == ENTER 且 event.getAction == ACTION_DOWN 時也觸發
                //還要判斷 event != null ,因為某些輸入法會返回null
                if (actionId == EditorInfo.IME_ACTION_SEND
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && KeyEvent.KEYCODE_ENTER == event.getKeyCode() && KeyEvent.ACTION_DOWN == event.getAction())) {
                    if (edtScanInput.getText() != null){
                        //比對ListView中有無重覆資料
                        int iCount=0;
                        for (int i=0; i<assetsList.size(); i++){
                            //compareTo 比對相同回傳0,不同回傳-11
                            if (assetsList.get(i).compareTo(edtScanInput.getText().toString()) == 0){
                                iCount += 1;
                            }
                        }
                        if (iCount==0){  //無重覆
                            //加入ListView
                            assetsList.add(edtScanInput.getText().toString());
                            listAdapter = new ArrayAdapter<>(MainActivity.this,android.R.layout.simple_list_item_1,assetsList);
                            listView.setAdapter(listAdapter);

                            //寫入SQLite
                            ContentValues values = new ContentValues();
                            values.put("barcode", edtScanInput.getText().toString());
                            values.put("input_type", "M");
                            spref = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
                            values.put("ins_user", spref.getString("USER_EMAIL",""));
                            
							Date dt=new Date();
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
                            values.put("ins_date", sdf.format(dt));
                            try {
                                long id = db.insert("inve", null, values);
                                tvScanCode2.setText(String.valueOf(id));
                            } catch (SQLiteException e) {
                                e.printStackTrace();
                                Toast.makeText(mainactivity, e.toString(), Toast.LENGTH_LONG).show();
                            }

                            //ListView判斷有無資料
                            if (!assetsList.isEmpty()){
                                //tvScanCode2.setText(String.valueOf(listView.getAdapter().getCount()));
                            }
                        }else{
                            Toast.makeText(mainactivity, "重複輸入", Toast.LENGTH_LONG).show();
                        }

                        return true;
                    }
                }
                return false;
            }
        });

        /*
        //ListView 範例
        listAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,list);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					Toast.makeText(getApplicationContext(), "你選擇的是" + list[position], Toast.LENGTH_SHORT).show();
				}
			});
			*/
    }

	private void initiateScanning(IntentIntegrator integrator){
        //SharedPreferences sp = getSharedPreferences("pref_general",0);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        //掃描方向(Zxing預設只能橫向)
        String str_orientation = prefs.getString("list_scan_orientation","-1");
        switch (str_orientation){
            case "1":  //直向
                integrator.setCaptureActivity(CaptureActivityVerticalOrientation.class);
                integrator.setOrientationLocked(false);
                break;
            case "0":  //橫向
                break;
            case "-1":  //自動旋轉(依感應器)
                integrator.setCaptureActivity(CaptureActivityAnyOrientation.class);
                integrator.setOrientationLocked(false);
        }

        //指定允許掃描的barcode格式
		integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        //掃描視窗下排顯示的文字
		integrator.setPrompt("請將條碼置於框中掃描");
        // Use a specific camera of the device
		integrator.setCameraId(0);

		//是否播放提示音
        integrator.setBeepEnabled(false);
        boolean sw_beep = prefs.getBoolean("swh_scan_beep",false);
        if (sw_beep) {
            integrator.setBeepEnabled(true);
        }

		//是否保留掃碼成功時的截圖
		integrator.setBarcodeImageEnabled(false);
		integrator.initiateScan();
	}
	
	// 接收 ZXing 掃描後回傳來的結果
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        /*
		//方法1:直接呼叫zxing app
        if(requestCode==1)
        {
            if(resultCode==RESULT_OK)
            {
                // ZXing回傳的內容
				iReturn=iReturn+1;
                String contents = intent.getStringExtra("SCAN_RESULT");
				if (iReturn==1)
				{
					TextView textView1 = (TextView) findViewById(R.id.return_code_1);
					textView1.setText(contents);
				}
				if (iReturn==2)
				{
					TextView textView2 = (TextView) findViewById(R.id.return_code_2);
					textView2.setText(contents);
				}
				
				//連續掃描
				Intent intent2 = new Intent("com.google.zxing.client.android.SCAN");
				intent2.putExtra("SCAN_FORMATS", "PRODUCT_MODE,CODE_39,CODE_93,CODE_128,DATA_MATRIX,ITF");
				startActivityForResult(intent2, 1);
            }
            else
            if(resultCode==RESULT_CANCELED)
            {
                Toast.makeText(this, "取消掃描", Toast.LENGTH_LONG).show();
            }
        }
        */

		//方法2:整合zxing
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if(scanResult.getContents() != null){
            String scanContent=scanResult.getContents();
            String scanFormat=scanResult.getFormatName();

            edtScanInput.setText(scanContent);
            tvScanCode.setText(scanFormat);

            int iCount=0;
            for (int i=0; i<assetsList.size(); i++){
                //compareTo 比對相同回傳0,不同回傳-11
                if (assetsList.get(i).compareTo(edtScanInput.getText().toString()) == 0){
                    iCount += 1;
                }
            }
            if (iCount==0){
                assetsList.add(edtScanInput.getText().toString());
                listAdapter = new ArrayAdapter<>(MainActivity.this,android.R.layout.simple_list_item_1,assetsList);
                listView.setAdapter(listAdapter);
                Toast.makeText(mainactivity, scanResult.getContents(), Toast.LENGTH_LONG).show();


                if (!assetsList.isEmpty()){
                    tvScanCode2.setText(String.valueOf(listView.getAdapter().getCount()));
                }
            }else{
                Toast.makeText(mainactivity, "重複掃描", Toast.LENGTH_LONG).show();
            }

			//如果在調用掃碼的時候setBarcodeImageEnabled(true)  
            //通過下面的方法獲取截圖的路徑
            //String imgPath = scanResult.getBarcodeImagePath();
			//textView2.setText(imgPath);
			
			//連續掃描
            IntentIntegrator integrator2 = new IntentIntegrator(mainactivity);
            initiateScanning(integrator2);
        }else{
            Toast.makeText(getApplicationContext(),"取消掃描",Toast.LENGTH_SHORT).show();
        }
    }
	
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //return true;
			Intent i = new Intent(this,SettingsActivity.class);
			startActivity(i);
			
        }else if (id == R.id.action_clear) {
            edtScanInput.setText("");
            tvScanCode.setText("");
            tvScanCode2.setText("");
            assetsList.clear();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_login) {
            Intent i = new Intent(this,LoginActivity.class);
            startActivity(i);

        } else if (id == R.id.nav_http) {
            new wsGetHTTP().execute(url);

        } else if (id == R.id.nav_download) {
            new wsGetERP().execute();

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private class wsGetHTTP extends AsyncTask<String,String,String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            return HttpURLConnect.getData(url);
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                JSONArray ar = new JSONArray(s);
                for (int i=0; i<ar.length(); i++){
                    JSONObject jsonobject = ar.getJSONObject(i);
                    //Flowers  flowers = new Flowers();
                    //flowers.setName(jsonobject.getString("name"));
                    assetsList.add(jsonobject.getString("name"));
                }
            }
            catch (JSONException e){
                e.printStackTrace();
            }
            //FlowerAdapter adapter = new FlowerAdapter(Fetch.this, R.layout.flowers_list_items, flowersList);
            //lv.setAdapter(adapter);
            //listAdapter = new ArrayAdapter<>(getApplicationContext(),android.R.layout.simple_list_item_1,assetsList);
            listAdapter = new ArrayAdapter<>(MainActivity.this,android.R.layout.simple_list_item_1,assetsList);
            listView.setAdapter(listAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Toast.makeText(getApplicationContext(), "你選擇的是" + assetsList.get(position), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private class wsGetERP extends AsyncTask<String,String,String> {

        @Override
        protected void onPreExecute() {
            //super.onPreExecute();
            waiting_dialog = ProgressDialog.show(MainActivity.this, "請稍後", "資料更新中", true,true,
                    new DialogInterface.OnCancelListener(){
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            wsGetERP.this.cancel(true);
                        }
                    }
                    );
            assetsList.clear();
        }

        @Override
        protected String doInBackground(String... strings) {
            final String NAMESPACE = "http://www.dsc.com.tw/tiptop/TIPTOPServiceGateWay" ;
            final String URL = "http://192.168.28.222:6394/ws/r/aws_ttsrv2?WSDL";
            final String SOAP_ACTION = "http://www.dsc.com.tw/tiptop/TIPTOPServiceGateWay/GetItemData";
            //final String METHOD_NAME = "GetItemData";
            final String METHOD_NAME = "GetAssetData";
            InputStream stream = null;
            List<Entry> entries = null;
            StringBuilder parString = new StringBuilder();

            strErrorMsg="";
            //ERP Web Service
			SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
			//String request_xml="<Request> <Access> <Authentication user=\"tiptop\" password=\"tiptop\" /> <Connection application=\"InveScan\" source=\"192.168.19.16\" />	<Organization name=\"FORMAL_TW\" />	<Locale language=\"zh_tw\" /> </Access> <RequestContent> <Parameter> <Record> <Field name=\"condition\" value=\" ima01 like '85201-03%'\"/> </Record> </Parameter> </RequestContent> </Request>";
            String request_xml="<Request> <Access> <Authentication user=\"tiptop\" password=\"tiptop\" /> <Connection application=\"InveScan\" source=\"192.168.19.16\" />	<Organization name=\"FORMAL_TW\" />	<Locale language=\"zh_tw\" /> </Access> <RequestContent> <Parameter> <Record> <Field name=\"condition\" value=\" faj19 = 't00126'\"/> </Record> </Parameter> </RequestContent> </Request>";
			request.addProperty("request",request_xml);			//System.out.println("request=" + request);

			SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
			envelope.dotNet = false;
			envelope.setAddAdornments(false);
			envelope.setOutputSoapObject(request);
			HttpTransportSE ht = new HttpTransportSE(URL);
            try {
                ht.call(null, envelope);
                final SoapPrimitive response = (SoapPrimitive)envelope.getResponse();
                ws_result = response.toString();
                }
            /*
            catch (Exception e){
                e.printStackTrace();
                strErrorMsg = e.getMessage();
            }*/
            catch (SoapFault e) {
                e.printStackTrace();
                strErrorMsg = "SoapFault: " + e.getMessage();
            } catch (IOException e) {
                e.printStackTrace();
                strErrorMsg = "IOException: " + e.getMessage();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
                strErrorMsg = "XmlPullParserException: " + e.getMessage();
            }


            StringBuilder sb = new StringBuilder();
            sb.append("<Response>" + "\n"
                    +"  <Execution>" + "\n"
                    +"    <Status code=\"0\" sqlcode=\"0\" description=\"\"/>" + "\n"
                    +"  </Execution>" + "\n"
                    +"  <ResponseContent>" + "\n"
                    +"    <Parameter/>" + "\n"
                    +"    <Document>" + "\n"
                    +"      <RecordSet id=\"1\">" + "\n"
                    +"        <Master name=\"ima_file\">" + "\n"
                    +"          <Record>" + "\n"
                    +"            <Field name=\"ima01\" value=\"85201-0302\"/>" + "\n"
                    +"            <Field name=\"ima02\" value=\"1.0mm ZIF FPC Conn.\"/>" + "\n"
                    +"            <Field name=\"ima021\" value=\"SMT R/A T/C Type\"/>" + "\n"
                    +"          </Record>" + "\n"
                    +"        </Master>" + "\n"
                    +"      </RecordSet>" + "\n"
                    +"      <RecordSet id=\"2\">" + "\n"
                    +"        <Master name=\"ima_file\">" + "\n"
                    +"          <Record>" + "\n"
                    +"            <Field name=\"ima01\" value=\"85201-03051\"/>" + "\n"
                    +"            <Field name=\"ima02\" value=\"1.0mm ZIF FPC Conn.\"/>" + "\n"
                    +"            <Field name=\"ima021\" value=\"SMT R/A T/C Type\"/>" + "\n"
                    +"          </Record>" + "\n"
                    +"        </Master>" + "\n"
                    +"      </RecordSet>" + "\n"
                    +"    </Document>" + "\n"
                    +"  </ResponseContent>" + "\n"
                    +"</Response>"
            );

            if (ws_result == null){
                return "";
            }

            ERPXmlParser ERPXmlParser = new ERPXmlParser();
            try {
                //1,ERP WS
                stream = new ByteArrayInputStream(ws_result.getBytes(Charset.forName("UTF-8")));
                //2.測試資料
                //stream = new ByteArrayInputStream(sb.toString().getBytes(Charset.forName("UTF-8")));
                entries = ERPXmlParser.parse(stream);
                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            }
            catch (Exception e){
                e.printStackTrace();
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            if (entries != null) {
                for (Entry entry : entries) {
                    parString.delete( 0, parString.length() );
                    //parString.append("料號:"+entry.item_id);
                    //parString.append("\n品名:"+entry.item_name);
                    //parString.append("\n規格:"+entry.item_spec);
                    parString.append("財編:").append(entry.item_id);
                    parString.append("\n品名:").append(entry.item_name);
                    parString.append("\n部門:").append(entry.item_spec);
                    // If the user set the preference to include summary text,
                    // adds it to the display.
                    //if (pref) {
                    //    htmlString.append(entry.summary);
                    //}
                    assetsList.add(parString.toString());
                }
            }

            return parString.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            /*
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("tiptop ws回傳")
                    .setMessage(s.toString())
                    .setNeutralButton("確定", null)
                    .show();
            */
            if (s != "") {
                listAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, assetsList);
                listView.setAdapter(listAdapter);
            }else{
                if (strErrorMsg != ""){
                    Toast.makeText(getApplicationContext(), strErrorMsg, Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(getApplicationContext(), "取無資料或無法連結服務!!", Toast.LENGTH_SHORT).show();
                }
            }

            waiting_dialog.dismiss();
			
        }

        /*
        @Override
        protected void onCancelled() {
            waiting_dialog.cancel();
        }
        */
    }

    private void wsGetERP_old(){
        final String NAMESPACE = "http://www.dsc.com.tw/tiptop/TIPTOPServiceGateWay" ;
        final String URL = "http://192.168.28.222:6394/ws/r/aws_ttsrv2?WSDL";
        final String SOAP_ACTION = "http://www.dsc.com.tw/tiptop/TIPTOPServiceGateWay/GetItemData";
        final String METHOD_NAME = "GetItemData";

        new Thread(new Runnable(){

            @Override
            public void run()
            {
                try
                {
                    SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);

                    String request_xml="<Request> <Access> <Authentication user=\"tiptop\" password=\"tiptop\" /> <Connection application=\"\" source=\"192.168.1.2\" />	<Organization name=\"FORMAL_TW\" />	<Locale language=\"zh_tw\" /> </Access> <RequestContent> <Parameter> <Record> <Field name=\"condition\" value=\" ima01 = '85201-0302'\"/> </Record> </Parameter> </RequestContent> </Request>";
							/*
							StringBuilder sb = new StringBuilder();
							sb.append("&lt;Request>" + "\n"
									  + "&lt;Access>" + "\n"
									  + "&lt;Authentication user=\"tiptop\" password=\"tiptop\" />" + "\n"
									  + "&lt;Connection application=\"\" source=\"192.168.1.2\" />" + "\n"
									  + "&lt;Organization name=\"FORMAL_TW\" />" + "\n"
									  + "&lt;Locale language=\"zh_tw\" />" + "\n"
									  + "&lt;/Access>" + "\n"
									  + "&lt;RequestContent>" + "\n"
									  + "&lt;Parameter>" + "\n"
									  + "&lt;Record>" + "\n"
									  + "&lt;Field name=\"condition\" value=\" ima01 = '85201-0302'\"/>" + "\n"
									  + "&lt;/Record>" + "\n"
									  + "&lt;/Parameter>" + "\n"
									  + "&lt;/RequestContent>" + "\n"
									  + "&lt;/Request>"
									  );
							*/

                    request.addProperty("request",request_xml);
                    //request.addProperty("request", sb.toString());

							/*
							PropertyInfo pi = new PropertyInfo();
							pi.setNamespace("http://www.dsc.com.tw/tiptop/TIPTOPServiceGateWay");
							pi.setNamespace("tip");
							pi.setName("request");
							pi.setValue(request_xml);
							request.addProperty(pi);
							*/

                    System.out.println("request=" + request);

                    SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                    envelope.dotNet = false;
                    envelope.setAddAdornments(false);

                    //SoapFault error_out = (SoapFault)envelope.bodyOut;
                    //System.out.println("error_out="+error_out.toString());

                    //envelope.bodyOut = request;
                    envelope.setOutputSoapObject(request);
                    HttpTransportSE ht = new HttpTransportSE(URL);
                    ht.debug = true;
                    ht.call(null, envelope);

                    System.out.println("debug=" + ht.requestDump);
                    System.out.println("debug=" + ht.responseDump);

                    //SoapFault error = (SoapFault)envelope.bodyIn;
                    //System.out.println("error=" + error.toString());

                    final SoapPrimitive response = (SoapPrimitive)envelope.getResponse();
                    //SoapObject result = (SoapObject)envelope.bodyIn;
                    //SoapObject detail = (SoapObject)result.getProperty("GetItemDataResponse");
                    //String data=detail.getProperty(1).toString();

                    //Log.d(logcat_tag,"response="+response.toString());
                    //Log.d(logcat_tag, "result=" + result.toString());
                    //Log.d(logcat_tag, "detail=" + detail.toString());
                    //Log.d(logcat_tag, "data=" + data.toString());

                    runOnUiThread (new Runnable(){
                        public void run() {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("tiptop ws回傳")
                                    .setMessage(response.toString())
                                    .setNeutralButton("確定", null)
                                    .show();
                        }
                    });
                }
                catch (Exception e)
                {
                    Log.e("tkotko", e.toString());
                }
            }


        }).start();
    }

}
