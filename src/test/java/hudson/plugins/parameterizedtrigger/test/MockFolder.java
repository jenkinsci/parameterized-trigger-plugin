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

package hudson.plugins.parameterizedtrigger.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import jenkins.model.Jenkins;
import jenkins.model.ModifiableTopLevelItemGroup;

import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.model.ItemGroupMixIn;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.AbstractItem;
import hudson.model.Job;
import hudson.model.listeners.ItemListener;

/**
 * Replace this to org.jvnet.hudson.test.MockFolder
 * when the target will get Jenkins >= 1.494.
 */
public class MockFolder extends AbstractItem implements ModifiableTopLevelItemGroup, TopLevelItem {
    private List<TopLevelItem> itemList = new ArrayList<TopLevelItem>();
    private ItemGroupMixIn mixin = new ItemGroupMixIn(this, this) {
        @Override
        protected void add(TopLevelItem item) {
            getItems().add(item);
        }
        
        @Override
        protected File getRootDirFor(String name) {
            return MockFolder.this.getRootDirFor(name);
        }
    };

    @SuppressWarnings("rawtypes")
    protected MockFolder(ItemGroup parent, String name) {
        super(parent, name);
    }

    @Override
    public TopLevelItem doCreateItem(StaplerRequest req, StaplerResponse rsp)
            throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<TopLevelItem> getItems() {
        return itemList;
    }

    @Override
    public String getUrlChildPrefix() {
        return "job";
    }

    @Override
    public TopLevelItem getItem(String name) {
        if (name == null) {
            return null;
        }
        for (TopLevelItem item: getItems()) {
            if (name.equals(item.getName())) {
                return item;
            }
        }
        return null;
    }

    @Override
    public File getRootDirFor(TopLevelItem child) {
        return getRootDirFor(child.getName());
    }

    protected File getRootDirFor(String name) {
        return new File(new File(getRootDir(), "jobs"), name);
    }

    @Override
    public void onRenamed(TopLevelItem item, String oldName, String newName)
            throws IOException {
        // nothing to do.
    }

    @Override
    public void onDeleted(TopLevelItem item) throws IOException {
        for (ItemListener l: ItemListener.all()) {
            l.onDeleted(item);
        }
        getItems().remove(item);
    }

    @Override
    public TopLevelItemDescriptor getDescriptor() {
        return (TopLevelItemDescriptor)Jenkins.getInstance().getDescriptor(MockFolder.class);
    }

    @Override
    public <T extends TopLevelItem> T copy(T src, String name)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TopLevelItem createProjectFromXML(String name, InputStream xml)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TopLevelItem createProject(TopLevelItemDescriptor type, String name,
            boolean notify) throws IOException {
        return mixin.createProject(type, name, notify);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Collection<? extends Job> getAllJobs() {
        Set<Job> jobs = new HashSet<Job>();
        for (TopLevelItem item: getItems()) {
            jobs.addAll(item.getAllJobs());
        }
        return jobs;
    }
    
    @Extension
    public static class DescriptorImpl extends TopLevelItemDescriptor {

        @Override
        public String getDisplayName() {
            return "MockFolder";
        }

        @SuppressWarnings("rawtypes")
        @Override
        public TopLevelItem newInstance(ItemGroup parent, String name) {
            return new MockFolder(parent, name);
        }
    }
}
