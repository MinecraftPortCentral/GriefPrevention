/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
 * Copyright (c) contributors
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
package me.ryanhamshire.GriefPrevention;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class Tests {

    @Test
    public void TrivialTest() {
        assertTrue(true);
    }

    @Test
    public void WordFinder_BeginningMiddleEnd() {
        WordFinder finder = new WordFinder(Arrays.asList("alpha", "beta", "gamma"));
        assertTrue(finder.hasMatch("alpha"));
        assertTrue(finder.hasMatch("alpha etc"));
        assertTrue(finder.hasMatch("etc alpha etc"));
        assertTrue(finder.hasMatch("etc alpha"));

        assertTrue(finder.hasMatch("beta"));
        assertTrue(finder.hasMatch("beta etc"));
        assertTrue(finder.hasMatch("etc beta etc"));
        assertTrue(finder.hasMatch("etc beta"));

        assertTrue(finder.hasMatch("gamma"));
        assertTrue(finder.hasMatch("gamma etc"));
        assertTrue(finder.hasMatch("etc gamma etc"));
        assertTrue(finder.hasMatch("etc gamma"));
    }

    @Test
    public void WordFinder_Casing() {
        WordFinder finder = new WordFinder(Arrays.asList("aLPhA"));
        assertTrue(finder.hasMatch("alpha"));
        assertTrue(finder.hasMatch("aLPhA"));
        assertTrue(finder.hasMatch("AlpHa"));
        assertTrue(finder.hasMatch("ALPHA"));
    }

    @Test
    public void WordFinder_Punctuation() {
        WordFinder finder = new WordFinder(Arrays.asList("alpha"));
        assertTrue(finder.hasMatch("What do you think,alpha?"));
    }

    @Test
    public void WordFinder_NoMatch() {
        WordFinder finder = new WordFinder(Arrays.asList("alpha"));
        assertFalse(finder.hasMatch("Unit testing is smart."));
    }
}
