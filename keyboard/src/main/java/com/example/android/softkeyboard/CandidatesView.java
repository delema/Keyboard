/*
 * Copyright (C) 2008-2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.softkeyboard;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class CandidatesView extends View {

	private static final int OUT_OF_BOUNDS = -1;
	private static final int MAX_SUGGESTIONS = 32;
	private static final int SCROLL_PIXELS = 20;
	private static final List<String> EMPTY_LIST = new ArrayList<String>();
	private SoftKeyboard mService;
	private List<String> mSuggestions;
	private int mSelected;
	private int mTouchX = OUT_OF_BOUNDS;
	private Drawable mListSelectort;
	private boolean mTypedWordValid;
	private Rect mBgPadding;
	private int[] mWordWidth = new int[MAX_SUGGESTIONS];
	private int mColorText;
	private int mColorRecommended;
	private int mColorLine;
	private int mHorizontalPadding;
	private int mVerticalPadding;
	private Paint mPaint;
	private boolean mScrolled;
	private int mScrollX;

	private int mWidth;

	private GestureDetector mGestureDetector;

	public CandidatesView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		initialize(context);
	}
	/**
	 * Construct a CandidatesView for showing suggested words for completion.
	 * 
	 * @param context
	 */
	public CandidatesView(Context context) {
		super(context);
		initialize(context);
	}

	protected void initialize(final Context context) {

		Resources resources = context.getResources();
		//mListSelectort = resources.getDrawable(android.R.drawable.list_selector_background);
        mListSelectort = getBackground();

        setBackgroundColor(resources.getColor(R.color.candidate_background));

		mColorText = resources.getColor(R.color.candidate_text);
		mColorRecommended = resources.getColor(R.color.candidate_recommended);
		mColorLine = resources.getColor(R.color.candidate_line);
		mVerticalPadding = resources.getDimensionPixelSize(R.dimen.candidate_vertical_padding);
		mHorizontalPadding = resources.getDimensionPixelSize(R.dimen.candidate_horizontal_padding);

		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setColor(mColorText);
		mPaint.setAntiAlias(true);
		mPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.candidate_font_height));
		mPaint.setStrokeWidth(0);
		mPaint.setFakeBoldText(true);

		mGestureDetector = new GestureDetector(
		context, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				mScrolled = true;
				int sx = getScrollX();
				sx += distanceX;
				if (sx < 0) {
					sx = 0;
				}
				if (sx + getWidth() > mWidth) {
					sx -= distanceX;
				}
				mScrollX = sx;
				scrollTo(sx, getScrollY());
				invalidate();
				return true;
			}
		});
		setHorizontalFadingEdgeEnabled(true);
		setWillNotDraw(false);
		setHorizontalScrollBarEnabled(false);
		setVerticalScrollBarEnabled(false);
	}
	/**
	 * A connection back to the service to communicate with the text field
	 * 
	 * @param listener
	 */
	public void setService(SoftKeyboard listener) {
		mService = listener;
	}

	@Override
	public int computeHorizontalScrollRange() {
		return mWidth;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		Rect padding = new Rect();
		mListSelectort.getPadding(padding);

		final int desiredWidth = padding.left + padding.right + getSuggestedMinimumWidth();
		int measuredWidth = resolveSize(desiredWidth, widthMeasureSpec);

		final int w = MeasureSpec.getSize(widthMeasureSpec);
		final int width = MeasureSpec.getSize(measuredWidth);

		final int desiredHeight =
		((int) mPaint.getTextSize()) + mVerticalPadding * 2 + padding.top + padding.bottom;
		int measuredHeight = resolveSize(desiredHeight, heightMeasureSpec);

		final int h = MeasureSpec.getSize(heightMeasureSpec);
		final int height = MeasureSpec.getSize(measuredHeight);

		// Maximum possible width and desired height
		setMeasuredDimension(measuredWidth, measuredHeight);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mSuggestions == null) {
			return;
		}
		if (mBgPadding == null) {
			mBgPadding = new Rect(0, 0, 0, 0);
			if (getBackground() != null) {
				getBackground().getPadding(mBgPadding);
			}
		}
		int x = 0;
		final int count = mSuggestions.size();
		final int height = getHeight();
		final Rect bgPadding = mBgPadding;
		final Paint paint = mPaint;
		final int touchX = mTouchX;
		final int scrollX = getScrollX();
		final boolean scrolled = mScrolled;
		final boolean typedWordValid = mTypedWordValid;
		final int y = (height - (int) mPaint.getTextSize()) / 2 - (int) mPaint.ascent();
        // Centra la lista de sugerencias
        final int width = getWidth();
        if (mWidth < width) {
           x = (width - mWidth) / 2;
        }

		for (int i = 0; i < count; i++) {
			String suggestion = mSuggestions.get(i);

			final int wordWidth = mWordWidth[i];

			paint.setColor(mColorText);
			if (touchX + scrollX >= x && touchX + scrollX < x + wordWidth && !scrolled) {
				mListSelectort.setBounds(x, bgPadding.top, x + wordWidth, height);
				mListSelectort.setState(new int[] {android.R.attr.state_pressed });
				mListSelectort.draw(canvas);
				mSelected = i;
			}
			canvas.drawText(suggestion, x + mHorizontalPadding, y, paint);
			if (i < count -1) {
				paint.setColor(mColorLine);
				canvas.drawLine(x + wordWidth, getPaddingTop(), x + wordWidth, height - getPaddingBottom(), paint);
			}
			x += wordWidth;
		}

		if (mScrollX != getScrollX()) {
			scrollToTarget();
		}
	}

	protected void inflate() {
		mWidth = 0;
		if (mSuggestions == null)
			return;

		int x = 0;
		final int count = mSuggestions.size();
		final Paint paint = mPaint;

		for (int i = 0; i < count; i++) {
			String suggestion = mSuggestions.get(i);
			float textWidth = paint.measureText(suggestion);
			final int wordWidth = (int) textWidth + mHorizontalPadding * 2;

			mWordWidth[i] = wordWidth;

			x += wordWidth;
		}
		mWidth = x;
	}

	private void scrollToTarget() {
		int sx = getScrollX();
		if (mScrollX > sx) {
			sx += SCROLL_PIXELS;
			if (sx >= mScrollX) {
				sx = mScrollX;
				requestLayout();
			}
		}
		else {
			sx -= SCROLL_PIXELS;
			if (sx <= mScrollX) {
				sx = mScrollX;
				requestLayout();
			}
		}
		scrollTo(sx, getScrollY());
		invalidate();
	}

	public void setSuggestions(
	List<String> suggestions, boolean completions, boolean typedWordValid) {
		clear();
		if (suggestions != null) {
			mSuggestions = new ArrayList<String>(suggestions);
		}
		mTypedWordValid = typedWordValid;
		scrollTo(0, 0);
		mScrollX = 0;
		// Compute the total width
		inflate();
		invalidate();
		requestLayout();
	}

	public void clear() {
		mSuggestions = EMPTY_LIST;
		mTouchX = OUT_OF_BOUNDS;
		mSelected = OUT_OF_BOUNDS;
		invalidate();
	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {

		if (mGestureDetector.onTouchEvent(me)) {
			return true;
		}

		int action = me.getAction();
		int x = (int) me.getX();
		int y = (int) me.getY();
		mTouchX = x;

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mScrolled = false;
			invalidate();
			break;
		case MotionEvent.ACTION_MOVE:
			if (y <= 0) {
				if (mSelected != OUT_OF_BOUNDS) {
					mService.pickSuggestion(mSelected);
					mSelected = OUT_OF_BOUNDS;
				}
			}
			invalidate();
			break;
		case MotionEvent.ACTION_UP:
			if (!mScrolled) {
				if (mSelected != OUT_OF_BOUNDS) {
					mService.pickSuggestion(mSelected);
				}
			}
			mSelected = OUT_OF_BOUNDS;
			removeHighlight();
			requestLayout();
			break;
		}
		return true;
	}

	private void removeHighlight() {
		mTouchX = OUT_OF_BOUNDS;
		invalidate();
	}
}
