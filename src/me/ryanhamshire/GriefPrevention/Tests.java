package me.ryanhamshire.GriefPrevention;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class Tests
{
   @Test
   public void TrivialTest()
   {
       assertTrue(true);
   }
   
   @Test
   public void WordFinder_BeginningMiddleEnd()
   {
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
   public void WordFinder_Casing()
   {
       WordFinder finder = new WordFinder(Arrays.asList("aLPhA"));
       assertTrue(finder.hasMatch("alpha"));
       assertTrue(finder.hasMatch("aLPhA"));
       assertTrue(finder.hasMatch("AlpHa"));
       assertTrue(finder.hasMatch("ALPHA"));
   }
   
   @Test
   public void WordFinder_Punctuation()
   {
       WordFinder finder = new WordFinder(Arrays.asList("alpha"));
       assertTrue(finder.hasMatch("What do you think,alpha?"));
   }
   
   @Test
   public void WordFinder_NoMatch()
   {
       WordFinder finder = new WordFinder(Arrays.asList("alpha"));
       assertFalse(finder.hasMatch("Unit testing is smart."));
   }
}