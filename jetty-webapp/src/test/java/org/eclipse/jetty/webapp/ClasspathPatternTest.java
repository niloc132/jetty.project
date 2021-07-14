//
//  ========================================================================
//  Copyright (c) 1995-2021 Mort Bay Consulting Pty Ltd and others.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.webapp;

import java.net.URI;
import java.util.Arrays;
import java.util.function.Supplier;

import org.eclipse.jetty.util.IncludeExcludeSet;
import org.eclipse.jetty.util.TypeUtil;
import org.eclipse.jetty.webapp.ClasspathPattern.ByLocationOrModule;
import org.eclipse.jetty.webapp.ClasspathPattern.ByPackageOrName;
import org.eclipse.jetty.webapp.ClasspathPattern.Entry;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClasspathPatternTest
{
    private final ClasspathPattern _pattern = new ClasspathPattern();
    
    protected static Supplier<URI> NULL_SUPPLIER = new Supplier<URI>()
    {
        public URI get()
        {
            return null;
        } 
    };

    @BeforeEach
    public void before()
    {
        _pattern.clear();
        _pattern.add("org.package.");
        _pattern.add("-org.excluded.");
        _pattern.add("org.example.FooBar");
        _pattern.add("-org.example.Excluded");
        _pattern.addAll(Arrays.asList(
            "-org.example.Nested$Minus",
            "org.example.Nested",
            "org.example.Nested$Something"));

        assertThat(_pattern, Matchers.containsInAnyOrder(
            "org.package.",
            "-org.excluded.",
            "org.example.FooBar",
            "-org.example.Excluded",
            "-org.example.Nested$Minus",
            "org.example.Nested",
            "org.example.Nested$Something"
        ));
    }

    @Test
    public void testClassMatch()
    {
        assertTrue(_pattern.match("org.example.FooBar"));
        assertTrue(_pattern.match("org.example.Nested"));

        assertFalse(_pattern.match("org.example.Unknown"));
        assertFalse(_pattern.match("org.example.FooBar.Unknown"));
    }

    @Test
    public void testPackageMatch()
    {
        assertTrue(_pattern.match("org.package.Something"));
        assertTrue(_pattern.match("org.package.other.Something"));

        assertFalse(_pattern.match("org.example.Unknown"));
        assertFalse(_pattern.match("org.example.FooBar.Unknown"));
        assertFalse(_pattern.match("org.example.FooBarElse"));
    }

    @Test
    public void testExplicitNestedMatch()
    {
        assertTrue(_pattern.match("org.example.Nested$Something"));
        assertFalse(_pattern.match("org.example.Nested$Minus"));
        assertTrue(_pattern.match("org.example.Nested$Other"));
    }

    @Test
    public void testImplicitNestedMatch()
    {
        assertTrue(_pattern.match("org.example.FooBar$Other"));
        assertTrue(_pattern.match("org.example.Nested$Other"));
    }

    @Test
    public void testDoubledNested()
    {
        assertTrue(_pattern.match("org.example.Nested$Something$Else"));

        assertFalse(_pattern.match("org.example.Nested$Minus$Else"));
    }

    @Test
    public void testMatchAll()
    {
        _pattern.clear();
        _pattern.add(".");
        assertTrue(_pattern.match("org.example.Anything"));
        assertTrue(_pattern.match("org.example.Anything$Else"));
    }

    @Test
    public void testMatchFundamentalExcludeSpecific()
    {
        _pattern.clear();
        _pattern.add("javax.");
        _pattern.add("-javax.ws.rs.", "-javax.inject.");
        assertFalse(_pattern.match("org.example.Anything"));
        assertTrue(_pattern.match("javax.servlet.HttpServlet"));
        assertFalse(_pattern.match("javax.ws.rs.ProcessingException"));
    }

    @SuppressWarnings("restriction")
    @Test
    @DisabledOnJre(JRE.JAVA_8)
    public void testIncludedLocations() throws Exception
    {
        // jar from JVM classloader
        URI locString = TypeUtil.getLocationOfClass(String.class);

        // a jar from maven repo jar
        URI locJunit = TypeUtil.getLocationOfClass(Test.class);

        // class file 
        URI locTest = TypeUtil.getLocationOfClass(ClasspathPatternTest.class);

        ClasspathPattern pattern = new ClasspathPattern();
        pattern.include("something");
        assertThat(pattern.match(String.class), Matchers.is(false));
        assertThat(pattern.match(Test.class), Matchers.is(false));
        assertThat(pattern.match(ClasspathPatternTest.class), Matchers.is(false));

        // Add directory for both JVM classes
        pattern.include(locString.toASCIIString());

        // Add jar for individual class and classes directory
        pattern.include(locJunit.toString(), locTest.toString());

        assertThat(pattern.match(String.class), Matchers.is(true));
        assertThat(pattern.match(Test.class), Matchers.is(true));
        assertThat(pattern.match(ClasspathPatternTest.class), Matchers.is(true));

        pattern.add("-java.lang.String");
        assertThat(pattern.match(String.class), Matchers.is(false));
        assertThat(pattern.match(Test.class), Matchers.is(true));
        assertThat(pattern.match(ClasspathPatternTest.class), Matchers.is(true));
    }

    @SuppressWarnings("restriction")
    @Test
    @DisabledOnJre(JRE.JAVA_8)
    public void testIncludedLocationsOrModule()
    {
        // jar from JVM classloader
        URI modString = TypeUtil.getLocationOfClass(String.class);
        // System.err.println(mod_string);

        // a jar from maven repo jar
        URI locJunit = TypeUtil.getLocationOfClass(Test.class);
        // System.err.println(loc_junit);

        // class file
        URI locTest = TypeUtil.getLocationOfClass(ClasspathPatternTest.class);
        // System.err.println(loc_test);

        ClasspathPattern pattern = new ClasspathPattern();
        pattern.include("something");
        assertThat(pattern.match(String.class), Matchers.is(false));
        assertThat(pattern.match(Test.class), Matchers.is(false));
        assertThat(pattern.match(ClasspathPatternTest.class), Matchers.is(false));

        // Add module for all JVM base classes
        pattern.include("jrt:/java.base");

        // Add jar for individual class and classes directory
        pattern.include(locJunit.toString(), locTest.toString());

        assertThat(pattern.match(String.class), Matchers.is(true));
        assertThat(pattern.match(Test.class), Matchers.is(true));
        assertThat(pattern.match(ClasspathPatternTest.class), Matchers.is(true));

        pattern.add("-java.lang.String");
        assertThat(pattern.match(String.class), Matchers.is(false));
        assertThat(pattern.match(Test.class), Matchers.is(true));
        assertThat(pattern.match(ClasspathPatternTest.class), Matchers.is(true));
    }

    @SuppressWarnings("restriction")
    @Test
    @EnabledOnJre(JRE.JAVA_8)
    public void testExcludeLocations()
    {
        // jar from JVM classloader
        URI locString = TypeUtil.getLocationOfClass(String.class);
        // System.err.println(locString);

        // a jar from maven repo jar
        URI locJunit = TypeUtil.getLocationOfClass(Test.class);
        // System.err.println(locJunit);

        // class file 
        URI locTest = TypeUtil.getLocationOfClass(ClasspathPatternTest.class);
        // System.err.println(locTest);

        ClasspathPattern pattern = new ClasspathPattern();

        // include everything
        pattern.include(".");

        assertThat(pattern.match(String.class), Matchers.is(true));
        assertThat(pattern.match(Test.class), Matchers.is(true));
        assertThat(pattern.match(ClasspathPatternTest.class), Matchers.is(true));

        // Add directory for both JVM classes
        pattern.exclude(locString.toString());

        // Add jar for individual class and classes directory
        pattern.exclude(locJunit.toString(), locTest.toString());

        assertThat(pattern.match(String.class), Matchers.is(false));
        assertThat(pattern.match(Test.class), Matchers.is(false));
        assertThat(pattern.match(ClasspathPatternTest.class), Matchers.is(false));
    }

    @SuppressWarnings("restriction")
    @Test
    @DisabledOnJre(JRE.JAVA_8)
    public void testExcludeLocationsOrModule() throws Exception
    {
        // jar from JVM classloader
        URI modString = TypeUtil.getLocationOfClass(String.class);
        // System.err.println(modString);

        // a jar from maven repo jar
        URI locJunit = TypeUtil.getLocationOfClass(Test.class);
        // System.err.println(locJunit);

        // class file
        URI locTest = TypeUtil.getLocationOfClass(ClasspathPatternTest.class);
        // System.err.println(locTest);

        ClasspathPattern pattern = new ClasspathPattern();

        // include everything
        pattern.include(".");

        assertThat(pattern.match(String.class), Matchers.is(true));
        assertThat(pattern.match(Test.class), Matchers.is(true));
        assertThat(pattern.match(ClasspathPatternTest.class), Matchers.is(true));

        // Add directory for both JVM classes
        pattern.exclude("jrt:/java.base/");

        // Add jar for individual class and classes directory
        pattern.exclude(locJunit.toString(), locTest.toString());

        assertThat(pattern.match(String.class), Matchers.is(false));
        assertThat(pattern.match(Test.class), Matchers.is(false));
        assertThat(pattern.match(ClasspathPatternTest.class), Matchers.is(false));
    }
    
    @Test
    public void testWithNullLocation() throws Exception
    {
        ClasspathPattern pattern = new ClasspathPattern();
        
        IncludeExcludeSet<Entry, String> names = new IncludeExcludeSet<>(ByPackageOrName.class);
        IncludeExcludeSet<Entry, URI> locations = new IncludeExcludeSet<>(ByLocationOrModule.class);

        //Test no name or location includes or excludes - should match
        assertThat(ClasspathPattern.combine(names, "a.b.c", locations, NULL_SUPPLIER), Matchers.is(true));
        
        names.include(pattern.newEntry("a.b.", true));
        names.exclude(pattern.newEntry("d.e.", false));
       
        //Test explicit include by name no locations - should match
        assertThat(ClasspathPattern.combine(names, "a.b.c", locations, NULL_SUPPLIER), Matchers.is(true));
        
        //Test explicit exclude by name no locations - should not match
        assertThat(ClasspathPattern.combine(names, "d.e.f", locations, NULL_SUPPLIER), Matchers.is(false));
        
        //Test include by name with location includes - should match
        locations.include(pattern.newEntry("file:/foo/bar", true));
        assertThat(ClasspathPattern.combine(names, "a.b.c", locations, NULL_SUPPLIER), Matchers.is(true));
        
        //Test include by name but with location exclusions - should not match
        locations.clear();
        locations.exclude(pattern.newEntry("file:/high/low", false));
        assertThat(ClasspathPattern.combine(names, "a.b.c", locations, NULL_SUPPLIER), Matchers.is(false));
        
        //Test neither included or excluded by name, but with location exclusions - should not match
        assertThat(ClasspathPattern.combine(names, "g.b.r", locations, NULL_SUPPLIER), Matchers.is(false));
        
        //Test neither included nor excluded by name, but with location inclusions - should not match
        locations.clear();
        locations.include(pattern.newEntry("file:/foo/bar", true));
        assertThat(ClasspathPattern.combine(names, "g.b.r", locations, NULL_SUPPLIER), Matchers.is(false));
    }

    @Test
    public void testLarge()
    {
        ClasspathPattern pattern = new ClasspathPattern();
        for (int i = 0; i < 500; i++)
        {
            assertTrue(pattern.add("n" + i + "." + Integer.toHexString(100 + i) + ".Name"));
        }

        for (int i = 0; i < 500; i++)
        {
            assertTrue(pattern.match("n" + i + "." + Integer.toHexString(100 + i) + ".Name"));
        }
    }

    @Test
    public void testJvmModule()
    {
        URI uri = TypeUtil.getLocationOfClass(String.class);
        System.err.println(uri);
        System.err.println(uri.toString().split("/")[0]);
        System.err.println(uri.toString().split("/")[1]);
    }
}
