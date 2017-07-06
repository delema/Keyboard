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
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.PopupWindow;

import java.util.HashMap;
import java.util.Map;

import es.lema.orthos.inputmethod.common.StringUtils;

public class LatinKeyboardView extends KeyboardView {

    static final int KEYCODE_OPTIONS = -100;
    static final int KEYCODE_LANGUAGE_SWITCH = -101;

    private Map<Key,View> alternativesKeyboardCache;
    private PopupWindow popupKeyboard;
    private int pointerId;
    private AlternativesKeyboardView alternativesKeyboardView;

    public LatinKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        alternativesKeyboardCache = new HashMap<Key,View>();
        popupKeyboard = new PopupWindow(context);
        popupKeyboard.setBackgroundDrawable(null);
        alternativesKeyboardView = null;
    }

    @Override
    protected boolean onLongPress(Key key) {

        if (key.codes[0] == Keyboard.KEYCODE_CANCEL) {
            getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null);
            return true;
        } else if (!StringUtils.isEmpty(key.popupCharacters)) {
            View keyboardContainer  = alternativesKeyboardCache.get(key);
            if (keyboardContainer == null) {
                AlternativesKeyboard alternativesKeysKeyboard =
                new AlternativesKeyboard(
                getContext(),
                key.popupResId,
                key.popupCharacters,
                -1,
                getPaddingLeft() + getPaddingRight());
                LayoutInflater inflater = (LayoutInflater)
                getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                keyboardContainer = inflater.inflate(R.layout.alternatives, null);
                alternativesKeyboardView = (AlternativesKeyboardView)
                keyboardContainer.findViewById(R.id.alternatives_keyboard_view);
                alternativesKeyboardView.setOnKeyboardActionListener(new OnKeyboardActionListener() {
                    public void onKey(int keyCode, int[] keyCodes) {
                        getOnKeyboardActionListener().onKey(keyCode, keyCodes);
                        dismissPopupKeyboard();
                    }
                    public void onText(CharSequence text) {}
                    public void swipeLeft() {}
                    public void swipeRight() {}
                    public void swipeUp() { }
                    public void swipeDown() { }
                    public void onPress(int keyCode) {}
                    public void onRelease(int keyCode) {}
                });
                alternativesKeyboardView.setKeyboard(alternativesKeysKeyboard);
                keyboardContainer.measure(
                MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.AT_MOST));
                alternativesKeyboardCache.put(key, keyboardContainer);
            }
            else {
                alternativesKeyboardView = (AlternativesKeyboardView)
                keyboardContainer.findViewById(R.id.alternatives_keyboard_view);
            }
            int[] coordinates = new int[2];

            getLocationInWindow(coordinates);
            int x = key.x
                  + getPaddingLeft()
                  + key.width
                  - keyboardContainer.getMeasuredWidth()
                  + keyboardContainer.getPaddingRight()
                  + coordinates[0];
            int y = key.y
                  + getPaddingTop()
                  - keyboardContainer.getMeasuredHeight()
                  + keyboardContainer.getPaddingBottom()
                  + coordinates[1];
            x = x < 0 ? 0 : x;
            alternativesKeyboardView.setShifted(isShifted());
            popupKeyboard.setContentView(keyboardContainer);
            popupKeyboard.setWidth(keyboardContainer.getMeasuredWidth());
            popupKeyboard.setHeight(keyboardContainer.getMeasuredHeight());
            popupKeyboard.showAtLocation(this, Gravity.NO_GRAVITY, x, y);
            alternativesKeyboardView.setPositionX(x);
            alternativesKeyboardView.setPositionY(y);
            alternativesKeyboardView.invalidateAllKeys();
            Key firstKey =
            alternativesKeyboardView.getKeyboard().getKeys().get(0);
            alternativesKeyboardView.onDownEvent(
            firstKey.x,
            firstKey.y,
            pointerId,
            SystemClock.uptimeMillis());
            return true;
        }
        return super.onLongPress(key);
    }

    void setSubtypeOnSpaceKey(final InputMethodSubtype subtype) {
        final LatinKeyboard keyboard = (LatinKeyboard)getKeyboard();
        keyboard.setSpaceIcon(getResources().getDrawable(subtype.getIconResId()));
        invalidateAllKeys();
    }

    public void setKeyboard(Keyboard keyboard) {
        super.setKeyboard(keyboard);
        alternativesKeyboardCache.clear();
    }

    private void dismissPopupKeyboard() {
        if (popupKeyboard.isShowing()) {
            popupKeyboard.dismiss();
            invalidateAllKeys();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {

        final int index = me.getActionIndex();
        pointerId = me.getPointerId(index);

        if (alternativesKeyboardView != null) {

            final int action = me.getActionMasked();
            final long eventTime = me.getEventTime();

            int x = (int) me.getX(index)
                  - alternativesKeyboardView.getPositionX()
                  - popupKeyboard.getContentView().getPaddingLeft();
            if (x < 0) x = 0;
            if (x > popupKeyboard.getWidth() - popupKeyboard.getContentView().getPaddingRight()) {
                x = popupKeyboard.getWidth() - popupKeyboard.getContentView().getPaddingRight();
            }
            int y = (int) me.getY(index)
                  - alternativesKeyboardView.getPositionY()
                  - popupKeyboard.getContentView().getPaddingTop();
            if (y < 0) y = 0;
            if (y > popupKeyboard.getHeight() - popupKeyboard.getContentView().getPaddingBottom()) {
                y = popupKeyboard.getHeight() - popupKeyboard.getContentView().getPaddingBottom();
            }
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    alternativesKeyboardView.onDownEvent(x, y, pointerId, eventTime);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    alternativesKeyboardView.onUpEvent(x, y, pointerId, eventTime);
                    alternativesKeyboardView = null;
                    break;
                case MotionEvent.ACTION_MOVE:
                    alternativesKeyboardView.onMoveEvent(x, y, pointerId, eventTime);
                    break;
            }
            return true;
        }

        return super.onTouchEvent(me);
    }
}
