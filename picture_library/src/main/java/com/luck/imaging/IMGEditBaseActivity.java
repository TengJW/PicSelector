package com.luck.imaging;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.ViewSwitcher;

import com.luck.imaging.core.IMGMode;
import com.luck.imaging.core.IMGText;
import com.luck.imaging.view.IMGColorGroup;
import com.luck.imaging.view.IMGView;
import com.luck.picture.lib.R;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.ucrop.PicturePhotoGalleryAdapter;
import com.luck.ucrop.model.CutInfo;

import java.util.List;

import static com.luck.imaging.IMGEditActivity.EXTRA_IMAGE_URI;
import static com.luck.imaging.IMGEditActivity.EXTRA_SELECTION_MODE;

/**
 * Created by felix on 2017/12/5 下午3:08.
 */

abstract class IMGEditBaseActivity extends Activity implements View.OnClickListener,
        IMGTextEditDialog.Callback, RadioGroup.OnCheckedChangeListener,
        DialogInterface.OnShowListener, DialogInterface.OnDismissListener {

    protected IMGView mImgView;

    private RadioGroup mModeGroup;

    private IMGColorGroup mColorGroup;

    private IMGTextEditDialog mTextDialog;

    private View mLayoutOpSub;

    private ViewSwitcher mOpSwitcher, mOpSubSwitcher;

    public static final int OP_HIDE = -1;

    public static final int OP_NORMAL = 0;

    public static final int OP_CLIP = 1;

    public static final int OP_SUB_DOODLE = 0;

    public static final int OP_SUB_MOSAIC = 1;

    protected RecyclerView imgsRcv;
    //    protected Uri uri;
//    protected List<Uri> uris;
    protected List<CutInfo> cutInfos;
    protected CutInfo cutInfo;
    protected int selectionMode;
    protected PicturePhotoGalleryAdapter adapter;
    protected int cutIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null) {
            finish();
        }
        selectionMode = intent.getIntExtra(EXTRA_SELECTION_MODE, PictureConfig.SINGLE);
        if (selectionMode == PictureConfig.SINGLE) {
            cutInfo = (CutInfo) intent.getSerializableExtra(EXTRA_IMAGE_URI);
            setCropData();
        } else {
//            uris =(List<Uri>) intent.getParcelableExtra(EXTRA_IMAGE_URI);
//            uri=uris.get(cutIndex);

            cutInfos = (List<CutInfo>) intent.getSerializableExtra(EXTRA_IMAGE_URI);
            cutInfo = cutInfos.get(cutIndex);
            setCropData();
        }
    }

    protected void setCropData() {
        Bitmap bitmap = getBitmap();
        if (bitmap != null) {
            setContentView(R.layout.image_edit_activity);
            initViews();
            try {
                WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                DisplayMetrics outMetrics = new DisplayMetrics();
                wm.getDefaultDisplay().getMetrics(outMetrics);
                int windowWidth = outMetrics.widthPixels;
                float mult = windowWidth / (float) bitmap.getWidth();
                float newHeight = (float) bitmap.getHeight() * mult;
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, (int) newHeight);
                params.gravity = Gravity.CENTER;
                mImgView.setLayoutParams(params);
            } catch (Exception e) {
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, bitmap.getHeight());
                params.gravity = Gravity.CENTER;
                mImgView.setLayoutParams(params);
            }
            mImgView.setImageBitmap(bitmap);
            onCreated();
            // 预览图 一页5个,裁剪到第6个的时候滚动到最新位置，不然预览图片看不到
            try {
                if (cutIndex >= 5) {
                    imgsRcv.scrollToPosition(cutIndex);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else finish();
    }


    public void onCreated() {

    }

    protected void setCropEnable(boolean enable) {

        try {
            mImgView.setCatEnable(enable);
            if (!enable) {
                findViewById(R.id.btn_clip).setVisibility(View.GONE);//裁剪功能
            }
            findViewById(R.id.rb_mosaic).setVisibility(View.GONE);//马赛克功能
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initViews() {
        mImgView = findViewById(R.id.image_canvas);
        mModeGroup = findViewById(R.id.rg_modes);

        mOpSwitcher = findViewById(R.id.vs_op);
        mOpSubSwitcher = findViewById(R.id.vs_op_sub);

        mColorGroup = findViewById(R.id.cg_colors);
        mColorGroup.setOnCheckedChangeListener(this);

        mLayoutOpSub = findViewById(R.id.layout_op_sub);

//        imgsRcv = findViewById(R.id.imgsRcv);
//        imgsRcv = (RecyclerView) findViewById(R.id.recyclerView);
//        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
//        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
//        imgsRcv.setLayoutManager(mLayoutManager);
//        adapter = new PicturePhotoGalleryAdapter(this, uris);
//        imgsRcv.setAdapter(adapter);

        imgsRcv = (RecyclerView) findViewById(R.id.imgsRcv);
        if (selectionMode == PictureConfig.SINGLE) {
            imgsRcv.setVisibility(View.GONE);
        } else {
            imgsRcv.setVisibility(View.VISIBLE);
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
            mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
            imgsRcv.setLayoutManager(mLayoutManager);
            for (CutInfo info : cutInfos) {
                info.setCut(false);
            }
            cutInfos.get(cutIndex).setCut(true);
            adapter = new PicturePhotoGalleryAdapter(this, cutInfos);
            imgsRcv.setAdapter(adapter);
        }

    }


    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.rb_doodle) {
            onModeClick(IMGMode.DOODLE);
        } else if (vid == R.id.btn_text) {
            onTextModeClick();
        } else if (vid == R.id.rb_mosaic) {
            onModeClick(IMGMode.MOSAIC);
        } else if (vid == R.id.btn_clip) {
            onModeClick(IMGMode.CLIP);
        } else if (vid == R.id.btn_undo) {
            onUndoClick();
        } else if (vid == R.id.tv_done) {
            onDoneClick();
        } else if (vid == R.id.tv_cancel) {
            onCancelClick();
        } else if (vid == R.id.ib_clip_cancel) {
            onCancelClipClick();
        } else if (vid == R.id.ib_clip_done) {
            onDoneClipClick();
        } else if (vid == R.id.tv_clip_reset) {
            onResetClipClick();
        } else if (vid == R.id.ib_clip_rotate) {
            onRotateClipClick();
        }
    }

    public void updateModeUI() {
        IMGMode mode = mImgView.getMode();
        switch (mode) {
            case DOODLE:
                mModeGroup.check(R.id.rb_doodle);
                setOpSubDisplay(OP_SUB_DOODLE);
                break;
            case MOSAIC:
                mModeGroup.check(R.id.rb_mosaic);
                setOpSubDisplay(OP_SUB_MOSAIC);
                break;
            case NONE:
                mModeGroup.clearCheck();
                setOpSubDisplay(OP_HIDE);
                break;
        }
    }

    public void onTextModeClick() {
        if (mTextDialog == null) {
            mTextDialog = new IMGTextEditDialog(this, this);
            mTextDialog.setOnShowListener(this);
            mTextDialog.setOnDismissListener(this);
        }
        mTextDialog.show();
    }

    @Override
    public final void onCheckedChanged(RadioGroup group, int checkedId) {
        onColorChanged(mColorGroup.getCheckColor());
    }

    public void setOpDisplay(int op) {
        if (op >= 0) {
            mOpSwitcher.setDisplayedChild(op);
        }
    }

    public void setOpSubDisplay(int opSub) {
        if (opSub < 0) {
            mLayoutOpSub.setVisibility(View.GONE);
        } else {
            mOpSubSwitcher.setDisplayedChild(opSub);
            mLayoutOpSub.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onShow(DialogInterface dialog) {
        mOpSwitcher.setVisibility(View.GONE);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mOpSwitcher.setVisibility(View.VISIBLE);
    }

    public abstract Bitmap getBitmap();

    public abstract void onModeClick(IMGMode mode);

    public abstract void onUndoClick();

    public abstract void onCancelClick();

    public abstract void onDoneClick();

    public abstract void onCancelClipClick();

    public abstract void onDoneClipClick();

    public abstract void onResetClipClick();

    public abstract void onRotateClipClick();

    public abstract void onColorChanged(int checkedColor);

    @Override
    public abstract void onText(IMGText text);
}
