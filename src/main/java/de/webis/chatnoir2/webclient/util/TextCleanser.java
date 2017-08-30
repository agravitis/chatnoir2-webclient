/*
 * ChatNoir 2 Web Frontend.
 * Copyright (C) 2014-2017 Janek Bevendorff, Webis Group
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package de.webis.chatnoir2.webclient.util;


/**
 * Text cleansing tools.
 */
public class TextCleanser
{
    private static final String[] WESTERN_UNICODE_CHARS = {
            "à", "á", "â", "ã", "ä", "å", "æ", "ç", "è", "é", "ê", "ë", "í", "î",
            "ï", "ò", "ó", "ô", "õ", "ö", "ø", "ù", "ú", "û", "ü", "ý", "ÿ", "œ", "ß",
            "À", "Á", "Â", "Ã", "Ä", "Å", "Æ", "Ç", "È", "É", "Ê", "Ë", "Í", "Î",
            "Ï", "Ò", "Ó", "Ô", "Õ", "Ö", "Ø", "Ù", "Ú", "Û", "Ü", "Ý", "Œ", "Ÿ"
    };

    private static final String[] BROKEN_ISO_8859_1_CHARS = {
            "Ã ", "Ã¡", "Ã¢", "Ã£", "Ã¤", "Ã¥", "Ã¦", "Ã§", "Ã¨", "Ã©", "Ãª", "Ã«", "Ã­", "Ã®",
            "Ã¯", "Ã²", "Ã³", "Ã´", "Ãµ", "Ã¶", "Ã¸", "Ã¹", "Ãº", "Ã»", "Ã¼", "Ã½", "Ã¿", "Å", "Ã",
            "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã",
            "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Å", "Å¸"
    };

    private static final String[] BROKEN_ISO_8859_15_CHARS = {
            "Ã ", "Ã¡", "Ã¢", "Ã£", "Ã€", "Ã¥", "ÃŠ", "Ã§", "Ãš", "Ã©", "Ãª", "Ã«", "Ã­", "Ã®",
            "Ã¯", "Ã²", "Ã³", "ÃŽ", "Ãµ", "Ã¶", "Ãž", "Ã¹", "Ãº", "Ã»", "ÃŒ", "Ãœ", "Ã¿", "Å", "Ã",
            "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã",
            "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Ã", "Å", "Åž"
    };

    private boolean mIsHtml;
    private String mString;

    /**
     * Cleanse the given string by removing any clutter such as runs of special characters, hyphens or spaces.
     * Performs all available text cleansing operations provided by this class.
     *
     * @param str non-HTML text to cleanse
     * @return cleansed and more readable string
     */
    public static String cleanseAll(String str)
    {
        return cleanseAll(str, false);
    }

    /**
     * Cleanse the given string by removing any clutter such as runs of special characters, hyphens or spaces.
     * Performs all available text cleansing operations provided by this class.
     *
     * @param str text to cleanse
     * @param html input is HTML escaped
     * @return cleansed and more readable string
     */
    public static String cleanseAll(String str, boolean html)
    {
        // order is important!
        return new TextCleanser(str, html)
                .encodingErrors()
                .doubleHtmlEscape()
                .unclosedBrackets()
                .repeatedWords()
                .whitespace()
                .get();
    }

    /**
     * @param str string to cleanse
     * @param html whether string is HTML
     */
    public TextCleanser(String str, boolean html)
    {
        mString = str;
        mIsHtml = html;
    }

    /**
     * Return cleansed string.
     *
     * @return cleansed string
     */
    public String get()
    {
        return mString;
    }

    /**
     * Remove question mark place holder characters caused by broken encoding
     * and repair western multi-byte characters which were interpreted as single-byte.
     */
    public TextCleanser encodingErrors()
    {
        // repair western unicode characters which were interpreted as ISO 8859-1 or ISO 8859-15
        for (int i = 0; i < WESTERN_UNICODE_CHARS.length; ++i) {
            mString = mString.replace(BROKEN_ISO_8859_1_CHARS[i], WESTERN_UNICODE_CHARS[i]);
            if (!BROKEN_ISO_8859_1_CHARS[i].equals(BROKEN_ISO_8859_15_CHARS[i])) {
                mString = mString.replace(BROKEN_ISO_8859_15_CHARS[i], WESTERN_UNICODE_CHARS[i]);
            }
        }

        // strip unicode replacement characters
        mString =  mString.replace("\ufffd", "");

        return this;
    }

    /**
     * Correct double HTML escaping.
     */
    public TextCleanser doubleHtmlEscape()
    {
        mString = mString.replaceAll("&amp;(\\w{1,8});", "&$1;");
        return this;
    }

    /**
     * Remove short passages (up to 10 characters) of text at the end if they are preceded by
     * an opening bracket which isn't closed.
     */
    public TextCleanser unclosedBrackets()
    {
        if (mIsHtml) {
            mString = mString.replaceFirst("(\\(|\\[|&lt;).{0,10}(?!(?:\\)|]|&gt;))\\s*$", "").trim();
        } else {
            mString = mString.replaceFirst("(\\(|\\[|<).{0,10}(?!(?:\\)|]|>))\\s*$", "").trim();
        }
        return this;
    }

    /**
     * Remove consecutive runs of white space.
     */
    public TextCleanser whitespace()
    {
        if (!mIsHtml) {
            mString = mString.replaceAll("[ \\s]+", " ");
        } else {
            mString = mString.replaceAll("(?:[ \\s]|&nbsp;)+", " ");
        }

        return this;
    }

    /**
     * Remove consecutive runs of non-word special characters.
     */
    @Deprecated
    public TextCleanser nonWordChars()
    {
        if (!mIsHtml) {
            mString = mString.trim()
                // remove runs of special characters
                .replaceAll("(([,;.:\\-_#'+~*^°!\"§$%&/()={}<>|])\\2)(?:\\w+\\1)*", "").trim()
                // remove non-word characters at the beginning or end
                // (opening brackets at the beginning or closing at the end are okay)
                .replaceAll("^[,;.:\\-_#'+~*^°!\"§$%&/)=}\\]>|]+|[,;.:\\-_#'+~*^°!\"§$%&/(={\\[<|]+$", "").trim();
        } else {
            // do the same, but also consider HTML entities
            mString = mString.trim()
                .replaceAll("((&amp;|&lt;|&gt;|[,;.:\\-_#'+~*^°!\"§$%/()={}|])\\2)(?:\\w+\\1)*", "").trim()
                .replaceAll("^(&amp;|&gt;|[,;.:\\-_#'+~*^°!\"§$%/)=}\\]|])+|(&amp;|&lt;|[,;.:\\-_#'+~*^°!\"§$%/(={\\[|])+$", "").trim();
        }

        return this;
    }

    /**
     * Remove repeated words.
     */
    public TextCleanser repeatedWords()
    {
        if (!mIsHtml) {
            mString = mString.replaceAll("([^\\s]+?) \\1{2,}", "").trim();
        } else {
            mString = mString.replaceAll("((?:<(\\w+)>)?([^\\s])+?(?:</\\2>)?)(\\s+\\1){2,}", "$1 $1").trim();
        }

        return this;
    }
}
