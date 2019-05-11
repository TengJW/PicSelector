package com.luck.imaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;

import com.luck.imaging.core.IMGMode;
import com.luck.imaging.core.IMGText;
import com.luck.imaging.core.file.IMGAssetFileDecoder;
import com.luck.imaging.core.file.IMGDecoder;
import com.luck.imaging.core.file.IMGFileDecoder;
import com.luck.imaging.core.util.IMGUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by felix on 2017/11/14 下午2:26.
 */

public class IMGEditActivity extends IMGEditBaseActivity {

    private static final int MAX_WIDTH = 1024;

    private static final int MAX_HEIGHT = 1024;

    public static final String EXTRA_IMAGE_URI = "IMAGE_URI";

    public static final String EXTRA_IMAGE_CROPENABLE = "IMAGE_CROPENABLE";
    public static final String EXTRA_IMAGE_EDITENABLE = "IMAGE_EDITENABLE";
    public static final String EXTRA_IMAGE_SAVE_PATH = "IMAGE_SAVE_PATH";
    public static final String EXTRA_IMAGE_WATERMARK = "IMAGE_WATERMARK";
    public static final String EXTRA_IMAGE_WATERMARKTEXTCOLOR = "IMAGE_WATERMARK_TEXTCOlOR";
    public static final String EXTRA_IMAGE_WATERMARKTEXTSIZE = "IMAGE_WATERMARK_TEXTSIZE";
    public static final String EXTRA_IMAGE_WATERMARKBACKGROUNDCOLOR = "IMAGE_WATERMARK_BACKGROUNDCOlOR";
    public static final String EXTRA_IMAGE_WATERMARKGRAVITY = "IMAGE_WATERMARK_GRAVITY";

    @Override
    public void onCreated() {
        Intent intent = getIntent();
        if (intent == null) {
            return ;
        }
        boolean cropEnable = intent.getBooleanExtra(EXTRA_IMAGE_CROPENABLE, false);
        setCropEnable(cropEnable);
    }

    @Override
    public Bitmap getBitmap() {
        Intent intent = getIntent();
        if (intent == null) {
            return null;
        }

        Uri uri = intent.getParcelableExtra(EXTRA_IMAGE_URI);

        if (uri == null) {
            return null;
        }

        IMGDecoder decoder = null;

        String path = uri.getPath();
        if (!TextUtils.isEmpty(path)) {
            switch (uri.getScheme()) {
                case "asset":
                    decoder = new IMGAssetFileDecoder(this, uri);
                    break;
                case "file":
                    decoder = new IMGFileDecoder(uri);
                    break;
            }
        }

        if (decoder == null) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        options.inJustDecodeBounds = true;
        decoder.decode(options);

        WindowManager wm = (WindowManager) getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);


        int statusHeight = -1;
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            int height = Integer.parseInt(clazz.getField("status_bar_height")
                    .get(object).toString());
            statusHeight = getApplicationContext().getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        if (options.outWidth > MAX_WIDTH) {
//            options.inSampleSize = IMGUtils.inSampleSize(Math.round(1f * options.outWidth / MAX_WIDTH));
//        }
//
//        if (options.outHeight > MAX_HEIGHT) {
//            options.inSampleSize = Math.max(options.inSampleSize,
//                    IMGUtils.inSampleSize(Math.round(1f * options.outHeight / MAX_HEIGHT)));
//        }


        if (options.outWidth > outMetrics.widthPixels || options.outHeight > (outMetrics.heightPixels - statusHeight)) {

            if ((options.outWidth - outMetrics.widthPixels) > (options.outHeight - (outMetrics.heightPixels - statusHeight))) {

                options.inSampleSize = IMGUtils.inSampleSize(Math.round(1f * options.outWidth / outMetrics.widthPixels));
            } else {
                options.inSampleSize = IMGUtils.inSampleSize(Math.round(1f * options.outHeight / (outMetrics.heightPixels - statusHeight)));
            }

        }
//        if (options.outWidth > outMetrics.widthPixels) {
//            options.inSampleSize = IMGUtils.inSampleSize(Math.round(1f * options.outWidth / outMetrics.widthPixels));
//        }
//
//        if (options.outHeight > (outMetrics.heightPixels - statusHeight)) {
//            options.inSampleSize = Math.max(options.inSampleSize,
//                    IMGUtils.inSampleSize(Math.round(1f * options.outHeight / (outMetrics.heightPixels - statusHeight))));
//        }

        options.inJustDecodeBounds = false;

        Bitmap bitmap = decoder.decode(options);
        if (bitmap == null) {
            return null;
        }

        return bitmap;
    }

