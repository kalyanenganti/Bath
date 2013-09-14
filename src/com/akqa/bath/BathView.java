package com.akqa.bath;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public final class BathView extends View {
	private static final String TAG = BathView.class.getSimpleName();
	private Bitmap dial;
	private Bitmap hand;
	private Bitmap monitor;
	private Bitmap marking;
	private Bitmap water, water1, water2, water3;
	private Paint dial_bkg;
	private Bitmap background; // holds the cached static part
	private Paint backgroundPaint, rectBlack, tub_capacity;
	private Rect r1;

	public BathView(Context context) {
		super(context);
		init();
	}

	public BathView(Context context, AttributeSet set) {
		super(context, set);
		init();

	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
	}

	private void init() {
		initDrawingTools();

	}

	private void initDrawingTools() {

		r1 = new Rect(260 - 50, 200 - 25, 260 + 50, 200 + 25);

		rectBlack = new Paint();
		rectBlack.setColor(Color.BLACK);
		rectBlack.setStyle(Style.FILL);

		rectBlack.setStyle(Paint.Style.STROKE);
		rectBlack.setColor(Color.WHITE);
		rectBlack.setStrokeWidth(1);

		tub_capacity = new Paint();
		tub_capacity.setColor(Color.WHITE);
		tub_capacity.setStyle(Style.FILL);
		tub_capacity.setTextSize(30);

		// image resources init
		dial = decodeSampledBitmapFromResource(getResources(),
				R.drawable.dialbg2x, 250, 250);
		hand = BitmapFactory.decodeResource(getResources(),
				R.drawable.dialhand2x);
		hand = Bitmap.createScaledBitmap(hand, 175, 25, true);
		monitor = decodeSampledBitmapFromResource(getResources(),
				R.drawable.monitorbg2x, 250, 125);
		marking = decodeSampledBitmapFromResource(getResources(),
				R.drawable.monitormarkings2x, 250, 125);

		// varying levels of water in tub by varying height of bitmap

		water = BitmapFactory.decodeResource(getResources(),
				R.drawable.monitorwater2x);
		water1 = Bitmap.createScaledBitmap(water, 290, 30, true);

		water2 = Bitmap.createScaledBitmap(water, 290, 63, true);

		water3 = Bitmap.createScaledBitmap(water, 290, 125, true);

		// Paint init
		dial_bkg = new Paint();
		dial_bkg.setFilterBitmap(true);// prevents jittery images
		dial_bkg.setAntiAlias(true);

		backgroundPaint = new Paint();
		backgroundPaint.setFilterBitmap(true);
		backgroundPaint.setAntiAlias(true);

	}

	private void drawBackground(Canvas canvas) {
		if (background == null) {
			Log.w(TAG, "Background not created");
		} else {
			canvas.drawBitmap(background, 0, 0, backgroundPaint);
		}
	}

	// scales the bitmap by reducing the input sample image
	private static Bitmap decodeSampledBitmapFromResource(Resources res,
			int resId, int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
	}

	// calculates input sample size of bitmap
	private static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			// Calculate ratios of height and width to requested height and
			// width
			final int heightRatio = Math.round((float) height
					/ (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// Choose the smallest ratio as inSampleSize value, this will
			// guarantee
			// a final image with both dimensions larger than or equal to the
			// requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}

	// Renders our view
	@Override
	public void onDraw(Canvas canvas) {
		float handAngle = ((MainActivity) getContext()).getMean_temp();// turn
																		// hand
																		// by an
																		// angle
																		// equal
																		// to
																		// temparature
		float m1 = ((MainActivity) getContext()).getM1();// get mass of hot
															// water filling tub
		float m2 = ((MainActivity) getContext()).getM2();// get mass of cold
															// water filling tub
		float fill_level = m1 + m2;
		drawBackground(canvas);
		drawDial(canvas);
		drawMeter(canvas);
		canvas.drawText(String.format("%.2f", (fill_level) / 1000), 216, 215,
				tub_capacity);
		drawMonitor(canvas);
		if (fill_level > 0) {
			if (fill_level > 5000) {
				drawWaterLevel1(canvas);
				if (fill_level > 75000) {
					drawWaterLevel2(canvas);
					if (fill_level > 150000) {
						drawWaterLevel3(canvas);
					}
				}
			}

		}
		drawMarkings(canvas);
		if (handAngle == 0) {// if temp is zero redraw
			invalidate();
		}
		float temp = 0.00f;
		canvas.save(); // save the position of the canvas
		canvas.rotate(handAngle, 170 + (hand.getWidth() / 2),
				130 + (hand.getHeight() / 2));
		canvas.drawBitmap(hand, 170, 130, dial_bkg);
		canvas.restore();
		if (handAngle != temp) {
			// if angle has changed redraw view
			temp = handAngle;
			invalidate();
		}

	}

	// draw rectangle temperature guage black
	private void drawMeter(Canvas canvas) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		// fill
		rectBlack.setStyle(Paint.Style.FILL);
		rectBlack.setColor(Color.BLACK);
		canvas.drawRect(r1, rectBlack);

		// border
		rectBlack.setStyle(Paint.Style.STROKE);
		rectBlack.setColor(Color.WHITE);
		canvas.drawRect(r1, rectBlack);
	}

	// draw Markings
	private void drawMarkings(Canvas canvas) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.drawBitmap(marking, 40, 300, dial_bkg);
	}

	// draw Water Level
	private void drawWaterLevel1(Canvas canvas) {
		canvas.drawBitmap(water1, 130, 405, dial_bkg);
	}

	private void drawWaterLevel2(Canvas canvas) {
		canvas.drawBitmap(water2, 130, 372, dial_bkg);
	}

	private void drawWaterLevel3(Canvas canvas) {
		canvas.drawBitmap(water3, 130, 300, dial_bkg);
	}

	// draw Monitor
	private void drawMonitor(Canvas canvas) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.drawBitmap(monitor, 40, 300, dial_bkg);
	}

	// draw Dial
	private void drawDial(Canvas canvas) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.drawBitmap(dial, 115, 5, dial_bkg);
	}

	// called on screen rotation/ view is changed
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		System.out.println("on size changed  called");

		Log.d(TAG, "Size changed to " + w + "x" + h);

		regenerateBackground();

	}

	// regenerate static part of our view
	private void regenerateBackground() {
		System.out.println("regenerate called");

		// free the old bitmap
		if (background != null) {
			background.recycle();
		}
		background = Bitmap.createBitmap(getWidth(), getHeight(),
				Bitmap.Config.ARGB_8888);
		Canvas backgroundCanvas = new Canvas(background);
		float scale = (float) getWidth();
		backgroundCanvas.scale(scale, scale);
		drawDial(backgroundCanvas);
		drawMeter(backgroundCanvas);
		drawMonitor(backgroundCanvas);
		drawMarkings(backgroundCanvas);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		Log.d(TAG, "Width spec: " + MeasureSpec.toString(widthMeasureSpec));
		Log.d(TAG, "Height spec: " + MeasureSpec.toString(heightMeasureSpec));

		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);

		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		int chosenWidth = chooseDimension(widthMode, widthSize);
		int chosenHeight = chooseDimension(heightMode, heightSize);

		int chosenDimension = Math.min(chosenWidth, chosenHeight);

		setMeasuredDimension(chosenDimension, chosenDimension);
	}

	private int chooseDimension(int mode, int size) {
		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
			return size;
		} else { // (mode == MeasureSpec.UNSPECIFIED)
			return getPreferredSize();
		}
	}

	// if no preferred size exists
	private int getPreferredSize() {
		return 300;
	}

}
