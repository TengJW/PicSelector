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
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.R;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.ucrop.UCropMulti;
import com.luck.ucrop.model.CutInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by felix on 2017/11/14 下午2:26.
 */

public class IMGEditActivity extends IMGEditBaseActivity {

    private static final int MAX_WIDTH = 1024;

    private static final int MAX_HEIGHT = 1024;

    public static final String EXTRA_IMAGE_URI = "IMAGE_URI";


    public static final String EXTRA_SELECTION_MODE = "SELECTIONMODE";

    public static final String EXTRA_IMAGE_CROPENABLE = "IMAGE_CROPENABLE";
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
            return;
        }
        boolean cropEnable = intent.getBooleanExtra(EXTRA_IMAGE_CROPENABLE, false);
        setCropEnable(cropEnable);
    }

    @Override
    public Bitmap getBitmap() {

        if (cutInfo == null) {
            return null;
        }
        IMGDecoder decoder = null;
        String path = cutInfo.getPath();
        Uri uri = null;
        try {
            uri = Uri.fromFile(new File(path));
        } catch (Exception e) {
            return null;
        }
        if (uri == null) {
            return null;
        }
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

        if (options.outWidth > outMetrics.widthPixels || options.outHeight > (outMetrics.heightPixels - statusHeight)) {
            int widthScale = Math.round(options.outWidth / outMetrics.widthPixels);
            int heightScale = Math.round(options.outHeight / (outMetrics.heightPixels - statusHeight));
            options.inSampleSize = widthScale > heightScale ? widthScale : heightScale;
        }
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
        if (selectionMode == PictureConfig.SINGLE) {
            singleSaveImg();
        } else {
            MultSaveImg();
        }

    }

    private void singleSaveImg() {
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

                    //编辑暂存文件
                    File file = new File(getCacheDir(), System.currentTimeMillis() + ".jpg");
                    Bitmap bitmap = mImgView.saveBitmap();
                    if (bitmap != null) {
                        FileOutputStream fout = null;
                        try {
                            fout = new FileOutputStream(file);
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
                        intent.putExtra(EXTRA_IMAGE_SAVE_PATH, file.getAbsolutePath());
                        IMGEditActivity.this.setResult(RESULT_OK, intent);
                        finish();

                        return;
                    }
                }
            };
            registerReceiver(broadcastReceiver, intentFilter);
            mImgView.addWaterMark(new IMGText(watermark, textcolor, textsize, backgroundColor, gravity));
        } else {

            //编辑暂存文件
            File file = new File(getCacheDir(), System.currentTimeMillis() + ".jpg");
            Bitmap bitmap = mImgView.saveBitmap();
            if (bitmap != null) {
                FileOutputStream fout = null;
                try {
                    fout = new FileOutputStream(file);
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
                intent.putExtra(EXTRA_IMAGE_SAVE_PATH, file.getAbsolutePath());
                setResult(RESULT_OK, intent);
                finish();
                return;
            }
        }

    }

    private void MultSaveImg() {
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
                    saveImgFile();
                    cutIndex++;
                    if (cutIndex >= cutInfos.size()) {
                        IMGEditActivity.this.setResult(RESULT_OK, new Intent()
                                .putExtra(UCropMulti.EXTRA_OUTPUT_URI_LIST, (Serializable) cutInfos)
                        );
                        finish();
                        overridePendingTransition(0, R.anim.ucrop_close);
                    } else {
                        cutInfo = cutInfos.get(cutIndex);
                        setCropData();
                    }
                    unregisterReceiver(broadcastReceiver);
                }
            };
            registerReceiver(broadcastReceiver, intentFilter);
            mImgView.addWaterMark(new IMGText(watermark, textcolor, textsize, backgroundColor, gravity));
        } else {
            saveImgFile();
            if (cutIndex >= cutInfos.size()) {
                List<LocalMedia> images = new ArrayList<>();
                for (CutInfo c : cutInfos) {
                    LocalMedia media = new LocalMedia();
                    String imageType = PictureMimeType.createImageType(c.getPath());
                    media.setCut(true);
                    media.setPath(c.getPath());
                    media.setCutPath(c.getPath());
                    media.setPictureType(imageType);
                    images.add(media);
                }
                Intent intent = PictureSelector.putIntentResult(images);
                IMGEditActivity.this.setResult(RESULT_OK, intent);
                finish();
            } else {
                cutIndex++;
                cutInfo = cutInfos.get(cutIndex);
                setCropData();
            }
        }
    }

    private void saveImgFile() {
        //编辑暂存文件
        File file = new File(getCacheDir(), System.currentTimeMillis() + ".jpg");
        Bitmap bitmap = mImgView.saveBitmap();
        if (bitmap != null) {
            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream(file);
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
            cutInfo.setCut(true);
            cutInfo.setCutPath(file.getAbsolutePath());
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
