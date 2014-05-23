package net.maxbrightnessimage;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

public class MainActivity extends Activity {

	private static String BRIGHTNESS = "brightness";
	private static String IMG_PATH = "imagePath";
	private static int RESULT_LOAD_IMAGE = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		ImageView v = (ImageView) findViewById(R.id.selected_image);
		v.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				selectPicture();
				return false;
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(MainActivity.class.getName(), "onResume. loadImage");
		
		loadImage();
		
		Log.i(MainActivity.class.getName(), "onResume. changeScreenToBrightest");
		changeScreenToBrightest();
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		Log.i(MainActivity.class.getName(), "onPause. recycleImageIfImageSelected");
		recycleImageIfNotDefaultImage();
		
		Log.i(MainActivity.class.getName(), "onPause. restoreBrightness");
		restoreBrightness();
	}

	private void recycleImageIfNotDefaultImage() {
		SharedPreferences p = getPreferences(MODE_PRIVATE);
		String picturePath = p.getString(IMG_PATH, "");
		ImageView imageView = (ImageView) findViewById(R.id.selected_image);
		boolean isDefaultImage = isBlank(picturePath);
		if ( !isDefaultImage ) {
		    Drawable drawable = imageView.getDrawable();
		    if (drawable instanceof BitmapDrawable) {
		    	Log.i(getClass().getName(), "recycle image");
		        BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
		        Bitmap bitmap = bitmapDrawable.getBitmap();
		        bitmap.recycle();
		        Log.i(getClass().getName(), "width:" + bitmap.getWidth() + ",height:" + bitmap.getHeight());
		    }
		}
	}

	private void changeScreenToBrightest() {
		SharedPreferences p = getPreferences(MODE_PRIVATE);
		p.edit().putInt(BRIGHTNESS, getActualBrightness()).commit();
		setActualBrightness(255);
	}
	
	private int getActualBrightness() {
    	int result = 255;
    	try {
			result = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
		} catch (Throwable e) {
			Log.e(MainActivity.class.getName(), "getBrightness fail", e);
		}
    	return result;
    }
	
	private void setActualBrightness(int brightness) {
    	android.provider.Settings.System.putInt(getContentResolver(),android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    	brightness = brightness > 255 ? 255 : brightness;
    	brightness = brightness <= 0 ? 1 : brightness;
    	android.provider.Settings.System.putInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS, brightness);
    	
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = (float) brightness/255;
        getWindow().setAttributes(lp);
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
		if (id == R.id.choose_image) {
			selectPicture();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void selectPicture() {
		Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(i, RESULT_LOAD_IMAGE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
	         Uri selectedImage = data.getData();
	         String[] filePathColumn = { MediaStore.Images.Media.DATA };
	 
	         Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
	         cursor.moveToFirst();
	 
	         int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
	         String picturePath = cursor.getString(columnIndex);
	         cursor.close();
	                      
	         SharedPreferences p = getPreferences(MODE_PRIVATE);
	         Editor editor = p.edit();
	         editor.putString(IMG_PATH, picturePath);
	         editor.commit();
        	 loadImage();
	     }
	}
	
	private void restoreBrightness() {
		SharedPreferences p = getPreferences(MODE_PRIVATE);
		setActualBrightness(p.getInt(BRIGHTNESS, 255));
	}
	
	@SuppressWarnings("deprecation")
	private void loadImage() {
		SharedPreferences p = getPreferences(MODE_PRIVATE);
		String picturePath = p.getString(IMG_PATH, "");
		
		ImageView imageView = (ImageView) findViewById(R.id.selected_image);
		if ( isBlank(picturePath) || !new File(picturePath).exists() ) {
			p.edit().putString(IMG_PATH, "").commit();
			imageView.setImageResource(R.drawable.default_image);
			return;
		}
		Display display = getWindowManager().getDefaultDisplay();
		Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
		Log.i(getClass().getName(), "display.h:" + display.getHeight() + ", display.w:" + display.getWidth());
		boolean isLargeImage = bitmap.getHeight() > display.getHeight() || bitmap.getWidth() > display.getWidth();
		if ( isLargeImage ) {
			for ( int i = 1; i < 10; i++ ) {
				int targetDensity = bitmap.getDensity() / i;
				try {
					int h = bitmap.getScaledHeight(targetDensity);
					int w = bitmap.getScaledWidth(targetDensity);
					Log.i(getClass().getName(), "reduce density to " + targetDensity);
					imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, w, h, true));
					break;
				} catch (OutOfMemoryError e) {
					Log.w(getClass().getName(), "OOM when targetDensity:" + targetDensity);
				}				
			}
		} else {
			imageView.setImageBitmap(bitmap);
		}
	}
	
	private boolean isBlank(String s) {
		return s == null || s.trim().length() == 0;
	}
	
	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			return rootView;
		}
	}

}
