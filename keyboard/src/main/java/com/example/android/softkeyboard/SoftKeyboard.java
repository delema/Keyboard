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

import android.app.Dialog;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.IBinder;
import android.text.InputType;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.ArrayList;
import java.util.List;

import es.lema.orthos.OrthosServiceManager;

/**
 * Example of writing an keyboard method for a soft keyboard. This code is focused
 * on simplicity over completeness, so it should in no way be considered to be a
 * complete soft keyboard implementation. Its purpose is to provide a basic
 * example for how you would get started writing an keyboard method, to be fleshed
 * out as appropriate.
 */
public class SoftKeyboard
extends InputMethodService
implements KeyboardView.OnKeyboardActionListener {

	/**
	 * This boolean indicates the optional example code for performing
	 * processing of hard keys in addition to regular text generation from
	 * on-screen interaction. It would be used for keyboard methods that perform
	 * language translations (such as converting text entered on a QWERTY
	 * keyboard to Chinese), but may not be used for keyboard methods that are
	 * primarily intended to be used for on-screen text entry.
	 */
	private static final String TAG = SoftKeyboard.class.getSimpleName();
	private final UIHandler mUIHandler = new UIHandler(this);
	private InputMethodManager mInputMethodManager;
	private LatinKeyboardView mInputView;
	private CandidatesView mCandidatesView;
	private CompletionInfo[] mCompletions;
	//private StringBuilder mComposing = new StringBuilder();
	private boolean mPredictionOn;
	private boolean mCompletionOn;
	private int mLastDisplayWidth;
	private boolean mCapsLock;
	private long mLastShiftTime;
	private long mMetaState;
	private LatinKeyboard mSymbolsKeyboard;
	private LatinKeyboard mSymbolsShiftedKeyboard;
	private LatinKeyboard mQwertyKeyboard;
	private LatinKeyboard mCurKeyboard;
	//private String mWordSeparators;
	private InputConnectionManager inputConnectionManager;


	/**
	 * Main initialization of the keyboard method component. Be sure to call to
	 * super class.
	 */
	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate(): SoftKeyboard");
		super.onCreate();
		mUIHandler.onCreate();
		mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		//mWordSeparators = getResources().getString(R.string.word_separators);
		OrthosServiceManager.create(this);
		inputConnectionManager = new InputConnectionManager(this);
	}

	/**
	 * This is the point where you can do all of your UI initialization. It is
	 * called after creation and any configuration change.
	 */
	@Override
	public void onInitializeInterface() {
		if (mQwertyKeyboard != null) {
			// Configuration changes can happen after the keyboard gets
			// recreated,
			// so we need to be able to re-build the keyboards if the available
			// space has changed.
			int displayWidth = getMaxWidth();
			if (displayWidth == mLastDisplayWidth)
				return;
			mLastDisplayWidth = displayWidth;
		}
		mQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty);
		mSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
		mSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);
	}
	/**
	 * Called by the framework when your view for creating keyboard needs to be
	 * generated. This will be called the first time your keyboard method is
	 * displayed, and every time it needs to be re-created such as due to a
	 * configuration change.
	 */
	@Override
	public View onCreateInputView() {
		mInputView = (LatinKeyboardView) getLayoutInflater().inflate(R.layout.keyboard, null);
		mInputView.setOnKeyboardActionListener(this);
		setLatinKeyboard(mQwertyKeyboard);
		return mInputView;
	}

    @Override
    public void setInputView(View view) {
        super.setInputView(view);
    }
	/**
	 * Called by the framework when your view for showing candidates needs to be
	 * generated, like {@link #onCreateInputView}.
	 */
	@Override
	public View onCreateCandidatesView() {
		mCandidatesView = (CandidatesView) getLayoutInflater().inflate(R.layout.candidates, null);
		mCandidatesView.setService(this);
		return mCandidatesView;
	}

	@Override
	public void setCandidatesView(View view) {
		super.setCandidatesView(view);
	}
    /**
     * Determine the basic space needed to resize the application behind.
     */
    @Override
    public void onComputeInsets(Insets outInsets) {

        super.onComputeInsets(outInsets);
        if (!isFullscreenMode()) {
            outInsets.contentTopInsets = outInsets.visibleTopInsets;
        }
    }

	private void setLatinKeyboard(LatinKeyboard keyboard) {
		final boolean shouldSupportLanguageSwitchKey =
		mInputMethodManager.shouldOfferSwitchingToNextInputMethod(getToken());
		keyboard.setLanguageSwitchKeyVisibility(shouldSupportLanguageSwitchKey);
		mInputView.setKeyboard(keyboard);
	}

	private void updateCandidates() {
		if (mPredictionOn) {
			inputConnectionManager.updateCandidates();
		}
	}
	/**
	 * This is the main point where we do our initialization of the keyboard method
	 * to begin operating on an application. At this point we have been bound to
	 * the client, and are now receiving all of the detailed information about
	 * the target of our edits.
	 */
	@Override
	public void onStartInput(EditorInfo attribute, boolean restarting) {
		super.onStartInput(attribute, restarting);

		// Reset our state. We want to do this even if restarting, because
		// the underlying state of the text editor could have changed in any
		// way.

		if (!restarting) {
			// Clear shift states.
			mMetaState = 0;
		}

		mPredictionOn = false;
		mCompletionOn = false;
		mCompletions = null;

		// We are now going to initialize our state based on the type of
		// text being edited.
		switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
		case InputType.TYPE_CLASS_NUMBER:
		case InputType.TYPE_CLASS_DATETIME:
			// Numbers and dates default to the symbols keyboard, with
			// no extra features.
			mCurKeyboard = mSymbolsKeyboard;
			break;

		case InputType.TYPE_CLASS_PHONE:
			// Phones will also default to the symbols keyboard, though
			// often you will want to have a dedicated phone keyboard.
			mCurKeyboard = mSymbolsKeyboard;
			break;

		case InputType.TYPE_CLASS_TEXT:
			// This is general text editing. We will default to the
			// normal alphabetic keyboard, and assume that we should
			// be doing predictive text (showing candidates as the
			// user types).
			mCurKeyboard = mQwertyKeyboard;
			mPredictionOn = true;

			// We now look for a few special variations of text that will
			// modify our behavior.
			int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
			if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
					|| variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
				// Do not display predictions / what the user is typing
				// when they are entering a password.
				mPredictionOn = false;
			}

			if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
					|| variation == InputType.TYPE_TEXT_VARIATION_URI
					|| variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
				// Our predictions are not useful for e-mail addresses
				// or URIs.
				mPredictionOn = false;
			}

			if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
				// If this is an auto-complete text view, then our predictions
				// will not be shown and instead we will allow the editor
				// to supply their own. We only show the editor's
				// candidates when in fullscreen mode, otherwise relying
				// own it displaying its own UI.
				mPredictionOn = false;
				mCompletionOn = isFullscreenMode();
			}

			// We also want to look at the current state of the editor
			// to decide whether our alphabetic keyboard should start out
			// shifted.
			updateShiftKeyState(attribute);
			break;

		default:
			// For all unknown keyboard types, default to the alphabetic
			// keyboard with no special features.
			mCurKeyboard = mQwertyKeyboard;
			updateShiftKeyState(attribute);
		}

		// Update the label on the enter key, depending on what the application
		// says it will do.
		mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);
		// Inicializa la entrada de datos
		inputConnectionManager.onStartInput();
		updateCandidates();
	}

	/**
	 * This is called when the user is done editing a field. We can use this to
	 * reset our state.
	 */
	@Override
	public void onFinishInput() {
		super.onFinishInput();

		// We only hide the candidates window when finishing keyboard on
		// a particular editor, to avoid popping the underlying application
		// up and down if the user is entering text into the bottom of
		// its window.
		setCandidatesViewShown(false);

		mCurKeyboard = mQwertyKeyboard;
		if (mInputView != null) {
			mInputView.closing();
		}
	}

	@Override
	public void onStartInputView(EditorInfo attribute, boolean restarting) {
		super.onStartInputView(attribute, restarting);
		// Apply the selected keyboard to the keyboard view.
		setLatinKeyboard(mCurKeyboard);
		mInputView.closing();
		final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
		mInputView.setSubtypeOnSpaceKey(subtype);
	}

	@Override
	public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
		mInputView.setSubtypeOnSpaceKey(subtype);
	}

	/**
	 * Deal with the editor reporting movement of its cursor.
	 */
	@Override
	public void onUpdateSelection(
	int oldStart,
	int oldEnd,
	int newStart,
	int newEnd,
	int candidatesStart,
	int candidatesEnd) {
		super.onUpdateSelection(oldStart, oldEnd, newStart, newEnd, candidatesStart, candidatesEnd);
		if (newStart != candidatesEnd || newEnd != candidatesEnd) {
			inputConnectionManager.finishComposingText();
		}
		updateCandidates();
	}

	/**
	 * This tells us about completions that the editor has determined based on
	 * the current text in it. We want to use this in fullscreen mode to show
	 * the completions ourself, since the editor can not be seen in that
	 * situation.
	 */
	@Override
	public void onDisplayCompletions(CompletionInfo[] completions) {
		if (mCompletionOn) {
			if (completions == null) {
				setSuggestions(null, false, false);
				return;
			}
			setSuggestions(completions, true, true);
		}
	}

	/**
	 * This translates incoming hard key events in to edit operations on an
	 * InputConnection.
	 */
	private boolean translateKeyDown(int keyCode, KeyEvent event) {
		mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState, keyCode, event);
		int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
		mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
		if (c == 0) {
			return false;
		}
		return true;
	}

	/**
	 * Use this to monitor key events being delivered to the application. We get
	 * first crack at them, and can either resume them or let them continue to
	 * the app.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Use this to monitor key events being delivered to the application. We get
	 * first crack at them, and can either resume them or let them continue to
	 * the app.
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return super.onKeyUp(keyCode, event);
	}

	/**
	 * Helper to update the shift state of our keyboard based on the initial
	 * editor state.
	 */
	private void updateShiftKeyState(EditorInfo attr) {
		if (attr != null && mInputView != null && mQwertyKeyboard == mInputView.getKeyboard()) {
			int caps = 0;
			EditorInfo ei = getCurrentInputEditorInfo();
			if (ei != null && ei.inputType != InputType.TYPE_NULL) {
				caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
			}
			mInputView.setShifted(mCapsLock || caps != 0);
		}
	}

	// Implementation of KeyboardViewListener

	public void onKey(int keyCode, int[] keyCodes) {
		if (keyCode == Keyboard.KEYCODE_SHIFT) {
			handleShift();
		} else if (keyCode == Keyboard.KEYCODE_CANCEL) {
			handleClose();
		} else if (keyCode == LatinKeyboardView.KEYCODE_LANGUAGE_SWITCH) {
			handleLanguageSwitch();
		} else if (keyCode == LatinKeyboardView.KEYCODE_OPTIONS) {
			// Show a menu or somethin'
		} else if (keyCode == Keyboard.KEYCODE_MODE_CHANGE && mInputView != null) {
			Keyboard current = mInputView.getKeyboard();
			if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
				setLatinKeyboard(mQwertyKeyboard);
			} else {
				setLatinKeyboard(mSymbolsKeyboard);
				mSymbolsKeyboard.setShifted(false);
			}
		} else {
			inputConnectionManager.onKey(keyCode);
		}
	}

	public void onText(CharSequence text) {
		inputConnectionManager.onText(text);
	}

	public void setSuggestions(
	CompletionInfo[] completions, boolean completion, boolean typedWordValid) {
		mCompletions = completions;
		if (completions != null && completions.length > 0) {
			setCandidatesViewShown(true);
		} else if (isExtractViewShown()) {
			setCandidatesViewShown(true);
		}
		if (mCandidatesView != null) {
			List<String> suggestions = new ArrayList<String>();
			if (completions != null) {
				for (int i = 0; i < completions.length; i++) {
					suggestions.add(completions[i].getText().toString());
				}
			}
			mCandidatesView.setSuggestions(suggestions, completion, typedWordValid);
		}
	}

	private void handleShift() {
		if (mInputView == null) {
			return;
		}

		Keyboard currentKeyboard = mInputView.getKeyboard();
		if (mQwertyKeyboard == currentKeyboard) {
			// Alphabet keyboard
			checkToggleCapsLock();
			mInputView.setShifted(mCapsLock || !mInputView.isShifted());
		} else if (currentKeyboard == mSymbolsKeyboard) {
			mSymbolsKeyboard.setShifted(true);
			setLatinKeyboard(mSymbolsShiftedKeyboard);
			mSymbolsShiftedKeyboard.setShifted(true);
		} else if (currentKeyboard == mSymbolsShiftedKeyboard) {
			mSymbolsShiftedKeyboard.setShifted(false);
			setLatinKeyboard(mSymbolsKeyboard);
			mSymbolsKeyboard.setShifted(false);
		}
	}

	private void handleClose() {
		inputConnectionManager.commitText();
		requestHideSelf(0);
		mInputView.closing();
	}

	private IBinder getToken() {
		final Dialog dialog = getWindow();
		if (dialog == null) {
			return null;
		}
		final Window window = dialog.getWindow();
		if (window == null) {
			return null;
		}
		return window.getAttributes().token;
	}

	private void handleLanguageSwitch() {
		mInputMethodManager.switchToNextInputMethod(getToken(), false /* onlyCurrentIme */);
	}

	private void checkToggleCapsLock() {
		long now = System.currentTimeMillis();
		if (mLastShiftTime + 800 > now) {
			mCapsLock = !mCapsLock;
			mLastShiftTime = 0;
		} else {
			mLastShiftTime = now;
		}
	}

	public void pickDefaultCandidate() {
		pickSuggestion(0);
	}

	public void pickSuggestion(int index) {
		if (mCompletionOn && mCompletions != null && index < mCompletions.length) {
			CompletionInfo ci = mCompletions[index];
			getCurrentInputConnection().commitCompletion(ci);
			if (mCandidatesView != null) {
				mCandidatesView.clear();
			}
			updateShiftKeyState(getCurrentInputEditorInfo());
		} else  if (mPredictionOn && mCompletions != null && index < mCompletions.length){
			inputConnectionManager.composingText(mCompletions[index].getText());
			updateShiftKeyState(getCurrentInputEditorInfo());
		}
	}

	public void swipeRight() {
		if (mCompletionOn) {
			pickDefaultCandidate();
		}
	}

	public void swipeLeft() {
		inputConnectionManager.handleBackspace();
	}

	public void swipeDown() {
		handleClose();
	}

	public void swipeUp() {}

	public void onPress(int primaryCode) {}

	public void onRelease(int primaryCode) {}

	public boolean getCompletionOn() { return mCompletionOn;}
	public boolean getPredictionOn() { return mPredictionOn;}
	public LatinKeyboardView getInputView() {return mInputView;}
	public UIHandler getInterfaceHandler() {return mUIHandler;}
	public InputMethodManager getInputMethodManager() { return mInputMethodManager;}
}
