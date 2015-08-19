package com.how_old.app;

import java.io.ByteArrayOutputStream;

import org.apache.http.HttpRequest;
import org.json.JSONObject;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import android.graphics.Bitmap;
import android.hardware.Camera.Parameters;
import android.util.Log;

public class FaceDetect {

	public interface CallBack{
		
		void success(JSONObject result);
		void error(FaceppParseException exception);
	}
	public static void detect(final Bitmap bitMap,final CallBack callBack){
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					HttpRequests httpRequests = new HttpRequests(Content.Key,Content.Secret,true,true);
					Bitmap bmSmall =  Bitmap.createBitmap(bitMap, 0, 0, bitMap.getWidth(), bitMap.getHeight());
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					bmSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
					byte[] arrays = stream.toByteArray();
					
					PostParameters params = new PostParameters();
					params.setImg(arrays);
					JSONObject result = httpRequests.detectionDetect(params);
					
					Log.e("TAG", result.toString());
					
					if(callBack != null){
						callBack.success(result);
						
					}
				} catch (FaceppParseException e) {
					// TODO: handle exception
					if(callBack != null){
						callBack.error(e);
					}
				}
				
			}
		}).start();
	}
}
