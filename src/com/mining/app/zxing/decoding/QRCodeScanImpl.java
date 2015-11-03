/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mining.app.zxing.decoding;

import java.util.Vector;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.example.qr_codescan.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.mining.app.zxing.camera.CameraManager;
import com.mining.app.zxing.view.ViewfinderResultPointCallback;
import com.mining.app.zxing.view.ViewfinderView;

/**
 * This class handles all the messaging which comprises the state machine for
 * capture.
 */
public final class QRCodeScanImpl implements IQRCodeScan {

	private static final String TAG = QRCodeScanImpl.class.getSimpleName();

	private DecodeThread decodeThread;
	private State state;
	private IQRCodeScanCallback mQrCodeScanCallback;

	private enum State {
		PREVIEW, SUCCESS, DONE
	}

	public void start(ViewfinderView viewfinderView, Vector<BarcodeFormat> decodeFormats, String characterSet,
			IQRCodeScanCallback qrCodeScanCallback) {
		decodeThread = new DecodeThread(decodeFormats, characterSet, new ViewfinderResultPointCallback(viewfinderView));
		decodeThread.start();
		decodeThread.getHandler().setOnDecodingResult(new DecodeHandler.IOnDecodingResult() {

			@Override
			public void onSuccess(Result result, Bundle bundle) {
				Message message = qrCodeScanHandler.obtainMessage();
				message.what = R.id.decode_succeeded;
				message.obj = result;
				message.setData(bundle);
				message.sendToTarget();

			}

			@Override
			public void onFail() {
				Message message = qrCodeScanHandler.obtainMessage();
				message.what = R.id.decode_failed;
				message.sendToTarget();

			}

			@Override
			public void onQuit() {

			}
		});
		state = State.SUCCESS;
		mQrCodeScanCallback = qrCodeScanCallback;
		// Start ourselves capturing previews and decoding.
		CameraManager.get().startPreview();
		restartPreviewAndDecode();
	}

	private Handler qrCodeScanHandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message message) {
			do {
				if (message.what == R.id.auto_focus) {
					// Log.d(TAG, "Got auto-focus message");
					// When one auto focus pass finishes, start another. This is
					// the
					// closest thing to
					// continuous AF. It does seem to hunt a bit, but I'm not
					// sure
					// what else to do.
					if (state == State.PREVIEW) {
						CameraManager.get().requestAutoFocus(this, R.id.auto_focus);
					}
					break;
				}
				if (message.what == R.id.restart_preview) {
					Log.d(TAG, "Got restart preview message");
					restartPreviewAndDecode();
					break;
				}
				if (message.what == R.id.decode_succeeded) {
					Log.d(TAG, "Got decode succeeded message");
					state = State.SUCCESS;
					Bundle bundle = message.getData();

					/***********************************************************************/
					Bitmap barcode = bundle == null ? null : (Bitmap) bundle.getParcelable(DecodeThread.BARCODE_BITMAP);
					if (mQrCodeScanCallback != null) {
						mQrCodeScanCallback.handleDecode((Result) message.obj, barcode);
					}
					break;
				}
				if (message.what == R.id.decode_failed) {
					// We're decoding as fast as possible, so when one decode
					// fails,
					// start another.
					state = State.PREVIEW;
					CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
					break;
				}
			} while (false);
		}
	};

	public void quitSynchronously() {
		state = State.DONE;
		CameraManager.get().stopPreview();
		Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
		quit.sendToTarget();
		try {
			decodeThread.join();
		} catch (InterruptedException e) {
			// continue
		}

		// Be absolutely sure we don't send any queued up messages
		decodeThread.getHandler().removeMessages(R.id.decode_succeeded);
		decodeThread.getHandler().removeMessages(R.id.decode_failed);
	}

	private void restartPreviewAndDecode() {
		if (state == State.SUCCESS) {
			state = State.PREVIEW;
			CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
			CameraManager.get().requestAutoFocus(qrCodeScanHandler, R.id.auto_focus);
			if (mQrCodeScanCallback != null) {
				mQrCodeScanCallback.drawViewfinder();
			}
		}
	}

}
