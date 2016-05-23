package com.example.bt_car;

import android.R.integer;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class DirSensor {
	//宏
	private final int LOGIC_STOP   = 0x00;
	private final int LOGIC_UP     = 0x01;
	private final int LOGIC_DOWN   = 0x02;
	private final int LOGIC_LEFT   = 0x03;
	private final int LOGIC_RIGHT  = 0x04;
	private final int LOGIC_ULEFT  = 0x05;
	private final int LOGIC_URIGHT = 0x06;
	private final int LOGIC_DLEFT  = 0x07;
	private final int LOGIC_DRIGHT = 0x08;
	private final float YUZHI = (float) 1.8;
	//全局变量
	public int mLogicStatus;
	private Context context;
	private SensorManager sensorMag;
	private Sensor gravitySensor;
	private SensorEventListener sensorLis = new SensorEventListener() {
		
		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			float x = event.values[SensorManager.DATA_X];   
			float y = event.values[SensorManager.DATA_Y];   
			float z = event.values[SensorManager.DATA_Z];   
			Log.e("BT_sensor","x="+(int)x+","+"y="+(int)y+","+"z="+(int)z); 
			float absX = Math.abs(x);
			float absY = Math.abs(y);
			int currentLogic = 0;
			if (absX<YUZHI&&absY<YUZHI) {
				currentLogic = LOGIC_STOP;
			}else if (absX<YUZHI){/*前后*/
				if (y>=YUZHI) {
					currentLogic = LOGIC_DOWN;
				}else {
					currentLogic = LOGIC_UP;
				}			
			}else if (absY<YUZHI){/*左右*/
				if (x>=YUZHI) {
					currentLogic = LOGIC_LEFT;
				}else {
					currentLogic = LOGIC_RIGHT;
				}			
			}else if (y<-YUZHI) {/*前左右*/
				if (x>=YUZHI) {
					currentLogic = LOGIC_ULEFT;
				}else {
					currentLogic = LOGIC_URIGHT;
				}	
			}else if (y>YUZHI) {/*后左右*/
				if (x>=YUZHI) {
					currentLogic = LOGIC_DLEFT;
				}else {
					currentLogic = LOGIC_DRIGHT;					
				}	
			}
			mSendBroadcast(currentLogic, x, y);
		}
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}
	};
	public DirSensor(Context context) {
		this.context = context;
		sensorMag = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		gravitySensor = sensorMag.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);		
	}
	//开启重力传感器监听
	public void sensorListenerStart() {
		if (sensorMag!=null&&gravitySensor!=null) {
			sensorMag.registerListener(sensorLis, gravitySensor,
					SensorManager.SENSOR_DELAY_UI);
		}else {
			Log.e("BT_sensor", "开启重力传感器监听失败(传感器实例化异常)");
		}		
	}
	//停止重力传感器监听
	public void sensorListenerStop() {
		if (sensorMag!=null&&gravitySensor!=null) {
			sensorMag.unregisterListener(sensorLis);
			mSendBroadcast(LOGIC_STOP,0,0);
		}else {
			Log.e("BT_sensor", "停止重力传感器监听失败(传感器实例化异常)");
		}		
	}
	/*发送广播*/
	public void mSendBroadcast(int logicStatus, float x, float y) {
		Intent intent = new Intent();
		intent.setAction("com.example.bt_car.dirsensor");
		if (mLogicStatus != logicStatus) {
			intent.putExtra("cmd", 1);
			mLogicStatus = logicStatus;
		}else {
			intent.putExtra("cmd", 0);
		}
		intent.putExtra("logicStatus", logicStatus);
		intent.putExtra("x", x); 
		intent.putExtra("y", y); 
		context.sendBroadcast(intent);			
	}
	
}
