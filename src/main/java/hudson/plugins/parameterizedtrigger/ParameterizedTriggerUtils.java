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
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Properties;
import jenkins.util.VirtualFile;

/**
 * Common utility methods.
 */
public class ParameterizedTriggerUtils {
    /**
     * Load properties from string.
     *
     * @throws IOException
     */
    public static Properties loadProperties(String properties) throws IOException {
        Properties p = new Properties();
        p.load(new StringReader(properties));
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
    public static String readFileToString(FilePath f, String encoding) throws IOException, InterruptedException {
        InputStream in = f.read();
        try {
            return IOUtils.toString(in, encoding);
        } finally {
            in.close();
        }
    }
    
    /**
     * {@link} read VirtualFile
     * 
     * @param f file to read
     * @return read string
     * @throws IOException
     */
    public static String readFileToString(VirtualFile f) throws IOException, InterruptedException {
        InputStream in = f.open();
        try {
            return IOUtils.toString(in);
        } finally {
            in.close();
        }
    }
    
    public static ParametersAction mergeParameters(ParametersAction base, ParametersAction overlay) {
        LinkedHashMap<String,ParameterValue> params = new LinkedHashMap<String,ParameterValue>();
        for (ParameterValue param : base.getParameters())
            params.put(param.getName(), param);
        for (ParameterValue param : overlay.getParameters())
            params.put(param.getName(), param);
        return new ParametersAction(params.values().toArray(new ParameterValue[params.size()]));
    }

}
