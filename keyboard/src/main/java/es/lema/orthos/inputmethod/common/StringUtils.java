/*
 * Copyright (C) 2012 The Android Open Source Project
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

package es.lema.orthos.inputmethod.common;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class StringUtils {
    public static final int CAPITALIZE_NONE = 0;  // No caps, or mixed case
    public static final int CAPITALIZE_FIRST = 1; // First only
    public static final int CAPITALIZE_ALL = 2;   // All caps

    @Nonnull
    private static final String EMPTY_STRING = "";

    private static final char CHAR_LINE_FEED = 0X000A;
    private static final char CHAR_VERTICAL_TAB = 0X000B;
    private static final char CHAR_FORM_FEED = 0X000C;
    private static final char CHAR_CARRIAGE_RETURN = 0X000D;
    private static final char CHAR_NEXT_LINE = 0X0085;
    private static final char CHAR_LINE_SEPARATOR = 0X2028;
    private static final char CHAR_PARAGRAPH_SEPARATOR = 0X2029;

    private StringUtils() {
        // This utility class is not publicly instantiable.
    }

    // Taken from android.text.TextUtils. We are extensively using this method in many places,
    // some of which don't have the android libraries available.
    /**
     * Returns true if the string is null or 0-length.
     * @param str the string to be examined
     * @return true if str is null or zero length
     */
    public static boolean isEmpty(@Nullable final CharSequence str) {
        return (str == null || str.length() == 0);
    }

    /**
     * Returns whether the last composed word contains line-breaking character (e.g. CR or LF).
     * @param text the text to be examined.
     * @return {@code true} if the last composed word contains line-breaking separator.
     */
    public static boolean hasLineBreakCharacter(@Nullable final String text) {
        if (isEmpty(text)) {
            return false;
        }
        for (int i = text.length() - 1; i >= 0; --i) {
            final char c = text.charAt(i);
            switch (c) {
                case CHAR_LINE_FEED:
                case CHAR_VERTICAL_TAB:
                case CHAR_FORM_FEED:
                case CHAR_CARRIAGE_RETURN:
                case CHAR_NEXT_LINE:
                case CHAR_LINE_SEPARATOR:
                case CHAR_PARAGRAPH_SEPARATOR:
                    return true;
            }
        }
        return false;
    }
}
