package com.example.demo;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class ShowPicture extends Activity {
    ImageView imageView1;
    ImageView imageView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_picture);
        imageView1 = findViewById(R.id.imageView);
        imageView2 = findViewById(R.id.imageView2);

        Intent intent = getIntent();
        String mainName = intent.getStringExtra(DualCameraFragment.EXTRA_MAIN);
        String subName = intent.getStringExtra(DualCameraFragment.EXTRA_SUB);

        Log.d("name", mainName + " " + subName + " ");


        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        int screenWidth = wm.getDefaultDisplay().getWidth();

        ViewGroup.LayoutParams lp = imageView1.getLayoutParams();
        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.height = screenWidth;
        imageView1.setLayoutParams(lp);

        imageView1.setMaxWidth(screenWidth);
        imageView1.setMaxHeight(screenWidth);

        ViewGroup.LayoutParams lp2 = imageView2.getLayoutParams();
        lp2.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp2.height = screenWidth;
        imageView2.setLayoutParams(lp2);

        imageView2.setMaxWidth(screenWidth);
        imageView2.setMaxHeight(screenWidth);

//        String sdPath = Environment.getExternalStorageDirectory().getPath();
        String prefix = getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/";
        String mainPicturePath = prefix + mainName;
        String subPicturePath = prefix + subName;

        imageView1.setImageURI(Uri.parse(mainPicturePath));
        imageView2.setImageURI(Uri.parse(subPicturePath));
    }
}
