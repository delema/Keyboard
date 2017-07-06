package com.example.android.softkeyboard;

import android.content.res.Resources;
import android.os.Message;
import android.view.inputmethod.CompletionInfo;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import es.lema.orthos.inputmethod.utils.LeakGuardHandlerWrapper;

public final class UIHandler extends LeakGuardHandlerWrapper<SoftKeyboard> {

    private static final int MSG_UPDATE_SUGGESTION = 0;

    private int delayToUpdateSuggestions;

    public UIHandler(@Nonnull final SoftKeyboard softKeyboard) {
        super(softKeyboard);
    }

    public void onCreate() {
        final SoftKeyboard softKeyboard = getOwnerInstance();
        if (softKeyboard == null) {
            return;
        }
        final Resources resources = softKeyboard.getResources();
        delayToUpdateSuggestions = resources.getInteger(R.integer.delay_to_update_suggestions);
    }

    @Override
    public void handleMessage(final Message message) {
        final SoftKeyboard softKeyboard = getOwnerInstance();
        if (softKeyboard == null) {
            return;
        }
        switch (message.what) {
            case MSG_UPDATE_SUGGESTION:
                cancelUpdateSuggestion();
                List<String> wordList = (List<String>) message.obj;
                CompletionInfo[] completions = null;
                if (wordList != null && !wordList.isEmpty()) {
                    List<CompletionInfo> list = new ArrayList<CompletionInfo>();
                    for (int i = 0; i < wordList.size(); i++) {
                        list.add(new CompletionInfo(i, i, wordList.get(i)));
                    }
                    completions = list.toArray(new CompletionInfo[list.size()]);
                }
                softKeyboard.setSuggestions(completions, false, false);
                break;
        }
    }

    public void updateSuggestion(List<String> wordList) {
        Message message = obtainMessage(MSG_UPDATE_SUGGESTION, 0, 0, wordList);
        sendMessageDelayed(message, delayToUpdateSuggestions);
    }

    public void cancelUpdateSuggestion() {
        removeMessages(MSG_UPDATE_SUGGESTION);
    }

    public boolean hasPendingUpdateSuggestions() {
        return hasMessages(MSG_UPDATE_SUGGESTION);
    }
}
