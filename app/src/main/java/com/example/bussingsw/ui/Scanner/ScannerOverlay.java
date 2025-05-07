package com.example.bussingsw.ui.Scanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class ScannerOverlay extends View {

    private Paint cornerPaint;
    private Rect boxRect;
    private int cornerLength = 60; // Length of the corner lines
    private int cornerThickness = 10;

    public ScannerOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        cornerPaint = new Paint();
        cornerPaint.setColor(0xFFFFFFFF); // White color
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(cornerThickness);
        cornerPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        int boxSize = Math.min(width, height) * 3 / 4;

        int left = (width - boxSize) / 2;
        int top = (height - boxSize) / 2;
        int right = left + boxSize;
        int bottom = top + boxSize;

        boxRect = new Rect(left, top, right, bottom);

        // Draw corners only
        // Top-left
        canvas.drawLine(left, top, left + cornerLength, top, cornerPaint);
        canvas.drawLine(left, top, left, top + cornerLength, cornerPaint);

        // Top-right
        canvas.drawLine(right, top, right - cornerLength, top, cornerPaint);
        canvas.drawLine(right, top, right, top + cornerLength, cornerPaint);

        // Bottom-left
        canvas.drawLine(left, bottom, left + cornerLength, bottom, cornerPaint);
        canvas.drawLine(left, bottom, left, bottom - cornerLength, cornerPaint);

        // Bottom-right
        canvas.drawLine(right, bottom, right - cornerLength, bottom, cornerPaint);
        canvas.drawLine(right, bottom, right, bottom - cornerLength, cornerPaint);
    }

    public Rect getBoxRect() {
        return boxRect;
    }
}
