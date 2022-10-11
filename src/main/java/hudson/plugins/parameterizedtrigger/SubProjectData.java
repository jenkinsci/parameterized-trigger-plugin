/*
 * The MIT License
 *
 * Copyright (c) 2013 Sony Mobile Communications AB. All rights reserved.
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import hudson.model.Job;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * A class intended to store sub project depending on their relation to the project. It is used when returning project
 * information to be displayed under Subprojects in job view.<br>
 * <br>
 * Available sets are: <br>
 * Fixed - statically defined sub projects
 * Dynamic - dynamically defined sub projects
 * Triggered - sub projects recent triggered by executed builds
 * Unresolved - sub projects not defined or unresolved
 *
 * @author JO Sivtoft
 */
@SuppressFBWarnings(value="SIC_INNER_SHOULD_BE_STATIC_ANON")
public class SubProjectData {

    private final Comparator<Job> customComparator = new Comparator<Job>() {
        public int compare(Job job1, Job job2) {
            return job1.getFullName().compareTo(job2.getFullName());
        }
    };

    private final Set<Job> dynamic = new TreeSet<>(customComparator);
    private final Set<Job> fixed = new TreeSet<>(customComparator);
    private final Set<Job> triggered = new TreeSet<>(customComparator);
    private final Set<String> unresolved = new TreeSet<>();

    /**
     * A set intended to hold dynamically configured sub project
     *
     * @return  A set reserved for dynamically configured sub project
     */
    public Set<Job> getDynamic() {
        return dynamic;
    }

    /**
     * A set intended to hold fixed configured sub project
     *
     * @return  A set reserved for fixed configured sub project
     */
    public Set<Job> getFixed() {
        return fixed;
    }

    /**
     * A set intended to hold triggered sub project
     *
     * @return  A set reserved for triggered sub project
     */
    public Set<Job> getTriggered() {
        return triggered;
    }

    /**
     * A set intended to hold unresolved sub project
     *
     * @return  A set reserved for unresolved sub project
     */
    public Set<String> getUnresolved() {
        return unresolved;
    }
}
