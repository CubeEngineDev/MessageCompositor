/*
 * The MIT License
 * Copyright © 2013 Cube Island
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.cubeengine.dirigent.formatter;

import org.cubeengine.dirigent.parser.component.Component;
import org.cubeengine.dirigent.context.Context;
import org.cubeengine.dirigent.context.Arguments;

/**
 * Indicates a specific Formatter which doesn't need any message input parameters. Instead it's used for constant
 * expressions which don't consume any parameters.
 */
public abstract class ConstantFormatter extends Formatter<Void>
{
    @Override
    public boolean isApplicable(Object input)
    {
        return true;
    }

    @Override
    public final Component format(Void input, Context context, Arguments args)
    {
        return format(context, args);
    }

    /**
     * Formats the Constant expression
     *
     * @param context The compose context
     * @param args The arguments of the macro.
     *
     * @return the resulting Component
     */
    public abstract Component format(Context context, Arguments args);
}
