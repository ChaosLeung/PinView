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
import android.text.TextUtils;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;

/**
 * Provides a widget for enter PIN/OTP/password etc.
 *
 * @author Chaos Leong
 *         01/04/2017
 */
public class PinView extends AppCompatEditText {

    private static final String TAG = "PinView";

    private static final boolean DBG = false;

    private static final int DEFAULT_COUNT = 4;

    private static final InputFilter[] NO_FILTERS = new InputFilter[0];

    private static final int VIEW_TYPE_RECTANGLE = 0;
    private static final int VIEW_TYPE_LINE = 1;

    private int mViewType;

    private int mPinItemCount;

    private float mPinItemSize;
    private float mPinItemWidth;
    private float mPinItemHeight;
    private int mPinItemRadius;
    private int mPinItemSpacing;

    private final Paint mPaint;
    private final TextPaint mTextPaint;
    private final Paint mAnimatorTextPaint;

    private ColorStateList mLineColor;
    private int mCurLineColor = Color.BLACK;
    private int mLineWidth;

    private final Rect mTextRect = new Rect();
    private final RectF mItemBorderRect = new RectF();
    private final RectF mItemLineRect = new RectF();
    private final Path mPath = new Path();
    private final PointF mItemCenterPoint = new PointF();

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

        mViewType = a.getInt(R.styleable.PinView_viewType, VIEW_TYPE_RECTANGLE);
        mPinItemCount = a.getInt(R.styleable.PinView_itemCount, DEFAULT_COUNT);
        mPinItemSize = a.getDimensionPixelSize(R.styleable.PinView_itemSize,
                res.getDimensionPixelOffset(R.dimen.pv_pin_view_item_size));
        mPinItemHeight = mPinItemWidth = mPinItemSize;
        if (a.hasValue(R.styleable.PinView_itemHeight)) {
            mPinItemHeight = a.getDimensionPixelOffset(R.styleable.PinView_itemHeight,
                    res.getDimensionPixelOffset(R.dimen.pv_pin_view_item_size));
        }
        if (a.hasValue(R.styleable.PinView_itemWidth)) {
            mPinItemWidth = a.getDimensionPixelOffset(R.styleable.PinView_itemWidth,
                    res.getDimensionPixelOffset(R.dimen.pv_pin_view_item_size));
        }
        mPinItemSpacing = a.getDimensionPixelOffset(R.styleable.PinView_itemSpacing,
                res.getDimensionPixelOffset(R.dimen.pv_pin_view_item_spacing));
        mPinItemRadius = a.getDimensionPixelOffset(R.styleable.PinView_itemRadius, 0);
        mLineWidth = a.getDimensionPixelOffset(R.styleable.PinView_borderWidth,
                res.getDimensionPixelOffset(R.dimen.pv_pin_view_item_line_width));
        mLineColor = a.getColorStateList(R.styleable.PinView_borderColor);
        if (a.hasValue(R.styleable.PinView_lineWidth)) {
            mLineWidth = a.getDimensionPixelOffset(R.styleable.PinView_lineWidth,
                    res.getDimensionPixelOffset(R.dimen.pv_pin_view_item_line_width));
        }
        if (a.hasValue(R.styleable.PinView_lineColor)) {
            mLineColor = a.getColorStateList(R.styleable.PinView_lineColor);
        }

        a.recycle();

        checkItemRadius();

        setMaxLength(mPinItemCount);
        mPaint.setStrokeWidth(mLineWidth);
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

