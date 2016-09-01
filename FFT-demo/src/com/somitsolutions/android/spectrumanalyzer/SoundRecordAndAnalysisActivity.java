package com.somitsolutions.android.spectrumanalyzer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import ca.uol.aig.fftpack.RealDoubleFFT;

public class SoundRecordAndAnalysisActivity extends Activity implements OnClickListener {

	private final static int ID_BITMAPDISPLAYSPECTRUM = 1;
	private final static int ID_IMAGEVIEWSCALE = 2;
	int frequency = 8000;
	int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	AudioRecord audioRecord;
	int blockSize;// = 256;
	Button startStopButton;
	boolean started = false;
	boolean CANCELLED_FLAG = false;
	RecordAudio recordTask;
	ImageView imageViewDisplaySectrum;
	MyImageView imageViewScale;
	Bitmap bitmapDisplaySpectrum;
	Canvas canvasDisplaySpectrum;
	Paint paintSpectrumDisplay;
	Paint paintScaleDisplay;
	LinearLayout main;
	int width;
	int height;
	int left_Of_BimapScale;
	int left_Of_DisplaySpectrum;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Display display = getWindowManager().getDefaultDisplay();
		// Point size = new Point();
		// display.get(size);
		width = display.getWidth();
		height = display.getHeight();

