/*
 * Copyright 2017 Chaos Leong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chaos.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputFilter;
import android.text.TextPaint;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;

/**
 * Provides a widget for enter PIN/OTP/password etc.
 *
 * @author Chaos Leong
 *         01/04/2017
 *
 * <p>
 * <b>XML attributes</b>
 * <p>
 * See <a href="https://developer.android.com/reference/android/R.styleable.html#EditText">EditText Attributes</a>,
 * <a href="https://developer.android.com/reference/android/R.styleable.html#TextView">TextView Attributes</a>,
 * <a href="https://developer.android.com/reference/android/R.styleable.html#View">View Attributes</a>
 * @attr ref R.styleable#PinView_boxCount
 * @attr ref R.styleable#PinView_boxHeight
 * @attr ref R.styleable#PinView_boxRadius
 * @attr ref R.styleable#PinView_boxMargin
 * @attr ref R.styleable#PinView_borderWidth
 * @attr ref R.styleable#PinView_borderColor
 */

public class PinView extends AppCompatEditText {

    private static final String TAG = "PinView";

    private static final boolean DBG = false;

    private static final int DEFAULT_COUNT = 4;

    private static final InputFilter[] NO_FILTERS = new InputFilter[0];

    private int mPinBoxCount;

    private float mPinBoxHeight;
    private int mPinBoxRadius;
    private int mPinBoxMargin;

    private final Paint mPaint;
    private final TextPaint mTextPaint;
    private final Paint mAnimatorTextPaint;

    private ColorStateList mBorderColor;
    private int mCurBorderColor = Color.BLACK;
    private int mBorderWidth;

    private final Rect mTextRect = new Rect();
    private final RectF mBoxBorderRect = new RectF();
    private final Path mPath = new Path();
    private final PointF mBoxCenterPoint = new PointF();

    private ValueAnimator mDefaultAddAnimator;
    private boolean isAnimationEnable = false;

    public PinView(Context context) {
        this(context, null);
    }