    private void checkItemRadius() {
        if (mViewType == VIEW_TYPE_LINE) {
            int halfOfLineWidth = mLineWidth / 2;
            if (mPinItemRadius > halfOfLineWidth) {
                throw new RuntimeException("The itemRadius can not be greater than lineWidth when viewType is line");
            }
        }
        int halfOfItemWidth = (int) (mPinItemWidth / 2);
        if (mPinItemRadius > halfOfItemWidth) {
            throw new RuntimeException("The itemRadius can not be greater than itemWidth");
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        float boxHeight = mPinItemHeight;

        if (widthMode == MeasureSpec.EXACTLY) {
            // Parent has told us how big to be. So be it.
            width = widthSize;
        } else {
            float boxesWidth = (mPinItemCount - 1) * mPinItemSpacing + mPinItemCount * mPinItemWidth;
            width = Math.round(boxesWidth + getPaddingRight() + getPaddingLeft());
            if (mPinItemSpacing == 0) {
                width -= (mPinItemCount - 1) * mLineWidth;
            }
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

        if (mLineColor == null || mLineColor.isStateful()) {
            updateColors();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();

        updatePaints();
        drawPinView(canvas);

        canvas.restore();
    }

    private void updatePaints() {
        mPaint.setColor(mCurLineColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mLineWidth);
        mTextPaint.setColor(getCurrentTextColor());
    }

    private void drawPinView(Canvas canvas) {
        if (mViewType == VIEW_TYPE_RECTANGLE && mPinItemSpacing == 0 && mPinItemCount > 1) {
            // because the whole box view draw by updateRoundRectPath() has some bugs we can not fix it,
            // so just draw a whole box view
            drawWholeBoxView(canvas);
        }

        for (int i = 0; i < mPinItemCount; i++) {
            updateItemRectF(i);
            updateCenterPoint();

            if (mViewType == VIEW_TYPE_RECTANGLE) {
                if (mPinItemSpacing != 0) {
                    drawPerPinBox(canvas);
                }
            } else {
                drawPinLine(canvas, i);
            }

            if (DBG) {
                drawAnchorLine(canvas);
            }

            if (getText().length() > i) {
                if (isPasswordInputType(getInputType())) {
                    drawCircle(canvas, i);
                } else {
                    drawText(canvas, i);
                }
            } else if (!TextUtils.isEmpty(getHint()) && getHint().length() == mPinItemCount) {
                drawHint(canvas, i);
            }
        }
    }

    private void drawWholeBoxView(Canvas canvas) {
        float halfLineWidth = (float) mLineWidth / 2;
        float left = getPaddingLeft() + halfLineWidth;
        float right = left + getWidth() - getPaddingLeft() - getPaddingRight() - mLineWidth;
        float top = getPaddingTop() + halfLineWidth;
        float bottom = top + mPinItemHeight - mLineWidth;
        mItemBorderRect.set(left, top, right, bottom);

        updateRoundRectPath(mItemBorderRect, mPinItemRadius, mPinItemRadius, true, true);

        for (int i = 1; i < mPinItemCount; i++) {
            mPath.moveTo(mItemBorderRect.left + (mPinItemWidth - mLineWidth) * i, mItemBorderRect.top - mLineWidth / 2);
            mPath.rLineTo(0, mPinItemHeight - mLineWidth);
        }

        canvas.drawPath(mPath, mPaint);
    }

    private void drawPerPinBox(Canvas canvas) {
        updateRoundRectPath(mItemBorderRect, mPinItemRadius, mPinItemRadius, true, true);
        canvas.drawPath(mPath, mPaint);
    }

    private void drawPinLine(Canvas canvas, int i) {
        boolean l, r;
        l = r = true;
        if (mPinItemSpacing == 0 && mPinItemCount > 1) {
            if (i == 0) {
                // draw only left round
                r = false;
            } else if (i == mPinItemCount - 1) {
                // draw only right round
                l = false;
            } else {
                // draw rect
                l = r = false;
            }
        }
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(((float) mLineWidth) / 10);
        float halfLineWidth = (float) mLineWidth / 2;
        mItemLineRect.set(mItemBorderRect.left, mItemBorderRect.bottom - halfLineWidth, mItemBorderRect.right, mItemBorderRect.bottom + halfLineWidth);

        updateRoundRectPath(mItemLineRect, mPinItemRadius, mPinItemRadius, l, r);
        canvas.drawPath(mPath, mPaint);
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

    private void updateItemRectF(int i) {
        float halfLineWidth = (float) mLineWidth / 2;
        float left = getPaddingLeft() + i * (mPinItemSpacing + mPinItemWidth) + halfLineWidth;
        if (mPinItemSpacing == 0 && i > 0) {
            left = left - (mLineWidth) * i;
        }
        float right = left + mPinItemWidth - mLineWidth;
        float top = getPaddingTop() + halfLineWidth;
        float bottom = top + mPinItemHeight - mLineWidth;

        mItemBorderRect.set(left, top, right, bottom);
    }

    private void drawText(Canvas canvas, int i) {
        Paint paint = getPaintByIndex(i);
        // 1, Rect(4, -39, 20, 0)
        // 您, Rect(2, -47, 51, 3)
        // *, Rect(0, -39, 23, -16)
        // =, Rect(4, -26, 26, -10)
        // -, Rect(1, -19, 14, -14)
        // +, Rect(2, -32, 29, -3)
        drawTextAtBox(canvas, paint, getText(), i);
    }

    private void drawHint(Canvas canvas, int i) {
        Paint paint = getPaintByIndex(i);
        paint.setColor(getCurrentHintTextColor());
        drawTextAtBox(canvas, paint, getHint(), i);
    }

    private void drawTextAtBox(Canvas canvas, Paint paint, CharSequence text, int charAt) {
        paint.getTextBounds(text.toString(), charAt, charAt + 1, mTextRect);
        float cx = mItemCenterPoint.x;
        float cy = mItemCenterPoint.y;
        float x = cx - Math.abs(mTextRect.width()) / 2 - mTextRect.left;
        float y = cy + Math.abs(mTextRect.height()) / 2 - mTextRect.bottom;// always center vertical
        canvas.drawText(text, charAt, charAt + 1, x, y, paint);
    }

    private void drawCircle(Canvas canvas, int i) {
        Paint paint = getPaintByIndex(i);
        float cx = mItemCenterPoint.x;
        float cy = mItemCenterPoint.y;
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
        float cx = mItemCenterPoint.x;
        float cy = mItemCenterPoint.y;
        mPaint.setStrokeWidth(1);
        cx -= mPaint.getStrokeWidth() / 2;
        cy -= mPaint.getStrokeWidth() / 2;

        mPath.reset();
        mPath.moveTo(cx, mItemBorderRect.top);
        mPath.lineTo(cx, mItemBorderRect.top + Math.abs(mItemBorderRect.height()));
        canvas.drawPath(mPath, mPaint);

        mPath.reset();
        mPath.moveTo(mItemBorderRect.left, cy);
        mPath.lineTo(mItemBorderRect.left + Math.abs(mItemBorderRect.width()), cy);
        canvas.drawPath(mPath, mPaint);

        mPath.reset();

        mPaint.setStrokeWidth(mLineWidth);
    }

    private void updateColors() {
        boolean inval = false;

        int color;
        if (mLineColor != null) {
            color = mLineColor.getColorForState(getDrawableState(), 0);
        } else {
            color = getCurrentTextColor();
        }

        if (color != mCurLineColor) {
            mCurLineColor = color;
            inval = true;
        }

        if (inval) {
            invalidate();
        }
    }

    private void updateCenterPoint() {
        float cx = mItemBorderRect.left + Math.abs(mItemBorderRect.width()) / 2;
        float cy = mItemBorderRect.top + Math.abs(mItemBorderRect.height()) / 2;
        mItemCenterPoint.set(cx, cy);
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
     * @deprecated Use {@link #setLineColor(int)} instead.
     */
    @Deprecated
    public void setBorderColor(@ColorInt int color) {
        setLineColor(color);
    }

    /**
     * Sets the border color.
     *
     * @attr ref R.styleable#PinView_borderColor
     * @see #setBorderColor(int)
     * @see #getBorderColors()
     * @deprecated Use {@link #setLineColor(ColorStateList)} instead.
     */
    @Deprecated
    public void setBorderColor(ColorStateList colors) {
        setLineColor(colors);
    }

    /**
     * Gets the border colors for the different states (normal, selected, focused) of the PinView.
     *
     * @attr ref R.styleable#PinView_borderColor
     * @see #setBorderColor(ColorStateList)
     * @see #setBorderColor(int)
     * @deprecated Use {@link #getLineColors()} instead.
     */
    @Deprecated
    public ColorStateList getBorderColors() {
        return getLineColors();
    }

    /**
     * <p>Return the current color selected for normal border.</p>
     *
     * @return Returns the current border color.
     * @deprecated Use {@link #getCurrentLineColor()} instead.
     */
    @ColorInt
    @Deprecated
    public int getCurrentBorderColor() {
        return getCurrentLineColor();
    }

    /**
     * Sets the line color for all the states (normal, selected,
     * focused) to be this color.
     *
     * @param color A color value in the form 0xAARRGGBB.
     *              Do not pass a resource ID. To get a color value from a resource ID, call
     *              {@link android.support.v4.content.ContextCompat#getColor(Context, int) getColor}.
     * @attr ref R.styleable#PinView_lineColor
     * @see #setLineColor(ColorStateList)
     * @see #getLineColors()
     */
    public void setLineColor(@ColorInt int color) {
        mLineColor = ColorStateList.valueOf(color);
        updateColors();
    }

    /**
     * Sets the line color.
     *
     * @attr ref R.styleable#PinView_lineColor
     * @see #setLineColor(int)
     * @see #getLineColors()
     */
    public void setLineColor(ColorStateList colors) {
        if (colors == null) {
            throw new NullPointerException();
        }

        mLineColor = colors;
        updateColors();
    }

    /**
     * Gets the line colors for the different states (normal, selected, focused) of the PinView.
     *
     * @attr ref R.styleable#PinView_lineColor
     * @see #setLineColor(ColorStateList)
     * @see #setLineColor(int)
     */
    public ColorStateList getLineColors() {
        return mLineColor;
    }

    /**
     * <p>Return the current color selected for normal line.</p>
     *
     * @return Returns the current item's line color.
     */
    @ColorInt
    public int getCurrentLineColor() {
        return mCurLineColor;
    }

    /**
     * Sets the border width.
     *
     * @attr ref R.styleable#PinView_borderWidth
     * @see #getBorderWidth()
     * @deprecated Use {@link #setLineWidth(int)} instead.
     */
    @Deprecated
    public void setBorderWidth(@Px int borderWidth) {
        setLineWidth(borderWidth);
    }

    /**
     * @return Returns the width of the box's border.
     * @see #setBorderWidth(int)
     * @deprecated Use {@link #getLineWidth()} instead.
     */
    @Px
    @Deprecated
    public int getBorderWidth() {
        return getLineWidth();
    }

    /**
     * Sets the line width.
     *
     * @attr ref R.styleable#PinView_lineWidth
     * @see #getLineWidth()
     */
    public void setLineWidth(@Px int borderWidth) {
        checkItemRadius();
        mLineWidth = borderWidth;
        requestLayout();
    }

    /**
     * @return Returns the width of the item's line.
     * @see #setLineWidth(int)
     */
    @Px
    public int getLineWidth() {
        return mLineWidth;
    }

    /**
     * Sets the count of items.
     *
     * @attr ref R.styleable#PinView_itemCount
     * @see #getItemCount()
     */
    public void setItemCount(int count) {
        mPinItemCount = count;
        setMaxLength(count);
        requestLayout();
    }

    /**
     * @return Returns the count of items.
     * @see #setItemCount(int)
     */
    public int getItemCount() {
        return mPinItemCount;
    }

    /**
     * Sets the radius of square.
     *
     * @attr ref R.styleable#PinView_itemRadius
     * @see #getItemRadius()
     */
    public void setItemRadius(@Px int itemRadius) {
        checkItemRadius();
        mPinItemRadius = itemRadius;
        requestLayout();
    }

    /**
     * @return Returns the radius of square.
     * @see #setItemRadius(int)
     */
    @Px
    public int getItemRadius() {
        return mPinItemRadius;
    }

    /**
     * Specifies extra space between two items.
     *
     * @attr ref R.styleable#PinView_itemSpacing
     * @see #getItemSpacing()
     */
    public void setItemSpacing(@Px int itemSpacing) {
        mPinItemSpacing = itemSpacing;
        requestLayout();
    }

    /**
     * @return Returns the spacing between two items.
     * @see #setItemSpacing(int)
     */
    @Px
    public int getItemSpacing() {
        return mPinItemSpacing;
    }

    /**
     * Sets the height and width of item.
     *
     * @attr ref R.styleable#PinView_itemSize
     * @see #getItemSize()
     * @deprecated Use {@link #setItemHeight(float)} or {@link #setItemWidth(float)} instead.
     */
    @Deprecated
    public void setItemSize(float itemSize) {
        mPinItemWidth = mPinItemHeight = mPinItemSize = itemSize;
        requestLayout();
    }

    /**
     * @return Returns the size of item.
     * @see #setItemSize(float)
     * @deprecated Use {@link #getItemHeight()} or {@link #getItemWidth()} instead.
     */
    @Deprecated
    public float getItemSize() {
        return mPinItemSize;
    }

    /**
     * Sets the height of item.
     *
     * @attr ref R.styleable#PinView_itemHeight
     * @see #getItemHeight()
     */
    public void setItemHeight(float itemHeight) {
        mPinItemHeight = itemHeight;
        requestLayout();
    }

    /**
     * @return Returns the height of item.
     * @see #setItemHeight(float)
     */
    public float getItemHeight() {
        return mPinItemHeight;
    }

    /**
     * Sets the width of item.
     *
     * @attr ref R.styleable#PinView_itemWidth
     * @see #getItemWidth()
     */
    public void setItemWidth(float itemWidth) {
        checkItemRadius();
        mPinItemWidth = itemWidth;
        requestLayout();
    }

    /**
     * @return Returns the width of item.
     * @see #setItemWidth(float)
     */
    public float getItemWidth() {
        return mPinItemWidth;
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