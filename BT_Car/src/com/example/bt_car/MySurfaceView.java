/**
 * 
 */
package com.example.bt_car;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

/**
 * @author winder
 *
 */
public class MySurfaceView extends SurfaceView implements Callback, Runnable {
	private static final String TAG = "MySurfaceView";
	private SurfaceHolder mHolder;
	private Paint mPaint;
	private Thread mThread;
	private boolean mFlag;
	private Canvas mCanvas;
	public double mRad, mAngle;
	public int mLogicType, mLogicStatus;
	public Context mContext;
	
	private int mRockCentX, mRockCentY, mRockRadius;
	private int mBaseCentX, mBaseCentY, mBaseRadius;
	
	private final int LOGIC_STOP   = 0x00;
	private final int LOGIC_UP     = 0x01;
	private final int LOGIC_DOWN   = 0x02;
	private final int LOGIC_LEFT   = 0x03;
	private final int LOGIC_RIGHT  = 0x04;
	private final int LOGIC_ULEFT  = 0x05;
	private final int LOGIC_URIGHT = 0x06;
	private final int LOGIC_DLEFT  = 0x07;
	private final int LOGIC_DRIGHT = 0x08;
	
	public MySurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
		Log.e("MySurfaceView1", "x:"+this.getWidth()+",y:"+this.getHeight());
		mContext = context;

		mRockRadius = 100;
		mBaseRadius = 300;
		