    @Override
    public void onText(IMGText text) {
        mImgView.addStickerText(text);
    }

    @Override
    public void onModeClick(IMGMode mode) {
        IMGMode cm = mImgView.getMode();
        if (cm == mode) {
            mode = IMGMode.NONE;
        }
        mImgView.setMode(mode);
        updateModeUI();

        if (mode == IMGMode.CLIP) {
            setOpDisplay(OP_CLIP);
        }
    }

    @Override
    public void onUndoClick() {
        IMGMode mode = mImgView.getMode();
        if (mode == IMGMode.DOODLE) {
            mImgView.undoDoodle();
        } else if (mode == IMGMode.MOSAIC) {
            mImgView.undoMosaic();
        }
    }

    @Override
    public void onCancelClick() {
        finish();
    }

    private BroadcastReceiver broadcastReceiver;

    @Override
    public void onDoneClick() {
        final String path = getIntent().getStringExtra(EXTRA_IMAGE_SAVE_PATH);
        String watermark = getIntent().getStringExtra(EXTRA_IMAGE_WATERMARK);
        int textcolor = getIntent().getIntExtra(EXTRA_IMAGE_WATERMARKTEXTCOLOR, Color.WHITE);
        int textsize = getIntent().getIntExtra(EXTRA_IMAGE_WATERMARKTEXTSIZE, 15);
        int backgroundColor = getIntent().getIntExtra(EXTRA_IMAGE_WATERMARKBACKGROUNDCOLOR, Color.TRANSPARENT);
        int gravity = getIntent().getIntExtra(EXTRA_IMAGE_WATERMARKGRAVITY, Gravity.BOTTOM | Gravity.RIGHT);
        if (watermark != null && watermark.length() > 0) {
            //广播监听 水印是否绘制完成
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(EXTRA_IMAGE_WATERMARK);
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent it) {
                    if (!TextUtils.isEmpty(path)) {
                        Bitmap bitmap = mImgView.saveBitmap();
                        if (bitmap != null) {
                            FileOutputStream fout = null;
                            try {
                                fout = new FileOutputStream(path);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fout);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } finally {
                                if (fout != null) {
                                    try {
                                        fout.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            Intent intent = new Intent();
                            intent.putExtra(EXTRA_IMAGE_SAVE_PATH, path);
                            IMGEditActivity.this.setResult(RESULT_OK, intent);
                            finish();
                            return;
                        }
                    }
                    IMGEditActivity.this.setResult(RESULT_CANCELED);
                    finish();


                }
            };
            registerReceiver(broadcastReceiver, intentFilter);
            mImgView.addWaterMark(new IMGText(watermark, textcolor, textsize, backgroundColor, gravity));
        } else {
            if (!TextUtils.isEmpty(path)) {
                Bitmap bitmap = mImgView.saveBitmap();
                if (bitmap != null) {
                    FileOutputStream fout = null;
                    try {
                        fout = new FileOutputStream(path);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fout);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        if (fout != null) {
                            try {
                                fout.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_IMAGE_SAVE_PATH, path);
                    setResult(RESULT_OK, intent);
                    finish();
                    return;
                }
            }
            setResult(RESULT_CANCELED);
            finish();
        }


    }

    @Override
    public void onCancelClipClick() {
        mImgView.cancelClip();
        setOpDisplay(mImgView.getMode() == IMGMode.CLIP ? OP_CLIP : OP_NORMAL);
    }

    @Override
    public void onDoneClipClick() {
        mImgView.doClip();
        setOpDisplay(mImgView.getMode() == IMGMode.CLIP ? OP_CLIP : OP_NORMAL);
    }

    @Override
    public void onResetClipClick() {
        mImgView.resetClip();
    }

    @Override
    public void onRotateClipClick() {
        mImgView.doRotate();
    }

    @Override
    public void onColorChanged(int checkedColor) {
        mImgView.setPenColor(checkedColor);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