		blockSize = 256;
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// left_Of_BimapScale = main.getC.getLeft();
		MyImageView scale = (MyImageView) main.findViewById(ID_IMAGEVIEWSCALE);
		ImageView bitmap = (ImageView) main.findViewById(ID_BITMAPDISPLAYSPECTRUM);
		left_Of_BimapScale = scale.getLeft();
		left_Of_DisplaySpectrum = bitmap.getLeft();
	}

	protected void onCancelled(Boolean result) {

		try {
			if (audioRecord != null) {
				audioRecord.stop();
			}
		} catch (IllegalStateException e) {
			Log.e("Stop failed", e.toString());
		}
		/*
		 * //recordTask.cancel(true); Log.d("FFTSpectrumAnalyzer","onCancelled: New Screen"); Intent intent = new Intent(Intent.ACTION_MAIN);
		 * intent.addCategory(Intent.CATEGORY_HOME); intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(intent);
		 */
	}

	public void onClick(View v) {
		if (started == true) {
			// started = false;
			CANCELLED_FLAG = true;
			// recordTask.cancel(true);
			try {
				if (audioRecord != null) {
					audioRecord.stop();
				}
			} catch (IllegalStateException e) {
				Log.e("Stop failed", e.toString());

			}
			startStopButton.setText("Start");

			canvasDisplaySpectrum.drawColor(Color.BLACK);
		} else {
			started = true;
			CANCELLED_FLAG = false;
			startStopButton.setText("Stop");
			recordTask = new RecordAudio();
			recordTask.execute();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		/*
		 * try{ audioRecord.stop(); } catch(IllegalStateException e){ Log.e("Stop failed", e.toString());
		 *
		 * }
		 */
		if (recordTask != null) {
			recordTask.cancel(true);
		}
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	@Override
	public void onStart() {
		super.onStart();
		main = new LinearLayout(this);
		main.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT));
		main.setOrientation(LinearLayout.VERTICAL);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		imageViewDisplaySectrum = new ImageView(this);
		if (width > 512) {
			bitmapDisplaySpectrum = Bitmap.createBitmap(512, 300, Bitmap.Config.ARGB_8888);
		} else {
			bitmapDisplaySpectrum = Bitmap.createBitmap(256, 150, Bitmap.Config.ARGB_8888);
		}
		LinearLayout.LayoutParams layoutParams_imageViewScale = null;
		canvasDisplaySpectrum = new Canvas(bitmapDisplaySpectrum);
		paintSpectrumDisplay = new Paint();
		paintSpectrumDisplay.setColor(Color.GREEN);
		imageViewDisplaySectrum.setImageBitmap(bitmapDisplaySpectrum);
		if (width > 512) {
			LayoutParams layoutParams_imageViewDisplaySpectrum = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			layoutParams_imageViewDisplaySpectrum.setMargins(100, 600, 0, 0);
			imageViewDisplaySectrum.setLayoutParams(layoutParams_imageViewDisplaySpectrum);
			layoutParams_imageViewScale = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			layoutParams_imageViewScale.setMargins(100, 20, 0, 0);
		} else if ((width > 320) && (width < 512)) {
			LayoutParams layoutParams_imageViewDisplaySpectrum = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			layoutParams_imageViewDisplaySpectrum.setMargins(60, 250, 0, 0);
			imageViewDisplaySectrum.setLayoutParams(layoutParams_imageViewDisplaySpectrum);
			layoutParams_imageViewScale = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			layoutParams_imageViewScale.setMargins(60, 20, 0, 100);
		} else if (width < 320) {
			imageViewDisplaySectrum.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			layoutParams_imageViewScale = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		}
		imageViewDisplaySectrum.setId(ID_BITMAPDISPLAYSPECTRUM);
		main.addView(imageViewDisplaySectrum);

		imageViewScale = new MyImageView(this);
		imageViewScale.setLayoutParams(layoutParams_imageViewScale);
		imageViewScale.setId(ID_IMAGEVIEWSCALE);
		main.addView(imageViewScale);

		//Button
		startStopButton = new Button(this);
		startStopButton.setText("Start");
		startStopButton.setOnClickListener(this);
		startStopButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

		main.addView(startStopButton);

		setContentView(main);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();

		try {
			if (audioRecord != null) {
				audioRecord.stop();
			}
		} catch (IllegalStateException e) {
			Log.e("Stop failed", e.toString());

		}
		if (recordTask != null) {
			recordTask.cancel(true);
		}
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		try {
			if (audioRecord != null) {
				audioRecord.stop();
			}
		} catch (IllegalStateException e) {
			Log.e("Stop failed", e.toString());

		}
		if (recordTask != null) {
			recordTask.cancel(true);
		}
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	private class RecordAudio extends AsyncTask<Void, double[], Boolean> {

		private RealDoubleFFT transformer;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			transformer = new RealDoubleFFT(blockSize);
		}

		@Override
		protected Boolean doInBackground(Void... params) {

			int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
			audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, frequency, channelConfiguration, audioEncoding, bufferSize);
			int bufferReadResult;
			short[] buffer = new short[blockSize];
			double[] toTransform = new double[blockSize];
			try {
				audioRecord.startRecording();
			} catch (IllegalStateException e) {
				Log.e("Recording failed", e.toString());
			}
			while (started) {
				if (isCancelled() || (CANCELLED_FLAG == true)) {
					started = false;
					// publishProgress(cancelledResult);
					Log.d("doInBackground", "Cancelling the RecordTask");
					break;
				} else {
					bufferReadResult = audioRecord.read(buffer, 0, blockSize);

					for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
						toTransform[i] = buffer[i] / 32768.0; // signed 16 bit
					}

					transformer.ft(toTransform);
					publishProgress(toTransform);
				}
			}
			return true;
		}

		@Override
		protected void onProgressUpdate(double[]... progress) {
			Log.d("onProgressUpdate:", Integer.toString(progress[0].length));
			canvasDisplaySpectrum.drawColor(Color.GRAY);
			if (width > 512) {
				for (int i = 0; i < progress[0].length; i++) {
					int x = 2 * i;
					int downy = (int) (150 - (progress[0][i] * 10));
					int upy = 150;
					canvasDisplaySpectrum.drawLine(x, downy, x, upy, paintSpectrumDisplay);
				}
				imageViewDisplaySectrum.invalidate();
			} else {
				for (int i = 0; i < progress[0].length; i++) {
					int x = i;
					int downy = (int) (150 - (progress[0][i] * 10));
					int upy = 150;
					canvasDisplaySpectrum.drawLine(x, downy, x, upy, paintSpectrumDisplay);
				}

				imageViewDisplaySectrum.invalidate();
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			try {
				if (audioRecord != null) {
					audioRecord.stop();
				}
			} catch (IllegalStateException e) {
				Log.e("Stop failed", e.toString());
			}

			canvasDisplaySpectrum.drawColor(Color.BLACK);
			imageViewDisplaySectrum.invalidate();

		}
	}

	// Custom Imageview Class
	public class MyImageView extends ImageView {
		Paint paintScaleDisplay;
		Bitmap bitmapScale;
		Canvas canvasScale;

		public MyImageView(Context context) {
			super(context);
			if (width > 512) {
				bitmapScale = Bitmap.createBitmap(512, 50, Bitmap.Config.ARGB_8888);
			} else {
				bitmapScale = Bitmap.createBitmap(256, 50, Bitmap.Config.ARGB_8888);
			}

			paintScaleDisplay = new Paint();
			paintScaleDisplay.setColor(Color.WHITE);
			paintScaleDisplay.setStyle(Paint.Style.FILL);

			canvasScale = new Canvas(bitmapScale);

			setImageBitmap(bitmapScale);
			invalidate();
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);

			if (width > 512) {
				canvasScale.drawLine(0, 30, 512, 30, paintScaleDisplay);
				for (int i = 0, j = 0; i < 512; i = i + 128, j++) {
					for (int k = i; k < (i + 128); k = k + 16) {
						canvasScale.drawLine(k, 30, k, 25, paintScaleDisplay);
					}
					canvasScale.drawLine(i, 40, i, 25, paintScaleDisplay);
					String text = Integer.toString(j) + " KHz";
					canvasScale.drawText(text, i, 45, paintScaleDisplay);
				}
				canvas.drawBitmap(bitmapScale, 0, 0, paintScaleDisplay);
			} else if ((width > 320) && (width < 512)) {
				canvasScale.drawLine(0, 30, 0 + 256, 30, paintScaleDisplay);
				for (int i = 0, j = 0; i < 256; i = i + 64, j++) {
					for (int k = i; k < (i + 64); k = k + 8) {
						canvasScale.drawLine(k, 30, k, 25, paintScaleDisplay);
					}
					canvasScale.drawLine(i, 40, i, 25, paintScaleDisplay);
					String text = Integer.toString(j) + " KHz";
					canvasScale.drawText(text, i, 45, paintScaleDisplay);
				}
				canvas.drawBitmap(bitmapScale, 0, 0, paintScaleDisplay);
			} else if (width < 320) {
				canvasScale.drawLine(0, 30, 256, 30, paintScaleDisplay);
				for (int i = 0, j = 0; i < 256; i = i + 64, j++) {
					for (int k = i; k < (i + 64); k = k + 8) {
						canvasScale.drawLine(k, 30, k, 25, paintScaleDisplay);
					}
					canvasScale.drawLine(i, 40, i, 25, paintScaleDisplay);
					String text = Integer.toString(j) + " KHz";
					canvasScale.drawText(text, i, 45, paintScaleDisplay);
				}
				canvas.drawBitmap(bitmapScale, 0, 0, paintScaleDisplay);
			}
		}
	}
}
