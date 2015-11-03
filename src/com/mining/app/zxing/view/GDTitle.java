package com.mining.app.zxing.view;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.qr_codescan.R;

/**
 * 标题栏控件(包含左右按钮以及中间标题文本)
 *
 * @author yangyang.qian
 *
 */

/**
 * 由于界面需要，补充一个右侧按钮
 *
 * @author kongzhao.wkz
 * @since 2015/06/25
 */
public class GDTitle extends FrameLayout {

	/** 左边按钮 */
	private ImageButton mRight;
	/** 右边按钮 */
	private ImageButton mLeft;
	/** 文本 */
	private TextView mText;

	private Button mRightBtn;

	/** 皮肤类 */
	// SkinManager skinManager;

	private View v;

	private boolean mWithBack;

	public GDTitle(Context context) {
		this(context, null);
		init(context);
	}

	public GDTitle(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		init(context);
	}

	public GDTitle(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(final Context context) {
		mWithBack = true;
		LayoutInflater mInflater = LayoutInflater.from(context);
		v = mInflater.inflate(R.layout.view_gdtitle, null);

		mLeft = (ImageButton) v.findViewById(R.id.gdtitle_left);
		mRight = (ImageButton) v.findViewById(R.id.gdtitle_right);
		mText = (TextView) v.findViewById(R.id.gdtitle_text);

		mLeft.setFocusable(false);

		/**
		 * 把右边按钮的点击事件直接转化成系统返回按钮的点击事件
		 */
		mLeft.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!mWithBack)
					return;
				KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK);
				((Activity) context).dispatchKeyEvent(event);
				event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK);
				((Activity) context).dispatchKeyEvent(event);
			}
		});
		/**
		 * 增加右侧非图片按钮，并设置边框底图
		 *
		 * @author kongzhao.wkz
		 */
		mRightBtn = (Button) v.findViewById(R.id.gdtitle_rightBtn);
		// mRightBtn.setTextColor(Color.WHITE);
		// GradientDrawable drawable = new GradientDrawable();
		// drawable.setShape(GradientDrawable.RECTANGLE); // 画框
		// drawable.setStroke(1, Color.WHITE); // 边框粗细及颜色
		// drawable.setColor(0xffffff); // 边框内部颜色
		// mRightBtn.setBackgroundDrawable(drawable); // 设置背景（效果就是有边框及底色）

		addView(v);
	}

	public TextView getTitleView() {
		return mText;
	}

	public ImageButton getLeftView() {
		return mLeft;
	}

	public ImageButton getRightView() {
		return mRight;
	}

	public Button getRightButton() {
		return mRightBtn;
	}

	public void setText(String string) {
		mText.setText(string);
	}

	public void setText(int resid) {
		mText.setText(resid);
	}

	public void setTextColor(int color) {
		mText.setTextColor(color);
	}

	public void setViewBackgroundColor(int color) {
		v.setBackgroundColor(color);
	}

	public void setWithBack(boolean withBack) {
		mWithBack = withBack;
	}

}
