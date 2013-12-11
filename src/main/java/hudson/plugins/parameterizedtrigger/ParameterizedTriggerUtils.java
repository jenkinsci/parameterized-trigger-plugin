/*
 * The MIT License
 * 
 * Copyright (c) 2013 IKEDA Yasuyuki
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

package hudson.plugins.parameterizedtrigger;

import hudson.FilePath;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.jvnet.animal_sniffer.IgnoreJRERequirement;

/**
 * Common utility methods.
 */
public class ParameterizedTriggerUtils {
    /**
     * Load properties from string.
     * Extracted for sanitize JRE dependency.
     * 
     * @param properties
     * @return
     * @throws IOException
     */
    @IgnoreJRERequirement
    public static Properties loadProperties(String properties) throws IOException {
        Properties p = new Properties();
        try {
            p.load(new StringReader(properties));
        } catch(NoSuchMethodError _) {
            // {@link Properties#load(java.io.Reader)} is supported since Java 1.6
            // When used with Java1.5, fall back to
            // {@link Properties#load(java.io.InputStream)}, which does not support
            // Non-ascii strings.
            p.load(new StringInputStream(properties));
        }
        return p;
    }
    
    /**
     * {@link FilePath#readToString()} with encoding.
     * 
     * @param f file to read
     * @param encoding null for platform default encoding.
     * @return read string
     * @throws IOException
     */
    public static String readFileToString(FilePath f, String encoding) throws IOException {
        InputStream in = f.read();
        try {
            return IOUtils.toString(in, encoding);
        } finally {
            in.close();
        }
    }
    
    public static boolean isSupportNonAsciiPropertiesFile() {
        try {
            // Is {@link Properties#load(java.io.Reader)} supported?
            Properties.class.getMethod("load", Reader.class);
        } catch(NoSuchMethodException _) {
            return false;
        }
        return true;
    }

}
