package com.luck.imaging;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        } else finish();
    }

    public void onCreated() {

    }

    protected void setCropEnable(boolean enable){

        try {
            mImgView.setCatEnable(enable);
            if (!enable){
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
