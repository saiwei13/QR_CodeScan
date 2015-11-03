package com.mining.app.zxing.decoding;

import java.util.Vector;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.mining.app.zxing.view.ViewfinderView;

/**
 * 二维码扫描的逻辑处理
 * @author xiangdong.wu
 *
 */
public interface IQRCodeScan {
	public interface IQRCodeScanCallback{
		public void handleDecode(Result result, Bitmap barcode);
		
		public void drawViewfinder();
	}
	/**
	 * 开始解码
	 * @param decodeFormats
	 * @param characterSet
	 */
	public void start(ViewfinderView viewfinderView,
			Vector<BarcodeFormat> decodeFormats, String characterSet,
			IQRCodeScanCallback qrCodeScanCallback);
	/**
	 * 停止解码
	 */
	 public void quitSynchronously();
}
