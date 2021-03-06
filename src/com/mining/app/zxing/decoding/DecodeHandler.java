/*
 * Copyright (C) 2010 ZXing authors
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

import java.util.Hashtable;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.example.qr_codescan.R;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.mining.app.zxing.camera.CameraManager;
import com.mining.app.zxing.camera.PlanarYUVLuminanceSource;

final class DecodeHandler extends Handler {
	/**
	 * 
	 * @author xiangdong.wu
	 *
	 */
	public interface IOnDecodingResult {
		public void onSuccess(Result result, Bundle bundle);

		public void onFail();

		public void onQuit();
	}

	private IOnDecodingResult mOnDecodingResult;

	/**
	 * 
	 * @param iOnDecodingResult
	 */
	public void setOnDecodingResult(IOnDecodingResult iOnDecodingResult) {
		this.mOnDecodingResult = iOnDecodingResult;
	}

	private static final String TAG = DecodeHandler.class.getSimpleName();

	private final MultiFormatReader multiFormatReader;

	DecodeHandler(Hashtable<DecodeHintType, Object> hints) {
		multiFormatReader = new MultiFormatReader();
		multiFormatReader.setHints(hints);
	}

	@Override
	public void handleMessage(Message message) {
		do {

			if (message.what == R.id.decode) {
				// Log.d(TAG, "Got decode message");
				decode((byte[]) message.obj, message.arg1, message.arg2);
				break;
			}

			if (message.what == R.id.quit) {
				Looper.myLooper().quit();
				if (mOnDecodingResult != null) {
					mOnDecodingResult.onQuit();
				}
				break;
			}
		} while (false);
	}

	/**
	 * Decode the data within the viewfinder rectangle, and time how long it
	 * took. For efficiency, reuse the same reader objects from one decode to
	 * the next.
	 *
	 * @param data
	 *            The YUV preview frame.
	 * @param width
	 *            The width of the preview frame.
	 * @param height
	 *            The height of the preview frame.
	 */
	private void decode(byte[] data, int width, int height) {
		long start = System.currentTimeMillis();
		Result rawResult = null;

		// modify here
		byte[] rotatedData = new byte[data.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++)
				rotatedData[x * height + height - y - 1] = data[x + y * width];
		}
		int tmp = width; // Here we are swapping, that's the difference to #11
		width = height;
		height = tmp;

		PlanarYUVLuminanceSource source = CameraManager.get().buildLuminanceSource(rotatedData, width, height);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		try {
			rawResult = multiFormatReader.decodeWithState(bitmap);
		} catch (ReaderException re) {
			// continue
		} finally {
			multiFormatReader.reset();
		}

		if (rawResult != null) {
			long end = System.currentTimeMillis();
			Log.d(TAG, "Found barcode (" + (end - start) + " ms):\n" + rawResult.toString());
			Bundle bundle = new Bundle();
			bundle.putParcelable(DecodeThread.BARCODE_BITMAP, source.renderCroppedGreyscaleBitmap());
			if (mOnDecodingResult != null) {
				mOnDecodingResult.onSuccess(rawResult, bundle);
			}
		} else {
			if (mOnDecodingResult != null) {
				mOnDecodingResult.onFail();
				;
			}
		}
	}

}