		mLogicStatus = -1;
		mHolder = this.getHolder();
		mHolder.addCallback(this);
		mPaint = new Paint();
		mPaint.setColor(Color.BLUE);
		mPaint.setAntiAlias(true);
		setFocusable(true);
	}

	public MySurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		Log.e("MySurfaceView2", "x:"+this.getWidth()+",y:"+this.getHeight());
		mContext = context;
		
		//mRockCentX  = mBaseCentX = this.getWidth()/2;//540;
		//mRockCentY  = mBaseCentY = this.getHeight()/2;//1300;
		mRockRadius = 100;
		mBaseRadius = 300;
		
		mLogicStatus = -1;
		mHolder = this.getHolder();
		mHolder.addCallback(this);
		mPaint = new Paint();
		mPaint.setColor(Color.BLUE);
		mPaint.setAntiAlias(true);
		setFocusable(true);
	}
	
	/**
	 * Constructor to initialize the size
	 * @param context
	 * @param x
	 * @param y
	 * @param r
	 */
	public MySurfaceView(Context context, int x, int y, int r) {
		super(context);
		mContext = context;
		mRockCentX  = mBaseCentX = x;
		mRockCentY  = mBaseCentY = y;
		mRockRadius = r;
		mBaseRadius = r * 3;
		
		mLogicStatus = -1;
		mHolder = this.getHolder();
		mHolder.addCallback(this);
		mPaint = new Paint();
		mPaint.setColor(Color.BLUE);
		mPaint.setAntiAlias(true);
		setFocusable(true);
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mRockCentX  = mBaseCentX = this.getWidth()/2;//540;
		mRockCentY  = mBaseCentY = this.getHeight()/2;//1300;
		mFlag = true;
		/* Setup thread to handle events and plain canvas */
		mThread = new Thread(this);
		mThread.start();
	}
	/*设置遥杆的相对圆心位置，圆心为(0,0)*/
	public void setRockPosition(float pointX, float pointY) {
		float pointDx = (mBaseCentX - pointX);
		float pointDy = (mBaseCentY + pointY);
		Log.e("BT_setRockPosition", "pointDx:"+pointDx+"pointDy:"+pointDy);
		double pointR = Math.sqrt(pointX * pointX + pointY * pointY);
		if ( pointR <= mBaseRadius ) {
			mRockCentX = (int) pointDx;
			mRockCentY = (int) pointDy;
		}
		else {
			mRockCentX = mBaseCentX - (int) (mBaseRadius * pointX / pointR);
			mRockCentY = mBaseCentY + (int) (mBaseRadius * pointY / pointR);
		}
		Log.i(TAG, "Set position: " + mRockCentX + "|" + mRockCentY);
	}
	
	/*判断遥杆的角度属于8向中的哪个方向*/
	int setLogicType(double angle) {
		int type = -1;
		if (angle > 22 && angle <= 67) {/*右前*/
			type = LOGIC_URIGHT;
		}
		else if (angle > 67 && angle <= 112) {/*前*/
			type = LOGIC_UP;
		}
		else if (angle > 112 && angle <= 157) {/*左前*/
			type = LOGIC_ULEFT;
		}
		else if ((angle > 157 && angle <= 180)||(angle > -180 && angle <= -157)) {/*左*/
			type = LOGIC_LEFT;
		}
		else if (angle > -157 && angle <= -112) {/*左后*/
			type = LOGIC_DLEFT;
		}
		else if (angle > -112 && angle <= -67) {/*后*/
			type = LOGIC_DOWN;
		}
		else if (angle > -67 && angle <= -22) {/*右后*/
			type = LOGIC_DRIGHT;
		}
		else if ((angle >= 0 && angle <= 22)||(angle > -22 && angle < 0)) {/*右*/
			type = LOGIC_RIGHT;
		}
		return type;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		/* Reset rocker when touch up */
		if (event.getAction() == MotionEvent.ACTION_UP) {
			mRockCentX = mBaseCentX;
			mRockCentY = mBaseCentY;
			mLogicType = LOGIC_STOP;
		} else {
//			setRockPosition(event.getX(), event.getY());
			float pointDx = event.getX() - mBaseCentX;
			float pointDy = event.getY() - mBaseCentY;
			double pointR = Math.sqrt(pointDx * pointDx + pointDy * pointDy);
			if ( pointR <= mBaseRadius ) {
				mRockCentX = (int) event.getX();
				mRockCentY = (int) event.getY();
			}
			else {
				mRockCentX = mBaseCentX + (int) (mBaseRadius * pointDx / pointR);
				mRockCentY = mBaseCentY + (int) (mBaseRadius * pointDy / pointR);
			}
			
			mRad = Math.acos(pointDx / pointR);	
			if ( event.getY() > mBaseCentY ) {
				mRad = -mRad;
			}
			if (pointR <= mRockRadius) {
				mLogicType = LOGIC_STOP;
				
			}
			else {
				mAngle = Math.toDegrees(mRad);
				mLogicType = setLogicType(mAngle);
				Log.i(TAG, "Degrees: " + mAngle + "', Set Logic Type: " + mLogicType);
			}
		}
		mSendBroadcast(0x04, mLogicType);
		return true;
	}

	/*发送广播*/
	public void mSendBroadcast(int cmd, int value) {
		if (mLogicStatus != value) {
			Intent intent = new Intent();
			intent.setAction("com.example.bt_car.mysurfaceview");
			intent.putExtra("cmd", cmd);
			intent.putExtra("value", value); 
			mContext.sendBroadcast(intent);
			Log.d(TAG, "sendBroadcast: " + cmd + " " + value);
			mLogicStatus = value;
		}
	}
	
	/**
	 * Draw the rocker in the thread
	 */
	public void myDraw() {
		try {
			mCanvas = mHolder.lockCanvas();
			if (mCanvas != null) {
				mPaint.setAlpha(0x77);
				mCanvas.drawColor(Color.WHITE);
				
				/* draw base */
				mCanvas.drawCircle(mBaseCentX, mBaseCentY, mBaseRadius, mPaint);
				/* draw rocker */
				mCanvas.drawCircle(mRockCentX, mRockCentY, mRockRadius, mPaint);
			}
		} 
		catch (Exception e) {
			// TODO: handle exception
		} 
		finally {
			if (mCanvas != null) {
				mHolder.unlockCanvasAndPost(mCanvas);
			}
		}
	}
	
	/**
	 * Calculate the angle for logic handle
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return
	 */
	public double getRad(int x1, int y1, int x2, int y2) {
		double dx, dy, rad;
		dx = x2 - x1;
		dy = y2 - y1;
		rad = Math.atan(dy / dx);
		return rad;
	}
	
	/**
	 * Handler the event
	 */
	public void myHandler()
	{
		double rad = Math.atan((mRockCentY - mBaseCentY) / (mRockCentX - mBaseCentX));
		Log.i(TAG, "Rad: " + rad);
	}
	
	/**
	 * 
	 */
	@Override
	public void run() {
		while (mFlag) {
			myDraw();
			try {
				Thread.sleep(20);
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		mFlag = false;
	}

	
}
