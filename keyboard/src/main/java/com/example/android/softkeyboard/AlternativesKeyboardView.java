/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * A view that renders a virtual {@link AlternativesKeyboard}. It handles rendering of keys and
 * detecting key presses and touch movements.
 */
public class AlternativesKeyboardView extends KeyboardView {

    private int pointerId;
    private int indexKey;
    private AlternativesDetector keyDetector;
    private int positionX;
    private int positionY;

    public AlternativesKeyboardView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        float value = getResources().getDimension(R.dimen.config_alternatives_keyboard_slide_allowance);
        keyDetector = new AlternativesDetector(value);
        indexKey = -1;
    }

    public void setKeyboard(Keyboard keyboard) {
        super.setKeyboard(keyboard);
        keyDetector.setKeyboard(keyboard, 0, 0);
        indexKey = -1;
    }

    public boolean onTouchEvent(final MotionEvent me) {
        final int action = me.getActionMasked();
        final long eventTime = me.getEventTime();
        final int index = me.getActionIndex();
        final int x = (int)me.getX(index);
        final int y = (int)me.getY(index);
        final int pointerId = me.getPointerId(index);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                onDownEvent(x, y, pointerId, eventTime);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                onUpEvent(x, y, pointerId, eventTime);
                break;
            case MotionEvent.ACTION_MOVE:
                onMoveEvent(x, y, pointerId, eventTime);
                break;
        }
        return true;
    }

    public void onDownEvent(final int x, final int y, final int pointerId, final long eventTime) {
        this.pointerId = pointerId;
        indexKey = detectKey(x, y);
    }

    public void onMoveEvent(final int x, final int y, final int pointerId, final long eventTime) {
        if (this.pointerId != pointerId) {
            return;
        }
        indexKey = detectKey(x, y);
    }

    public void onUpEvent(final int x, final int y, final int pointerId, final long eventTime) {
        if (this.pointerId != pointerId) {
            return;
        }
        if (indexKey != -1) {
            updateReleaseKeyGraphics(indexKey);
            onKeyInput(indexKey, x, y);
            indexKey = -1;
        }
    }

    private int detectKey(int x, int y) {
        final int oldKey = indexKey;
        final int newKey = keyDetector.detectHitKey(x, y);
        if (newKey == oldKey) {
            return newKey;
        }
        // A new key is detected.
        if (oldKey != -1) {
            updateReleaseKeyGraphics(oldKey);
        }
        if (newKey != -1) {
            updatePressKeyGraphics(newKey);
        }
        return newKey;
    }

    private void updateReleaseKeyGraphics(final int index) {
        Key key = getKeyboard().getKeys().get(index);
        key.onReleased(true);
        invalidateKey(index);
    }

    private void updatePressKeyGraphics(final int index) {
        Key key = getKeyboard().getKeys().get(index);
        key.onPressed();
        invalidateKey(index);
    }

    /**
     * Performs the specific action for this panel when the user presses
     * a key on the panel.
     */
    protected void onKeyInput(final int index, final int x, final int y) {
        Key key = getKeyboard().getKeys().get(index);
        getOnKeyboardActionListener().onKey(key.codes[0],key.codes);
    }

    public int getPositionX() {
        return positionX;
    }

    public void setPositionX(int positionX) {
        this.positionX = positionX;
    }

    public int getPositionY() {
        return positionY;
    }

    public void setPositionY(int positionY) {
        this.positionY = positionY;
    }
}
