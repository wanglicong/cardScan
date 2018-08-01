package com.riso.scancard0731;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import io.card.payment.CardIOActivity;


/**
 * @author 王黎聪
 * 2018年7月31日19:04:25
 */
public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private Button btn;
    private TextView tv;
    private String bitmapBase64;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);
        btn = (Button) findViewById(R.id.button);
        tv = (TextView) findViewById(R.id.tv);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        gotoNet();
                    }
                }).start();
            }
        });


    }

    public void clickBtn(View view) {
        CardIOActivity.activityStart(this, 0xFF00C5DC, 70);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (null != data && data.hasExtra(CardIOActivity.EXTRA_CAPTURED_CARD_IMAGE)) {
            byte[] byteArrayExtra = data.getByteArrayExtra(CardIOActivity.EXTRA_CAPTURED_CARD_IMAGE);
            int length = byteArrayExtra.length;
            System.out.println("压缩前的大小====" + (length / 1024f / 1024f) + " M");
            tv.setText((length / 1024f / 1024f) + " M");
            Bitmap bitmap = BitmapFactory.decodeByteArray(byteArrayExtra, 0, length);
            imageView.setImageBitmap(bitmap);
            bitmapBase64 = bitmapToBase64(bitmap);
        }
    }

    public void startPhotoZoom(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        // 下面这个crop=true是设置在开启的Intent中设置显示的VIEW可裁剪
        intent.putExtra("crop", "true");
        //该参数可以不设定用来规定裁剪区的宽高比
        intent.putExtra("aspectX", 90);
        intent.putExtra("aspectY", 54);
        //该参数设定为你的imageView的大小
        intent.putExtra("outputX", 540);
        intent.putExtra("outputY", 324);
        intent.putExtra("scale", true);
        //是否返回bitmap对象
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(getCacheDir().getAbsolutePath() + "/temp.jpg")));
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());//输出图片的格式
        startActivityForResult(intent, 3);
    }


    private void gotoNet() {
        try {
            String host = "https://dm-57.data.aliyun.com";
            String path = "/rest/160601/ocr/ocr_business_card.json";
            String method = "POST";
            String appcode = "eb53bd07ee34418098691c7c08494ee9";
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Authorization", "APPCODE " + appcode);
            headers.put("Content-Type", "application/json; charset=UTF-8");
            String bodys = "{\"inputs\":[{\"image\":{\"dataType\":50,\"dataValue\":\"" + bitmapBase64 + "\"}}]}";

            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(host + path).openConnection();
            httpURLConnection.setConnectTimeout(15000);
            httpURLConnection.setReadTimeout(15000);
            httpURLConnection.setRequestMethod(method);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setRequestProperty("Charset", "UTF-8");
            httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            httpURLConnection.setRequestProperty("accept", "application/json");
            httpURLConnection.setRequestProperty("Authorization", "APPCODE " + appcode);

            byte[] writebytes = bodys.getBytes();
            httpURLConnection.setRequestProperty("Content-Length", String.valueOf(writebytes.length));
            OutputStream outwritestream = httpURLConnection.getOutputStream();
            outwritestream.write(bodys.getBytes());
            outwritestream.flush();
            outwritestream.close();
            if (httpURLConnection.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                final StringBuilder stringBuilder = new StringBuilder();
                String readLine;
                while ((readLine = reader.readLine()) != null) {
                    stringBuilder.append(readLine);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv.setText(stringBuilder.toString());
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 转换成 Base64
     * @param bitmap
     * @return
     */
    public String bitmapToBase64(Bitmap bitmap) {

        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                System.out.println("压缩后的大小====" + (bitmapBytes.length / 1024f / 1024f) + " M");
                result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }


    public String bitmapToBase64(byte[] byteArr) {
        return Base64.encodeToString(byteArr, Base64.DEFAULT);
    }
}
