/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Anselm Brehme, Phillip Schichtel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.cubeisland.engine.formatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.cubeisland.engine.formatter.context.FormatContext;
import de.cubeisland.engine.formatter.formatter.Formatter;

public class DefaultMessageCompositor implements MessageCompositor
{
    public static final char MACRO_BEGIN = '{';
    public static final char MACRO_END = '}';
    public static final char MACRO_ESCAPE = '\\';
    public static final char MACRO_SEPARATOR = ':';

    private final Locale defaultLocale;

    public DefaultMessageCompositor()
    {
        this(Locale.getDefault());
    }

    public DefaultMessageCompositor(Locale defaultLocale)
    {
        this.defaultLocale = defaultLocale;
    }

    private enum State
    {
        NONE,
        START,
        POS,
        TYPE,
        ARGUMENTS
    }

    public final String composeMessage(String sourceMessage, Object... messageArgs)
    {
        return this.composeMessage(this.defaultLocale, sourceMessage, messageArgs);
    }

    public final String composeMessage(Locale locale, String sourceMessage, Object... messageArgs)
    {
        State state = State.NONE;
        boolean escape = false;
        // {[[<position>:]type[:<args>]]}
        StringBuilder finalString = new StringBuilder();
        char[] chars = sourceMessage.toCharArray();
        int curPos = 0;

        StringBuilder posBuffer = new StringBuilder();
        StringBuilder typeBuffer = new StringBuilder();
        StringBuilder argsBuffer = new StringBuilder();
        List<String> typeArguments = null;

        for (char curChar : chars)
        {
            switch (state)
            {
            case NONE:
                switch (curChar)
                {
                case MACRO_BEGIN:
                    if (escape)
                    {
                        escape = false;
                        finalString.append(curChar);
                        break;
                    }
                    state = State.START;
                    posBuffer = new StringBuilder();
                    typeBuffer = new StringBuilder();
                    break;
                case MACRO_ESCAPE:
                    if (escape)
                    {
                        finalString.append(curChar);
                        escape = false;
                    }
                    else
                    {
                        escape = true;
                    }
                    break;
                case MACRO_SEPARATOR:
                case MACRO_END:
                default:
                    if (escape)
                    {
                        escape = false;
                        finalString.append(MACRO_ESCAPE);
                    }
                    finalString.append(curChar);
                    break;
                }
                break;
            case START:
                switch (curChar)
                {
                case MACRO_BEGIN:
                case MACRO_ESCAPE:
                case MACRO_SEPARATOR:
                    state = State.NONE;
                    finalString.append(MACRO_BEGIN).append(curChar);
                    break;
                case MACRO_END:
                    this.format(locale, null, null, messageArgs[curPos], finalString);
                    curPos++;
                    state = State.NONE;
                    break;
                default: // expecting position OR type
                    if (Character.isDigit(curChar)) // pos
                    {
                        state = State.POS;
                        posBuffer.append(curChar);
                    }
                    else if ((curChar >= 'a' && curChar <= 'z') || (curChar >= 'A' && curChar <= 'Z')) // type
                    {
                        state = State.TYPE;
                        typeBuffer = new StringBuilder().append(curChar);
                    }
                    else
                    {
                        state = State.NONE;
                        finalString.append(MACRO_BEGIN).append(curChar);
                        break;
                    }
                    break;
                }
                break;
            case POS:
                switch (curChar)
                {
                case MACRO_BEGIN:
                case MACRO_ESCAPE:
                    state = State.NONE;
                    finalString.append(MACRO_BEGIN).append(posBuffer).append(curChar);
                    break;
                case MACRO_SEPARATOR:
                    state = State.TYPE;
                    break;
                case MACRO_END:
                    this.format(locale, null, null, messageArgs[Integer.valueOf(posBuffer.toString())], finalString);
                    state = State.NONE;
                    break;
                default:
                    if (Character.isDigit(curChar)) // pos
                    {
                        posBuffer.append(curChar);
                    }
                    else
                    {
                        state = State.NONE;
                        finalString.append(MACRO_BEGIN).append(posBuffer).append(curChar);
                    }
                    break;
                }
                break;
            case TYPE:
                switch (curChar)
                {
                case MACRO_BEGIN:
                case MACRO_ESCAPE:
                    state = State.NONE;
                    finalString.append(MACRO_BEGIN).append(posBuffer).append(posBuffer.length() == 0 ? MACRO_SEPARATOR : "")
                               .append(typeBuffer).append(curChar);
                    break;
                case MACRO_SEPARATOR:
                    if (typeBuffer.length() == 0)
                    {
                        finalString.append(MACRO_BEGIN);
                        if (posBuffer.length() != 0)
                        {
                            finalString.append(posBuffer).append(MACRO_SEPARATOR);
                        }
                        finalString.append(curChar);
                        state = State.NONE;
                        posBuffer = new StringBuilder();
                        break;
                    }
                    state = State.ARGUMENTS;
                    typeArguments = new ArrayList<String>();
                    argsBuffer = new StringBuilder();
                    break;
                case MACRO_END:
                    if (typeBuffer.length() == 0)
                    {
                        finalString.append(MACRO_BEGIN);
                        if (posBuffer.length() != 0)
                        {
                            finalString.append(posBuffer).append(MACRO_SEPARATOR);
                        }
                        finalString.append(curChar);
                        state = State.NONE;
                        posBuffer = new StringBuilder();
                        break;
                    }
                    int pos = curPos;
                    if (posBuffer.length() == 0)
                    {
                        curPos++; // No specified arg pos, increment counting pos...
                    }
                    else
                    {
                        pos = Integer.valueOf(posBuffer.toString()); // Specified arg pos, NO increment counting pos.
                    }
                    this.format(locale, typeBuffer.toString(), null, messageArgs[pos], finalString);
                    state = State.NONE;
                    break;
                default:
                    if ((curChar >= 'a' && curChar <= 'z') ||
                        (curChar >= 'A' && curChar <= 'Z') ||
                        Character.isDigit(curChar))
                    {
                        typeBuffer.append(curChar);
                    }
                    else
                    {
                        finalString.append(MACRO_BEGIN);
                        if (posBuffer.length() != 0)
                        {
                            finalString.append(posBuffer).append(MACRO_SEPARATOR);
                        }
                        finalString.append(curChar);
                        state = State.NONE;
                        posBuffer = new StringBuilder();
                        break;
                    }
                }
                break;
            case ARGUMENTS:
                switch (curChar)
                {
                case MACRO_ESCAPE:
                    if (escape) // "\\\\"
                    {
                        escape = false;
                        argsBuffer.append(curChar);
                        break;
                    }
                    escape = true;
                case MACRO_SEPARATOR:
                    if (escape)
                    {
                        argsBuffer.append(curChar);
                        break;
                    }
                    typeArguments.add(argsBuffer.toString());
                    argsBuffer = new StringBuilder(); // Next Flag...
                    break;
                case MACRO_END:
                    if (escape)
                    {
                        argsBuffer.append(curChar);
                    }
                    else
                    {
                        int pos = curPos;
                        if (posBuffer.length() == 0)
                        {
                            curPos++; // No specified arg pos, increment counting pos...
                        }
                        else
                        {
                            pos = Integer.valueOf(posBuffer.toString()); // Specified arg pos, NO increment counting pos.
                        }
                        typeArguments.add(argsBuffer.toString());
                        this.format(locale, typeBuffer.toString(), typeArguments, messageArgs[pos], finalString);
                        state = State.NONE;
                    }
                    break;
                default:
                    argsBuffer.append(curChar);
                }
                break;
            }
        }
        return finalString.toString();
    }

