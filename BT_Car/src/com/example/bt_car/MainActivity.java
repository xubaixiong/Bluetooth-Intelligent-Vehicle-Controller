package com.example.bt_car;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.Manifest;
import android.R.bool;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements OnTouchListener,OnCheckedChangeListener{
	//宏
	public final int CONNECT_FAILED = 0;
	public final int CONNECT_SUCCESS = 1;
	public final int READ_FAILED = 2;
	public final int WRITE_FAILED = 3;
	public final int DATA = 4;
	static final int CMD_SEND_DATA    = 0x04;
	private String bluetoothAddr = "20:16:04:18:55:23";/*蓝牙设备地址*/
	//组件
	Button sensorMode;
	ToggleButton remoteMode;
	MySurfaceView mySurfaceView;
	//全局变量
	public boolean isConnecting,pairing=false;
	private BluetoothAdapter bluetoothAdapter;
	BroadcastReceiver mReceiver;
	BluetoothSocket socket=null;
	DirSensor sensor;
    //以下方法
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//组件初始化
		widgetInit();
		//重力感应初始化
		sensor = new DirSensor(MainActivity.this);
		//获取蓝牙适配器
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		//判断有否打开蓝牙
		if(!bluetoothAdapter.isEnabled()){
			//不做提示，强行打开
			bluetoothAdapter.enable();
		}		
		//安卓6.0获取扫描权限
		getScanPermission();
		//注册广播
		registerBTBroadcast();
		//开启扫描
		scanOrNot(true);
		showToast("开始扫描小车...");	
	}
	private void widgetInit() {
		//组件实例化
		mySurfaceView = (MySurfaceView) findViewById(R.id.gameView);
		sensorMode = (Button) findViewById(R.id.btn_sensor);
		remoteMode = (ToggleButton) findViewById(R.id.remoteMode);
		//点击监听
		sensorMode.setOnTouchListener(this);
		//未连接前禁用遥控模式按钮
		remoteMode.setOnCheckedChangeListener(this);
		remoteMode.setClickable(false);
	}
	private void getScanPermission() {
		//判断是否有权限
		if (ContextCompat.checkSelfPermission(this,
		        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			//请求权限
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},1);
			//判断是否需要 向用户解释，为什么要申请该权限
			if(ActivityCompat.shouldShowRequestPermissionRationale(this,
			        Manifest.permission.READ_CONTACTS)) {
			    Toast.makeText(this, "shouldShowRequestPermissionRationale", Toast.LENGTH_SHORT).show();
			}
		}
	}
	public void registerBTBroadcast() {
		mReceiver = new BroadcastReceiver() {  		    
			public void onReceive(Context context, Intent intent) {  
		        String action = intent.getAction();  
		        //找到设备  
		        if (BluetoothDevice.ACTION_FOUND.equals(action)) {  
		            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);  
		            Log.e("BT_scan", "扫描到蓝牙设备:(" + device.getName()  
		                        +"->"+ device.getAddress()+")");
		            //如果设备是小车的蓝牙
		            if (device.getAddress().equals(bluetoothAddr)) {
		            	Log.e("BT_scan", "扫描到小车");
		            	// 搜索蓝牙设备的过程占用资源比较多，停止扫描
		            	scanOrNot(false);
		            	// 获取蓝牙设备的配对状态		     
		            	switch (device.getBondState()) {
			            	// 未配对
			            	case BluetoothDevice.BOND_NONE:
			            		Log.e("BT_bond", "蓝牙仍未配对，配对开始！");
				            	try {
					            	Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
					            	boolean pairReturn = (Boolean) createBondMethod.invoke(device);
					            	pairing = true;
				            	} catch (Exception e) {
				            		e.printStackTrace();
				            	}
				            	break;
			            	// 已配对
			            	case BluetoothDevice.BOND_BONDED:
			            		Log.e("BT_bond", "蓝牙已经配对，连接开始！");
				            	try {
					            	// 连接
					            	connect(device);
				            	} catch (IOException e) {
				            		e.printStackTrace();
				            	}
				            	break;
				            // 配对中
			            	case BluetoothDevice.BOND_BONDING:
			            		Log.e("BT_bond", "蓝牙在配对的过程(Pairing)！");
			            		break;
		            	}
					}
		        }  
		        //搜索完成  
		        else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED  
		                .equals(action)) {  
		            if (!isConnecting) {
		            	scanOrNot(true);
		            }
		        }
		        //成功连接
		        else if (BluetoothDevice.ACTION_ACL_CONNECTED  
		                .equals(action)) {  
		        	connectedHandle();
		        } 
		        //连接断开
		        else if (BluetoothDevice.ACTION_ACL_DISCONNECTED  
		                .equals(action)) {  
		        	disconnectedHandle();		        	
		        } 
		        //绑定状态改变
		        else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED  
		                .equals(action)) {  
		        	int cur_bond_state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
		        	String state = null;
		        	if (cur_bond_state==BluetoothDevice.BOND_BONDED) {
		        		state = "配对成功！";
					}else if (cur_bond_state==BluetoothDevice.BOND_BONDING){
						state = "正在配对...";
					}else if (cur_bond_state==BluetoothDevice.BOND_NONE){
						state = "配对无效";
					}
		        	Toast.makeText(MainActivity.this,state, Toast.LENGTH_SHORT)
					.show();		
		        }
		        /*遥杆状态改变*/
		        else if (action.equals("com.example.bt_car.mysurfaceview") ){
					int cmd = intent.getIntExtra("cmd", -1);
					int value = intent.getIntExtra("value", -1);
					if (cmd==CMD_SEND_DATA&&isConnecting) {
						sendMessage(String.valueOf(value));
					}
				}
		        /*重力感应状态改变*/
		        else if (action.equals("com.example.bt_car.dirsensor") ){
		        	//Log.e("BT_recvSensor", "!!!!!!");
					int cmd = intent.getIntExtra("cmd", -1);
					float x = intent.getFloatExtra("x", -1);
					float y = intent.getFloatExtra("y", -1);
					mySurfaceView.setRockPosition(x*60, y*60);
					//防止重复发送
					if (cmd==1&&isConnecting) {					
						int logicStatus = intent.getIntExtra("logicStatus", -1);
						sendMessage(String.valueOf(logicStatus));
					}
				}
		    }  
		};  

		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND); 
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		filter.addAction("com.example.bt_car.mysurfaceview");
		filter.addAction("com.example.bt_car.dirsensor");
		registerReceiver(mReceiver, filter);  
	}
	private void connect(BluetoothDevice device) throws IOException {
		// 固定的UUID
		final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
		UUID uuid = UUID.fromString(SPP_UUID);
		socket = device.createRfcommSocketToServiceRecord(uuid);
		socket.connect();
	}
	// 定义发送函数
	public void sendMessage(String message) {
		OutputStream outputstream = null;
		if (socket==null||isConnecting==false) {
			Toast.makeText(MainActivity.this, "连接未建立", Toast.LENGTH_SHORT)
			.show();
			return;
		}
		try {
			outputstream = socket.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(MainActivity.this, "客户端输出流建立失败", Toast.LENGTH_SHORT)
					.show();
		}
		try {
			outputstream.write(message.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(MainActivity.this, "发送数据失败", Toast.LENGTH_SHORT)
					.show();
		}
	}
	//状态变为已连接
	public void connectedHandle() {
		Log.d("BT_connect", "触发连接成功！"); 
		showToast("连接成功！");
		//停止扫描
    	scanOrNot(false);
		isConnecting = true;
		remoteMode.setClickable(true);
		setTitle("小车已连接"); 
	}
	//状态变为连接断开
	public void disconnectedHandle() {
		showToast("小车连接断开");
		Log.d("BT_connect", "触发连接断开！"); 
		isConnecting = false;
		remoteMode.setChecked(false);
		remoteMode.setClickable(false);
		setTitle("小车连接断开,重新扫描"); 
		//开启扫描
		scanOrNot(true);
		
	}
	public void scanOrNot(boolean trueOrNot) {
        if (trueOrNot) {
        	//获取蓝牙适配器
    		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    		//判断有否打开蓝牙
    		if(!bluetoothAdapter.isEnabled()){
    			//不做提示，强行打开
    			bluetoothAdapter.enable();
    		}
    		//开启扫描
    		bluetoothAdapter.startDiscovery();
    		setTitle("正在扫描");
		}else if(bluetoothAdapter.isDiscovering()){
			bluetoothAdapter.cancelDiscovery();
			setTitle("扫描停止");
		}
    }
	public void showToast(String showString) {
		Toast.makeText(MainActivity.this, showString, Toast.LENGTH_SHORT).show();
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
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		//获取蓝牙适配器
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		//判断有否打开蓝牙
		if(!bluetoothAdapter.isEnabled()){
			//不做提示，强行打开
			bluetoothAdapter.enable();
		}
	}
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.btn_sensor:
			if(event.getAction() == MotionEvent.ACTION_DOWN/*&&isConnecting*/){   
				//sendMessage("1");
				sensor.sensorListenerStart();
			}
			else if(event.getAction()==MotionEvent.ACTION_UP/*&&isConnecting*/){
				//sendMessage("0");
				sensor.sensorListenerStop();
			} 
			break;	
		}		
		return true;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		if (buttonView.getId()==R.id.remoteMode) {
			if (isChecked) {
				if (isConnecting) {
					//进入遥控模式
					sendMessage("9"); 
					Log.e("BT_remote", "进入遥控模式");
				}				
			}else {
				if (isConnecting) {
					//退出遥控模式
					sendMessage("a");
					Log.e("BT_remote", "退出遥控模式");
				}			
			}			
		}
	}
	//再按一次退出程序
	private long exitTime = 0;
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			if ((System.currentTimeMillis() - exitTime) > 2000) {
				Toast.makeText(getApplicationContext(), "再按一次退出本界面",
						Toast.LENGTH_SHORT).show();
				exitTime = System.currentTimeMillis();
			} else {			
				finish();
				System.exit(0);
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
	
