package com.example.android.softkeyboard;

import android.inputmethodservice.Keyboard;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodSubtype;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import es.lema.orthos.OrthosServiceManager;
import es.lema.orthos.inputmethod.common.StringUtils;
import es.lema.orthos.service.IOrthosSession;
import es.lema.orthos.service.Word;
import es.lema.orthos.service.WordList;

import static android.content.ContentValues.TAG;

public class InputConnectionManager {

    private static final int EDITOR_CONTENTS_CACHE_SIZE = 1024;
    // Regular expresion for word separator
    private static final String wordSeparators = "[\\p{Punct}\\s\\d]+";

    private final SoftKeyboard softKeyboard;
    private InputConnection inputConnection;
    private StringBuilder composingText;
    private String beforeText;
    private String afterText;
    private Pattern patternWordSeparators;

    public InputConnectionManager(
    @Nonnull final SoftKeyboard softKeyboard) {
        this.softKeyboard = softKeyboard;
        composingText = new StringBuilder();
        patternWordSeparators = Pattern.compile(wordSeparators);
    }

    public void onStartInput() {
        inputConnection = softKeyboard.getCurrentInputConnection();
        resetCursorPosition();
    }

    public void resetCursorPosition() {
        composingText.setLength(0);
        beforeText = "";
        CharSequence charSequence =
        inputConnection.getTextBeforeCursor(EDITOR_CONTENTS_CACHE_SIZE, 0);
        if (  !StringUtils.isEmpty(charSequence)
           && !isWordSeparator(charSequence.charAt(charSequence.length()-1))) {
            String[] words = patternWordSeparators.split(charSequence);
            beforeText = words[words.length - 1];
        }
        afterText = "";
        charSequence =
        inputConnection.getTextAfterCursor(EDITOR_CONTENTS_CACHE_SIZE, 0);
        if (  !StringUtils.isEmpty(charSequence)
           && !isWordSeparator(charSequence.charAt(0))) {
            String[] words = patternWordSeparators.split(charSequence);
            afterText = words[0];
        }
    }

    public boolean isWordSeparator(char code) {
        Matcher matcher =
        patternWordSeparators.matcher(new StringBuilder().append(code));
        return matcher.matches();
    }

    public void onKey(int keyCode) {
        if (keyCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else {
            handleCharacter(keyCode);
        }
    }

    public void onText(CharSequence text) {
        inputConnection.beginBatchEdit();
        commitText();
        inputConnection.commitText(text, 0);
        inputConnection.endBatchEdit();
    }

    public void commitText() {
        if (composingText.length() > 0) {
            inputConnection.commitText(composingText, 1);
        }
    }

    public void finishComposingText() {
        if (composingText.length() > 0) {
            inputConnection.finishComposingText();
        }
        resetCursorPosition();
    }

    public void composingText(CharSequence text) {
        int cursorPosition = 0;
        CharSequence charSequence =
        inputConnection.getTextBeforeCursor(Integer.MAX_VALUE, 0);
        if (!StringUtils.isEmpty(charSequence)) {
            cursorPosition = charSequence.length();
        }
        int start = cursorPosition - composingText.length() - beforeText.length();
        int end = cursorPosition + afterText.length();
        inputConnection.setComposingRegion(start, end);
        inputConnection.commitText(text, 1);
    }

    private void handleCharacter(int keyCode) {
        if (isWordSeparator((char) keyCode)) {
            commitText();
            if (keyCode == '\n') {
                sendKeyEvent(KeyEvent.KEYCODE_ENTER);
            } else {
                inputConnection.commitText(String.valueOf((char) keyCode), 1);
            }
        } else {
            if (softKeyboard.isInputViewShown()) {
                if (softKeyboard.getInputView().isShifted()) {
                    keyCode = Character.toUpperCase(keyCode);
                }
            }
            if (isAlphabet(keyCode) && softKeyboard.getPredictionOn()) {
                composingText.append((char) keyCode);
                inputConnection.setComposingText(composingText, 1);
            } else {
                inputConnection.commitText(String.valueOf((char) keyCode), 1);
            }
        }
    }

    public void handleBackspace() {
        final int length = composingText.length();
        if (length > 0) {
            composingText.delete(length - 1, length);
            inputConnection.setComposingText(composingText, 1);
        } else {
            sendKeyEvent(KeyEvent.KEYCODE_DEL);
            resetCursorPosition();
        }
    }

    private void sendKeyEvent(int keyCode) {
        inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
    }

    /**
     * Update the list of available candidates from the current composing text
     * in background.
     */
    public void updateCandidates() {
        UIHandler interfaceHandler = softKeyboard.getInterfaceHandler();
        StringBuilder candidate = new StringBuilder();
        candidate.append(beforeText)
                 .append(composingText)
                 .append(afterText);
        if (candidate.length() > 0) {
            UITask<String, List<String>> task =
            new UITask<String, List<String>>(interfaceHandler) {
                @Override
                protected List<String> doInBackground(String... params) {
                    List<String> result = new ArrayList<String>();
                    try {
                        IOrthosSession session =
                        OrthosServiceManager.getInstance().getSession(params[0]);
                        WordList wordList = session.nearest(params[1]);
                        if (wordList.isEmpty()) {
                            wordList = session.alternative(params[1]);
                        }
                        for (Word word : wordList) {
                            String form = word.getForm();
                            if (!result.contains(form)) {
                                result.add(form);
                            }
                        }
                    } catch (RemoteException e) {
                        Log.d(TAG, "updateCandidates()", e);
                    }
                    return result;
                }

                @Override
                protected void onPostExecute(List<String> result) {
                    handler.updateSuggestion(result);
                }
            };
            InputMethodSubtype subtype =
            softKeyboard.getInputMethodManager().getCurrentInputMethodSubtype();
            task.execute(subtype.getLocale(), candidate.toString());
        } else {
            interfaceHandler.updateSuggestion(null);
        }
    }

    private boolean isAlphabet(int code) {
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }
}