    private Map<String, List<Formatter>> formatters = new HashMap<String, List<Formatter>>();
    private Set<Formatter> defaultFormatters = new HashSet<Formatter>();

    public void registerFormatter(Formatter<?> formatter)
    {
        for (String name : formatter.names())
        {
            List<Formatter> list = this.formatters.get(name);
            if (list == null)
            {
                this.formatters.put(name, list = new ArrayList<Formatter>());
            }
            list.add(formatter);
        }
    }

    public void registerDefaultFormatter(Formatter formatter)
    {
        this.defaultFormatters.add(formatter);
    }

    // override in CE to append color at the end of format
    @SuppressWarnings("unchecked")
    private void format(Locale locale, String type, List<String> typeArguments, Object messageArgument, StringBuilder finalString)
    {
        if (type == null)
        {
            for (Formatter formatter : defaultFormatters)
            {
                if (formatter.isApplicable(messageArgument.getClass()))
                {
                    this.format(formatter, new FormatContext(formatter, null, locale, typeArguments), messageArgument, finalString);
                    return;
                }
            }
            finalString.append(String.valueOf(messageArgument));
            return;
        }
        List<Formatter> formatterList = this.formatters.get(type);
        if (formatterList != null)
        {
            for (Formatter formatter : formatterList)
            {
                if (formatter.isApplicable(messageArgument.getClass()))
                {
                    this.format(formatter, new FormatContext(formatter, type, locale, typeArguments), messageArgument, finalString);
                    return;
                }
            }
        }
        for (Formatter formatter : defaultFormatters)
        {
            if (formatter.isApplicable(messageArgument.getClass()))
            {
                this.format(formatter, new FormatContext(formatter, type, locale, typeArguments), messageArgument, finalString);
                return;
            }
        }
        throw new MissingFormatterException(type, messageArgument.getClass());
    }

    @SuppressWarnings("unchecked")
    private void format(Formatter formatter, FormatContext context, Object messageArgument, StringBuilder finalString)
    {
        this.preFormat(formatter, context, messageArgument, finalString);
        finalString.append(formatter.format(messageArgument, context));
        this.postFormat(formatter, context, messageArgument, finalString);
    }

    public void postFormat(Formatter formatter, FormatContext context, Object messageArgument, StringBuilder finalString)
    {

    }

    public void preFormat(Formatter formatter, FormatContext context, Object messageArgument, StringBuilder finalString)
    {

    }
}