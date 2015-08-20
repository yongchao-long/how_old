package com.how_old.app;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.facepp.error.FaceppParseException;
import com.how_old.app.FaceDetect.CallBack;
import com.how_old.app.R.id;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.os.Build;
import android.provider.MediaStore;

public class MainActivity extends ActionBarActivity implements OnClickListener{

	private static final int PICK_CODE = 0;
	private static final int TAKE_PHOTO = 3;
	private Button getImage;
	private Button Detect;
	private Button takePhoto;
	private ImageView Photo;
	private TextView Tip;
	private View Waitting;
	private String mCurrentSrc;
	private Bitmap mPhotoImg;
	private Paint mPaint;
	private Uri imageUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏

        setContentView(R.layout.activity_main);
        
       initView();
       initEvent();
       mPaint = new Paint();
    }

	private void initEvent() {
		// TODO Auto-generated method stub
		getImage.setOnClickListener(this);
		Detect.setOnClickListener(this);
		takePhoto.setOnClickListener(this);
	}

	private void initView() {
		// TODO Auto-generated method stub
		getImage = (Button) findViewById(R.id.id_getImage);
		Detect = (Button) findViewById(R.id.id_Detect);
		Photo = (ImageView) findViewById(R.id.id_Photo);
		Tip = (TextView) findViewById(R.id.id_Tip);
		Waitting =  findViewById(R.id.id_Waitting);
		takePhoto = (Button) findViewById(R.id.id_takePhoto);
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// TODO Auto-generated method stub
		switch(requestCode){
		case PICK_CODE:
		    if(intent != null){
			Uri uri = intent.getData();
			Cursor cursor = getContentResolver().query(uri, null, null, null, null);
			cursor.moveToFirst();
			int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
		    mCurrentSrc = cursor.getString(idx);
		    cursor.close();
		    resizePhoto();
		    Photo.setImageBitmap(mPhotoImg);
		    Tip.setText("Click Detect ==>");
		}
		    break;
		case TAKE_PHOTO:
			Bitmap bmPhoto = (Bitmap) intent.getExtras().get("data");
			mCurrentSrc = "image_capture";
			mPhotoImg = bmPhoto;
			Photo.setImageBitmap(mPhotoImg);
			Tip.setText("Click Detect ==>");
			
			break;
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	private void resizePhoto() {
		// TODO Auto-generated method stub
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentSrc, options);
		double ratio = Math.max(options.outWidth * 1.0 / 1024f, options.outHeight *1.0 / 1024f);
		options.inSampleSize = (int) Math.ceil(ratio);
		options.inJustDecodeBounds = false;
		mPhotoImg = BitmapFactory.decodeFile(mCurrentSrc, options);
	}

	private static final int MSG_SUCCESS = 1;
	private static final int MSG_ERROR = 2;
	private Handler mhandle = new Handler(){
		
		public void handleMessage(Message msg) {
			switch(msg.what){
			case MSG_SUCCESS:
				Waitting.setVisibility(View.GONE);
				JSONObject rs = (JSONObject) msg.obj;
				PrepareRsBitmap(rs);
				Photo.setImageBitmap(mPhotoImg);
				break;
			case MSG_ERROR:
				Waitting.setVisibility(View.GONE);
				String errorMsg = (String) msg.obj;
				if(TextUtils.isEmpty(errorMsg)){
					Tip.setText("Error.");
				}
				else{
					Tip.setText(errorMsg);
				}
				break;
			default:
				break;
			}
			 super.handleMessage(msg);
		}
	};
	
	private void PrepareRsBitmap(JSONObject rs) {
		// TODO Auto-generated method stub
		
		Bitmap bitmap = Bitmap.createBitmap(mPhotoImg.getWidth(), mPhotoImg.getHeight(), mPhotoImg.getConfig());
		
		Canvas canvas = new Canvas(bitmap);
		canvas.drawBitmap(mPhotoImg, 0, 0, null);
		try {
			JSONArray faces = rs.getJSONArray("face");
			int faceCount = faces.length();
			Tip.setText("find "+faceCount);
			for(int i = 0; i < faceCount; i++){
				//取得单独的face对象
				JSONObject face = faces.getJSONObject(i);
				JSONObject position = face.getJSONObject("position");
				
				float x = (float) position.getJSONObject("center").getDouble("x");
				float y = (float) position.getJSONObject("center").getDouble("y");
				float w = (float) position.getDouble("width");
				float h = (float) position.getDouble("height");
				
				x = x / 100 * bitmap.getWidth();
				y = y / 100 * bitmap.getHeight();
				w = w / 100 * bitmap.getWidth();
				h = h / 100 * bitmap.getHeight();
				
				mPaint.setColor(0xffffff);
				mPaint.setStrokeWidth(3);
				//画box
				canvas.drawLine(x - w/2, y - h/2, x - w/2, y + h/2, mPaint);//左边
				canvas.drawLine(x - w/2, y - h/2, x + w/2, y - h/2, mPaint);//底边
				canvas.drawLine(x + w/2, y + h/2, x - w/2, y + h/2, mPaint);//上边
				canvas.drawLine(x + w/2, y + h/2, x + w/2, y - h/2, mPaint);//右边
				
				//获取年龄和性别
				
				int age = face.getJSONObject("attribute").getJSONObject("age").getInt("value");
				String gender = face.getJSONObject("attribute").getJSONObject("gender").getString("value");
				
				//绘制年龄显示框
				Bitmap ageBitmap = BuiledAgeBitmap(age,"Male".equals(gender));
				
				int ageWidth = ageBitmap.getWidth();
				int ageHeight = ageBitmap.getHeight();
				
				if(bitmap.getWidth()<Photo.getWidth() && bitmap.getHeight()<Photo.getHeight()){
					float ratio = Math.max(bitmap.getWidth() * 1.0f / Photo.getWidth(), 
							bitmap.getHeight() * 1.0f / Photo.getHeight());
					
					ageBitmap = Bitmap.createScaledBitmap(ageBitmap, (int) (ageWidth * ratio), (int) (ageHeight * ratio), false);
				}
				canvas.drawBitmap(ageBitmap, x - ageBitmap.getWidth()/2, y - h/2 - ageBitmap.getHeight(), null);
				
				mPhotoImg = bitmap;
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	};
	
	private Bitmap BuiledAgeBitmap(int age, boolean isMale) {
		// TODO Auto-generated method stub
		TextView tv =(TextView) findViewById(R.id.id_age_and_gender);
		tv.setText(age+"");
		if(isMale){
			tv.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.male), null, null, null);
		}
		else{
			tv.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.female), null, null, null);
		}
		tv.setDrawingCacheEnabled(true);
		Bitmap bitmap = Bitmap.createBitmap(tv.getDrawingCache());
		tv.destroyDrawingCache();
		
		return bitmap;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
		case R.id.id_takePhoto:
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			startActivityForResult(intent, TAKE_PHOTO);
			break;
		case R.id.id_getImage:
			Intent intent2 = new Intent(Intent.ACTION_PICK);
			intent2.setType("image/*");
			startActivityForResult(intent2, PICK_CODE);
			break;
			
		case R.id.id_Detect:
			Waitting.setVisibility(View.VISIBLE);
			if(mCurrentSrc != null && !mCurrentSrc.trim().equals("")){
				if (mCurrentSrc.equals("image_capture")){
					
				}
				else {resizePhoto();}
			}else{
				mPhotoImg = BitmapFactory.decodeResource(getResources(), R.drawable.t4);
			}
			
			FaceDetect.detect(mPhotoImg, new CallBack() {
				
				@Override
				public void success(JSONObject result) {
					// TODO Auto-generated method stub
					Message msg = Message.obtain();
					msg.what = MSG_SUCCESS;
					msg.obj = result;
					mhandle.sendMessage(msg);
				}
				
				@Override
				public void error(FaceppParseException exception) {
					// TODO Auto-generated method stub
					Message msg = Message.obtain();
					msg.what = MSG_SUCCESS;
					msg.obj = exception.getErrorMessage();
					mhandle.sendMessage(msg);
				}
			});
			break;
		default:
			break;
		}
	}

}