    public PinView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.pinViewStyle);
    }

    public PinView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final Resources res = getResources();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.density = res.getDisplayMetrics().density;
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextSize(getTextSize());

        mAnimatorTextPaint = new TextPaint(mTextPaint);

        final Resources.Theme theme = context.getTheme();

        TypedArray a = theme.obtainStyledAttributes(attrs, R.styleable.PinView, defStyleAttr, 0);

        mPinBoxCount = a.getInt(R.styleable.PinView_boxCount, DEFAULT_COUNT);
        mPinBoxHeight = a.getDimensionPixelSize(R.styleable.PinView_boxHeight,
                res.getDimensionPixelOffset(R.dimen.pv_pin_view_box_height));
        mPinBoxMargin = a.getDimensionPixelOffset(R.styleable.PinView_boxMargin,
                res.getDimensionPixelOffset(R.dimen.pv_pin_view_box_margin));
        mBorderWidth = a.getDimensionPixelOffset(R.styleable.PinView_borderWidth,
                res.getDimensionPixelOffset(R.dimen.pv_pin_view_box_border_width));
        mPinBoxRadius = a.getDimensionPixelOffset(R.styleable.PinView_boxRadius,
                res.getDimensionPixelOffset(R.dimen.pv_pin_view_box_radius));
        mBorderColor = a.getColorStateList(R.styleable.PinView_borderColor);

        a.recycle();

        setMaxLength(mPinBoxCount);
        mPaint.setStrokeWidth(mBorderWidth);
        setupAnimator();

        setCursorVisible(false);
        setTextIsSelectable(false);
    }

    private void setMaxLength(int maxLength) {
        if (maxLength >= 0) {
            setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        } else {
            setFilters(NO_FILTERS);
        }
    }

    private void setupAnimator() {
        mDefaultAddAnimator = ValueAnimator.ofFloat(0.5f, 1f);
        mDefaultAddAnimator.setDuration(150);
        mDefaultAddAnimator.setInterpolator(new DecelerateInterpolator());
        mDefaultAddAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float scale = (float) animation.getAnimatedValue();
                int alpha = (int) (255 * scale);
                mAnimatorTextPaint.setTextSize(getTextSize() * scale);
                mAnimatorTextPaint.setAlpha(alpha);
                postInvalidate();
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        float boxHeight = mPinBoxHeight;

        if (widthMode == MeasureSpec.EXACTLY) {
            // Parent has told us how big to be. So be it.
            width = widthSize;
        } else {
            float boxesWidth = (mPinBoxCount - 1) * mPinBoxMargin + mPinBoxCount * boxHeight;
            width = Math.round(boxesWidth + getPaddingRight() + getPaddingLeft());
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            // Parent has told us how big to be. So be it.
            height = heightSize;
        } else {
            height = Math.round(boxHeight + getPaddingTop() + getPaddingBottom());
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);

        if (start != text.length()) {
            moveCursorToEnd();
        }

        if (isAnimationEnable) {
            final boolean isAdd = lengthAfter - lengthBefore > 0;
            if (isAdd) {
                if (mDefaultAddAnimator != null) {
                    mDefaultAddAnimator.end();
                    mDefaultAddAnimator.start();
                }
            }
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);

        if (focused) {
            moveCursorToEnd();
        }
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);

        if (selEnd != getText().length()) {
            moveCursorToEnd();
        }
    }

    private void moveCursorToEnd() {
        setSelection(getText().length());
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (mBorderColor == null || mBorderColor.isStateful()) {
            updateColors();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();

        mPaint.setColor(mCurBorderColor);
        mPaint.setStrokeWidth(mBorderWidth);
        mTextPaint.setColor(getCurrentTextColor());

        drawPinBox(canvas);

        canvas.restore();
    }

    private void drawPinBox(Canvas canvas) {
        for (int i = 0; i < mPinBoxCount; i++) {
            updateBoxRectF(i);
            updateCenterPoint();

            boolean l, r;
            l = r = true;
            if (mPinBoxMargin == 0) {
                if (mPinBoxCount > 1) {
                    if (i == 0) {
                        // draw only left round
                        r = false;
                    } else if (i == mPinBoxCount - 1) {
                        // draw only right round
                        l = false;
                    } else {
                        // draw rect
                        l = r = false;
                    }
                }
            }
            updateRoundRectPath(mBoxBorderRect, mPinBoxRadius, mPinBoxRadius, l, r);
            canvas.drawPath(mPath, mPaint);

            if (getText().length() > i) {
                if (DBG) {
                    drawAnchorLine(canvas);
                }

                if (isPasswordInputType(getInputType())) {
                    drawCircle(canvas, i);
                } else {
                    drawText(canvas, i);
                }
            }
        }
    }

    private void updateRoundRectPath(RectF rectF, float rx, float ry, boolean l, boolean r) {
        updateRoundRectPath(rectF, rx, ry, l, r, r, l);
    }

    private void updateRoundRectPath(RectF rectF, float rx, float ry,
                                     boolean tl, boolean tr, boolean br, boolean bl) {
        mPath.reset();

        float l = rectF.left;
        float t = rectF.top;
        float r = rectF.right;
        float b = rectF.bottom;

        float w = r - l;
        float h = b - t;

        float lw = w - 2 * rx;// line width
        float lh = h - 2 * ry;// line height

        mPath.moveTo(l, t + ry);

        if (tl) {
            mPath.rQuadTo(0, -ry, rx, -ry);// top-left corner
        } else {
            mPath.rLineTo(0, -ry);
            mPath.rLineTo(rx, 0);
        }

        mPath.rLineTo(lw, 0);

        if (tr) {
            mPath.rQuadTo(rx, 0, rx, ry);// top-right corner
        } else {
            mPath.rLineTo(rx, 0);
            mPath.rLineTo(0, ry);
        }

        mPath.rLineTo(0, lh);

        if (br) {
            mPath.rQuadTo(0, ry, -rx, ry);// bottom-right corner
        } else {
            mPath.rLineTo(0, ry);
            mPath.rLineTo(-rx, 0);
        }

        mPath.rLineTo(-lw, 0);

        if (bl) {
            mPath.rQuadTo(-rx, 0, -rx, -ry);// bottom-left corner
        } else {
            mPath.rLineTo(-rx, 0);
            mPath.rLineTo(0, -ry);
        }

        mPath.rLineTo(0, -lh);

        mPath.close();
    }

    private void updateBoxRectF(int i) {
        float startX = (getWidth() - (mPinBoxCount - 1) * mPinBoxMargin - mPinBoxCount * mPinBoxHeight) / 2;

        float left = startX + mPinBoxHeight * i + mPinBoxMargin * i + mBorderWidth;
        float right = left + mPinBoxHeight - 2 * mBorderWidth;
        float top = mBorderWidth + getPaddingTop();
        float bottom = top + mPinBoxHeight - 2 * mBorderWidth;

        mBoxBorderRect.set(left, top, right, bottom);
    }

    private void drawText(Canvas canvas, int i) {
        Paint paint = getPaintByIndex(i);
        paint.getTextBounds(getText().toString(), i, i + 1, mTextRect);
        // 1, Rect(4, -39, 20, 0)
        // 您, Rect(2, -47, 51, 3)
        // *, Rect(0, -39, 23, -16)1
        // =, Rect(4, -26, 26, -10)
        // -, Rect(1, -19, 14, -14)
        // +, Rect(2, -32, 29, -3)
        float cx = mBoxCenterPoint.x;
        float cy = mBoxCenterPoint.y;
        float x = cx - Math.abs(mTextRect.width()) / 2 - mTextRect.left;
        float y = cy + Math.abs(mTextRect.height()) / 2 - mTextRect.bottom;// always center vertical
        canvas.drawText(getText(), i, i + 1, x, y, paint);
    }

    private void drawCircle(Canvas canvas, int i) {
        Paint paint = getPaintByIndex(i);
        float cx = mBoxCenterPoint.x;
        float cy = mBoxCenterPoint.y;
        canvas.drawCircle(cx, cy, paint.getTextSize() / 2, paint);
    }

    private Paint getPaintByIndex(int i) {
        if (isAnimationEnable && i == getText().length() - 1) {
            mAnimatorTextPaint.setColor(mTextPaint.getColor());
            return mAnimatorTextPaint;
        } else {
            return mTextPaint;
        }
    }

    /**
     * For seeing the font position
     */
    private void drawAnchorLine(Canvas canvas) {
        float cx = mBoxCenterPoint.x;
        float cy = mBoxCenterPoint.y;
        mPaint.setStrokeWidth(1);
        cx -= mPaint.getStrokeWidth() / 2;
        cy -= mPaint.getStrokeWidth() / 2;

        mPath.reset();
        mPath.moveTo(cx, mBoxBorderRect.top);
        mPath.lineTo(cx, mBoxBorderRect.top + Math.abs(mBoxBorderRect.height()));
        canvas.drawPath(mPath, mPaint);

        mPath.reset();
        mPath.moveTo(mBoxBorderRect.left, cy);
        mPath.lineTo(mBoxBorderRect.left + Math.abs(mBoxBorderRect.width()), cy);
        canvas.drawPath(mPath, mPaint);

        mPath.reset();

        mPaint.setStrokeWidth(mBorderWidth);
    }

    private void updateColors() {
        boolean inval = false;

        int color;
        if (mBorderColor != null) {
            color = mBorderColor.getColorForState(getDrawableState(), 0);
        } else {
            color = getCurrentTextColor();
        }

        if (color != mCurBorderColor) {
            mCurBorderColor = color;
            inval = true;
        }

        if (inval) {
            invalidate();
        }
    }

    private void updateCenterPoint() {
        float cx = mBoxBorderRect.left + Math.abs(mBoxBorderRect.width()) / 2;
        float cy = mBoxBorderRect.top + Math.abs(mBoxBorderRect.height()) / 2;
        mBoxCenterPoint.set(cx, cy);
    }

    private static boolean isPasswordInputType(int inputType) {
        final int variation =
                inputType & (EditorInfo.TYPE_MASK_CLASS | EditorInfo.TYPE_MASK_VARIATION);
        return variation
                == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD)
                || variation
                == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD)
                || variation
                == (EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD);
    }

    @Override
    protected MovementMethod getDefaultMovementMethod() {
        // we don't need arrow key, return null will also disable the copy/paste/cut pop-up menu.
        return null;
    }

    /**
     * Sets the border color for all the states (normal, selected,
     * focused) to be this color.
     *
     * @param color A color value in the form 0xAARRGGBB.
     *              Do not pass a resource ID. To get a color value from a resource ID, call
     *              {@link android.support.v4.content.ContextCompat#getColor(Context, int) getColor}.
     * @attr ref R.styleable#PinView_borderColor
     * @see #setBorderColor(ColorStateList)
     * @see #getBorderColors()
     */
    public void setBorderColor(@ColorInt int color) {
        mBorderColor = ColorStateList.valueOf(color);
        updateColors();
    }

    /**
     * Sets the border color.
     *
     * @attr ref R.styleable#PinView_borderColor
     * @see #setBorderColor(int)
     * @see #getBorderColors()
     */
    public void setBorderColor(ColorStateList colors) {
        if (colors == null) {
            throw new NullPointerException();
        }

        mBorderColor = colors;
        updateColors();
    }

    /**
     * Gets the border colors for the different states (normal, selected, focused) of the PinView.
     *
     * @attr ref R.styleable#PinView_borderColor
     * @see #setBorderColor(ColorStateList)
     * @see #setBorderColor(int)
     */
    public ColorStateList getBorderColors() {
        return mBorderColor;
    }

    /**
     * <p>Return the current color selected for normal border.</p>
     *
     * @return Returns the current border color.
     */
    @ColorInt
    public int getCurrentBorderColor() {
        return mCurBorderColor;
    }

    /**
     * Sets the border width.
     *
     * @attr ref R.styleable#PinView_borderWidth
     * @see #getBorderWidth()
     */
    public void setBorderWidth(@Px int borderWidth) {
        mBorderWidth = borderWidth;
    }

    /**
     * @return Returns the width of the box's border.
     * @see #setBorderWidth(int)
     */
    @Px
    public int getBorderWidth() {
        return mBorderWidth;
    }

    /**
     * Sets the count of boxes.
     *
     * @attr ref R.styleable#PinView_boxCount
     * @see #getBoxCount()
     */
    public void setBoxCount(int len) {
        mPinBoxCount = len;
        setMaxLength(len);
        requestLayout();
    }

    /**
     * @return Returns the count of the boxes.
     * @see #setBoxCount(int)
     */
    public int getBoxCount() {
        return mPinBoxCount;
    }

    /**
     * Sets the radius of box's border.
     *
     * @attr ref R.styleable#PinView_boxRadius
     * @see #getBoxRadius()
     */
    public void setBoxRadius(@Px int pinBoxRadius) {
        mPinBoxRadius = pinBoxRadius;
    }

    /**
     * @return Returns the radius of boxes's border.
     * @see #setBoxRadius(int)
     */
    @Px
    public int getBoxRadius() {
        return mPinBoxRadius;
    }

    /**
     * Specifies extra space between the boxes.
     *
     * @attr ref R.styleable#PinView_boxMargin
     * @see #getBoxMargin()
     */
    public void setBoxMargin(@Px int pinBoxMargin) {
        mPinBoxMargin = pinBoxMargin;
        requestLayout();
    }

    /**
     * @return Returns the margin between of the boxes.
     * @see #setBoxMargin(int)
     */
    @Px
    public int getBoxMargin() {
        return mPinBoxMargin;
    }

    /**
     * Sets the height and width of box.
     *
     * @attr ref R.styleable#PinView_boxHeight
     * @see #getBoxHeight()
     */
    public void setBoxHeight(float boxHeight) {
        mPinBoxHeight = boxHeight;
    }

    /**
     * @return Returns the height of box.
     * @see #setBoxHeight(float)
     */
    public float getBoxHeight() {
        return mPinBoxHeight;
    }

    /**
     * Specifies whether the text animation should be enabled or disabled.
     * By the default, the animation is disabled.
     *
     * @param enable True to start animation when adding text, false to transition immediately
     */
    public void setAnimationEnable(boolean enable) {
        isAnimationEnable = enable;
    }
}