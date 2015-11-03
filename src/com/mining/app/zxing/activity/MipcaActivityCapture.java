package com.mining.app.zxing.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.Toast;

import com.example.qr_codescan.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.mining.app.zxing.camera.CameraManager;
import com.mining.app.zxing.decoding.IQRCodeScan;
import com.mining.app.zxing.decoding.InactivityTimer;
import com.mining.app.zxing.decoding.QRCodeScanImpl;
import com.mining.app.zxing.view.GDTitle;
import com.mining.app.zxing.view.ViewfinderView;

import java.io.IOException;
import java.util.Vector;
/**
 * Initial the camera
 * @author Ryan.Tang
 */

/**
 *
 * <p>
 * 采用 Intent intent = new Intent(); intent.setClass(MainActivity.this,
 * MipcaActivityCapture.class); intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
 * startActivityForResult(intent, SCANNIN_GREQUEST_CODE); 启动,并有相应的参数回调
 * 
 * @Override protected void onActivityResult(int requestCode, int resultCode,
 *           Intent data) { super.onActivityResult(requestCode, resultCode,
 *           data); switch (requestCode) { case SCANNIN_GREQUEST_CODE:
 *           if(resultCode == RESULT_OK){ Bundle bundle = data.getExtras();
 *           //显示url mTextView.setText(bundle.getString("result")); //显示图片
 *           mImageView.setImageBitmap((Bitmap)
 *           data.getParcelableExtra("bitmap")); } break; } }
 *           </p>
 * @author xiangdong.wu
 *
 */
public class MipcaActivityCapture extends Activity implements Callback, IQRCodeScan.IQRCodeScanCallback {

	private final String TAG = "chenwei.MipcaActiv";

	private IQRCodeScan mQRCodeScan;
	private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;
	private InactivityTimer inactivityTimer;
	private MediaPlayer mediaPlayer;
	private boolean playBeep;
	private static final float BEEP_VOLUME = 0.10f;
	private boolean vibrate;
	private GDTitle mGdTitle;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		try {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_capture);
			// ViewUtil.addTopView(getApplicationContext(), this,
			// R.string.scan_card);
			CameraManager.init(getApplication());
			viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
			mGdTitle = (GDTitle) findViewById(R.id.gdtitle);
			mGdTitle.setText(R.string.title_activity_Mipca_Capture);
			// Button mButtonBack = (Button) findViewById(R.id.button_back);
			// mButtonBack.setOnClickListener(new OnClickListener() {
			//
			// @Override
			// public void onClick(View v) {
			// MipcaActivityCapture.this.finish();
			//
			// }
			// });
			hasSurface = false;
			inactivityTimer = new InactivityTimer(this);
		}catch (Exception e){
			Toast.makeText(getApplication(),"摄像头启动失败",Toast.LENGTH_SHORT).show();
			finish();
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			initCamera(surfaceHolder);
		} else {
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		decodeFormats = null;
		characterSet = null;

		playBeep = true;
		AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
		if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
			playBeep = false;
		}
		initBeepSound();
		vibrate = true;

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mQRCodeScan != null) {
			mQRCodeScan.quitSynchronously();
			mQRCodeScan = null;
		}
		CameraManager.get().closeDriver();
	}

	@Override
	protected void onDestroy() {
		inactivityTimer.shutdown();
		super.onDestroy();
	}

	/**
	 * 处理解析的结果
	 * 
	 * @param result
	 * @param barcode
	 */
	@Override
	public void handleDecode(Result result, Bitmap barcode) {
		if (!barcode.isRecycled()) {
			barcode.recycle();
		}
		inactivityTimer.onActivity();
		playBeepSoundAndVibrate();
		String resultString = result.getText();

		Log.i(TAG,"handleDecode() resultString="+resultString);

		if (resultString.equals("")) {
			Toast.makeText(MipcaActivityCapture.this, "Scan failed!", Toast.LENGTH_SHORT).show();
		} else {

			Toast.makeText(this,"结果是："+resultString,Toast.LENGTH_LONG).show();

			Intent resultIntent = new Intent();
			Bundle bundle = new Bundle();
			bundle.putString("result", resultString);
			resultIntent.putExtras(bundle);
			this.setResult(RESULT_OK, resultIntent);
		}
		MipcaActivityCapture.this.finish();
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			return;
		} catch (RuntimeException e) {
			return;
		}
		if (mQRCodeScan == null) {
			mQRCodeScan = new QRCodeScanImpl();
			mQRCodeScan.start(viewfinderView, decodeFormats, characterSet, this);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;

	}

	@Override
	public void drawViewfinder() {
		viewfinderView.drawViewfinder();

	}

	private void initBeepSound() {
		if (playBeep && mediaPlayer == null) {
			// The volume on STREAM_SYSTEM is not adjustable, and users found it
			// too loud,
			// so we now play on the music stream.
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnCompletionListener(beepListener);

			AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.beep);
			try {
				mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
				file.close();
				mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
				mediaPlayer.prepare();
			} catch (IOException e) {
				mediaPlayer = null;
			}
		}
	}

	private static final long VIBRATE_DURATION = 200L;

	private void playBeepSoundAndVibrate() {
		if (playBeep && mediaPlayer != null) {
			mediaPlayer.start();
		}
		if (vibrate) {
			Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_DURATION);
		}
	}

	/**
	 * When the beep has finished playing, rewind to queue up another one.
	 */
	private final OnCompletionListener beepListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mediaPlayer) {
			mediaPlayer.seekTo(0);
		}
	};

}