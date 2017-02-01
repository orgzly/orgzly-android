/*
 *  Copyright 2010 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

// Taken from https://forge.onehippo.org/svn/jcr-shell/trunk/core/src/main/java/org/onehippo/forge/jcrshell/util/QuotedStringTokenizer.java

package com.orgzly.android.util;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/** StringTokenizer with Quoting support.
 *
 * This class is a copy of the java.util.StringTokenizer API and
 * the behavior is the same, except that single and double quoted
 * string values are recognized.
 * Delimiters within quotes are not considered delimiters.
 * Quotes can be escaped with '\'.
 *
 * @see java.util.StringTokenizer
 * Based on the tokenizer in org.mortbay.util
 */
public class QuotedStringTokenizer extends StringTokenizer {
    private static final int MAX_INIT_CAPACITY = 512;
    private static final String DEFAULT_DELIMITER = "\t\n\r";
    private String str;
    private String delimiter = DEFAULT_DELIMITER;
    private boolean returnQuotes = false;
    private boolean returnTokens = false;
    private StringBuilder token;
    private boolean hasToken = false;
    private int pos = 0;
    private int lastStart = 0;

    private enum State {
        START, TOKEN, SINGLE, DOUBLE
    }

    /* ------------------------------------------------------------ */
    public QuotedStringTokenizer(String string, String delim, boolean returnTokens, boolean returnQuotes) {
        super("");
        this.str = string;
        this.returnTokens = returnTokens;
        this.returnQuotes = returnQuotes;

        if (delim != null) {
            delimiter = delim;
        }

        if (delimiter.indexOf('\'') >= 0 || delimiter.indexOf('"') >= 0) {
            throw new IllegalArgumentException("Can't use quotes as delimiters: " + delimiter);
        }
        if (str.length() > (2 * MAX_INIT_CAPACITY) ) {
            token = new StringBuilder(MAX_INIT_CAPACITY);
        } else {
            token = new StringBuilder(str.length() / 2);
        }
    }

    /* ------------------------------------------------------------ */
    public QuotedStringTokenizer(String str, String delim, boolean returnTokens) {
        this(str, delim, returnTokens, false);
    }

    /* ------------------------------------------------------------ */
    public QuotedStringTokenizer(String str, String delim) {
        this(str, delim, false, false);
    }

    /* ------------------------------------------------------------ */
    public QuotedStringTokenizer(String str) {
        this(str, null, false, false);
    }

    /* ------------------------------------------------------------ */
    public boolean hasMoreTokens() {
        // Already found a token
        if (hasToken) {
            return true;
        }

        lastStart = pos;

        State state = State.START;

        boolean escape = false;
        while (pos < str.length()) {
            char c = str.charAt(pos++);

            switch (state) {
                case START: // Start
                    if (delimiter.indexOf(c) >= 0) {
                        if (returnTokens) {
                            token.append(c);
                            hasToken = true;
                        }
                    } else if (c == '\'') {
                        if (returnQuotes) {
                            token.append(c);
                        }
                        state = State.SINGLE;
                    } else if (c == '\"') {
                        if (returnQuotes) {
                            token.append(c);
                        }
                        state = State.DOUBLE;
                    } else {
                        token.append(c);
                        hasToken = true;
                        state = State.TOKEN;
                    }
                    continue;

                case TOKEN: // Token
                    hasToken = true;
                    if (delimiter.indexOf(c) >= 0) {
                        if (returnTokens) {
                            pos--;
                        }
                        return hasToken;
                    } else if (c == '\'') {
                        if (returnQuotes) {
                            token.append(c);
                        }
                        state = State.SINGLE;
                    } else if (c == '\"') {
                        if (returnQuotes) {
                            token.append(c);
                        }
                        state = State.DOUBLE;
                    } else {
                        token.append(c);
                    }
                    continue;

                case SINGLE: // Single Quote
                    hasToken = true;
                    if (escape) {
                        escape = false;
                        token.append(c);
                    } else if (c == '\'') {
                        if (returnQuotes) {
                            token.append(c);
                        }
                        state = State.TOKEN;
                    } else if (c == '\\') {
                        if (returnQuotes) {
                            token.append(c);
                        }
                        escape = true;
                    } else {
                        token.append(c);
                    }
                    continue;

                case DOUBLE: // Double Quote
                    hasToken = true;
                    if (escape) {
                        escape = false;
                        token.append(c);
                    } else if (c == '\"') {
                        if (returnQuotes) {
                            token.append(c);
                        }
                        state = State.TOKEN;
                    } else if (c == '\\') {
                        if (returnQuotes) {
                            token.append(c);
                        }
                        escape = true;
                    } else {
                        token.append(c);
                    }
                    continue;
            }
        }

        return hasToken;
    }

    /* ------------------------------------------------------------ */
    public String nextToken() {
        if (!hasMoreTokens() || token == null) {
            throw new NoSuchElementException();
        }
        String t = token.toString();
        token.setLength(0);
        hasToken = false;
        return t;
    }

    /* ------------------------------------------------------------ */
    public String nextToken(String delim) {
        delimiter = delim;
        pos = lastStart;
        token.setLength(0);
        hasToken = false;
        return nextToken();
    }

    /* ------------------------------------------------------------ */
    public boolean hasMoreElements() {
        return hasMoreTokens();
    }

    /* ------------------------------------------------------------ */
    public Object nextElement() {
        return nextToken();
    }

    /* ------------------------------------------------------------ */
    /** Not implemented.
     */
    public int countTokens() {
        return -1;
    }

    /* ------------------------------------------------------------ */
    /** Quote a string.
     * The string is quoted only if quoting is required due to
     * embeded delimiters, quote characters or the
     * empty string.
     * @param s The string to quote.
     * @return quoted string
     */
    public static String quote(String s, String delim) {
        if (s == null) {
            return null;
        }
        if (s.length() == 0) {
            return "\"\"";
        }

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\' || c == '\'' || delim.indexOf(c) >= 0) {
                StringBuilder b = new StringBuilder(s.length() + 8);
                quote(b, s);
                return b.toString();
            }
        }

        return s;
    }

    /* ------------------------------------------------------------ */
    /** Quote a string into a StringBuilder.
     * @param buf The StringBuilder
     * @param s The String to quote.
     */
    public static void quote(StringBuilder buf, String s) {
        synchronized (buf) {
            buf.append('"');
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '"') {
                    buf.append("\\\"");
                    continue;
                }
                if (c == '\\') {
                    buf.append("\\\\");
                    continue;
                }
                buf.append(c);
                continue;
            }
            buf.append('"');
        }
    }

    /* ------------------------------------------------------------ */
    /** Unquote a string.
     * @param s The string to unquote.
     * @return quoted string
     */
    public static String unquote(String s) {
        if (s == null) {
            return null;
        }
        if (s.length() < 2) {
            return s;
        }

        char first = s.charAt(0);
        char last = s.charAt(s.length() - 1);
        if (first != last || (first != '"' && first != '\'')) {
            return s;
        }
        StringBuilder b = new StringBuilder(s.length() - 2);
        synchronized (b) {
            boolean quote = false;
            for (int i = 1; i < s.length() - 1; i++) {
                char c = s.charAt(i);

                if (c == '\\' && !quote) {
                    quote = true;
                    continue;
                }
                quote = false;
                b.append(c);
            }

            return b.toString();
        }
    }
}
