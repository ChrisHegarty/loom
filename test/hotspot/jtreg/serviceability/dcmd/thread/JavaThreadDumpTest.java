/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @summary Basic test for jcmd JavaThread.dump
 * @library /test/lib
 * @run testng/othervm -Djdk.trackAllVirtualThreads=true JavaThreadDumpTest
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import jdk.test.lib.dcmd.PidJcmdExecutor;
import jdk.test.lib.process.OutputAnalyzer;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class JavaThreadDumpTest {

    private Path genThreadDumpPath(String suffix) throws IOException {
        Path dir = Path.of(".").toAbsolutePath();
        Path file = Files.createTempFile(dir, "threads-", suffix);
        Files.delete(file);
        return file;
    }

    private OutputAnalyzer threadDump(Path file, String... options) {
        String cmd = "JavaThread.dump";
        for (String option : options) {
            cmd += " " + option;
        }
        return new PidJcmdExecutor().execute(cmd + " " + file);
    }

    private boolean find(Path file, String text) throws IOException {
        try (Stream<String> stream = Files.lines(file)) {
            return  stream.anyMatch(line -> line.indexOf(text) >= 0);
        }
    }

    /**
     * Test thread dump in plain text format.
     */
    private void testPlainThreadDump(Path file, String... options) throws IOException {
        threadDump(file, options).shouldMatch("Created");

        // test that thread dump contains the name and id of the current thread
        String name = Thread.currentThread().getName();
        long tid = Thread.currentThread().getId();
        String expected = "\"" + name + "\" #" + tid;
        assertTrue(find(file, expected), expected + " not found in " + file);
    }

    /**
     * Test thread dump, should be in plain text format.
     */
    public void testThreadDump() throws IOException {
        Path file = genThreadDumpPath(".txt");
        testPlainThreadDump(file);
    }

    /**
     * Test thread dump in plain text format.
     */
    public void testPlainThreadDump() throws IOException {
        Path file = genThreadDumpPath(".txt");
        testPlainThreadDump(file, "-format=plain");
    }

    /**
     * Test thread in JSON format.
     */
    public void testJsonThreadDump() throws IOException {
        Path file = genThreadDumpPath(".json");
        threadDump(file, "-format=json").shouldMatch("Created");

        // test that thread dump contains id of the current thread
        long tid = Thread.currentThread().getId();
        String expected = "\"tid\": " + tid;
        assertTrue(find(file, expected), expected + " not found in " + file);
    }

    /**
     * Test that an existing file is not overwritten.
     */
    public void testNotClobbered() throws IOException {
        Path file = genThreadDumpPath(".txt");
        Files.writeString(file, "xxx");

        threadDump(file, "").shouldMatch("exists");

        // file should not be overridden
        assertEquals(Files.readString(file), "xxx");
    }

    /**
     * Test overwriting an existing file.
     */
    public void testOverrideFile() throws IOException {
        Path file = genThreadDumpPath(".txt");
        Files.writeString(file, "xxx");
        testPlainThreadDump(file,"-overwrite");
    }
}