/*
 * The MIT License
 *
 * Copyright (c) 2013, Chris Johnson
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

import hudson.Util;
import hudson.model.Action;
import hudson.model.BuildBadgeAction;
import hudson.model.InvisibleAction;
import hudson.model.Label;
import hudson.model.Queue;
import hudson.model.labels.LabelAssignmentAction;
import hudson.model.queue.SubTask;
import java.util.List;

/**
 * {@link Action} that restricts the job to a particular node 
 * when a project is scheduled
 * Will cause a unique build for each different node if a job is already queued.
 * 
 * @author Chris Johnson
 */
public class NodeAction extends InvisibleAction implements LabelAssignmentAction, Queue.QueueAction, BuildBadgeAction {
	private final String nodename;
	
	public NodeAction(String nodename) {
		this.nodename = nodename;
	}

	public Label getAssignedLabel(SubTask task) {
		return Label.get(nodename);
	}
	
	public boolean shouldSchedule(List<Action> actions) {
		// see if there is already a matching action with same node
		for (NodeAction other:Util.filter(actions, NodeAction.class)) {
			if(this.nodename.equals(other.nodename)){
				// there is already a task for this node.
				return false;
			}
		}
		return true;
	}
	/**
	 * @return the tooltip
	 */
	public String getTooltip() {
		return nodename;
	}
}
