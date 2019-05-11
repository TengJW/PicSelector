package com.luck.imaging.core;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.Gravity;

/**
 * Created by felix on 2017/12/1 下午2:43.
 */

public class IMGText {

    private String text;

    private int color = Color.WHITE;
    private int textsize = 15;
    private int backGroundColor = Color.TRANSPARENT;
    private int gravity = Gravity.RIGHT | Gravity.BOTTOM;

    public IMGText(String text, int color) {
        this.text = text;
        this.color = color;
    }

    public IMGText(String text, int color,int textsize, int backGroundColor, int gravity) {
        this.text = text;
        this.color = color;
        this.textsize = textsize;
        this.backGroundColor = backGroundColor;
        this.gravity = gravity;
    }

    public int getGravity() {
        return gravity;
    }

    public int getTextsize() {
        return textsize;
    }

    public void setTextsize(int textsize) {
        this.textsize = textsize;
    }

    public void setGravity(int gravity) {
        this.gravity = gravity;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getColor() {
        return color;
    }

    public int getBackGroundColor() {
        return backGroundColor;
    }

    public void setBackGroundColor(int backGroundColor) {
        this.backGroundColor = backGroundColor;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(text);
    }

    public int length() {
        return isEmpty() ? 0 : text.length();
    }

    @Override
    public String toString() {
        return "IMGText{" +
                "text='" + text + '\'' +
                ", color=" + color +
                '}';
    }
}
