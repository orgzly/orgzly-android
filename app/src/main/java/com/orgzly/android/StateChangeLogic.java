package com.orgzly.android;

import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgRange;

import java.util.Set;

/**
 * State setting logic.
 */
public class StateChangeLogic {
    final private Set<String> todoKeywords;
    final private Set<String> doneKeywords;

    private String state;

    private OrgRange scheduled;
    private OrgRange deadline;
    private OrgRange closed;

    public StateChangeLogic(Set<String> todoKeywords, Set<String> doneKeywords) {
        this.todoKeywords = todoKeywords;
        this.doneKeywords = doneKeywords;
    }

    public void setState(String targetState, String originalState, OrgRange scheduledTime, OrgRange deadlineTime) {
        if (targetState == null) {
            throw new IllegalArgumentException("Target state cannot be null");
        }

        this.scheduled = scheduledTime;
        this.deadline = deadlineTime;

        if (doneKeywords.contains(targetState)) { /* Target state is a done state. */
            if (todoKeywords.contains(originalState)) {
                boolean shifted = false;

                if (scheduled != null) {
                    if (scheduled.shift()) {
                        shifted = true;
                    }
                }
                if (deadline != null) {
                    if (deadline.shift()) {
                        shifted = true;
                    }
                }

                if (shifted) {
                    /* Keep the original state and remove the closed time */
                    state = originalState;
                    closed = null;
                } else {
                    /* Set state and closed time. */
                    state = targetState;
                    closed = OrgRange.getInstance(OrgDateTime.getInstance(false));
                }

            } else {
                /* Set state and update closed time. */
                state = targetState;
                closed = OrgRange.getInstance(OrgDateTime.getInstance(false));
            }

        } else { /* Target keyword is a to-do state.
            /* Set state and remove closed time. */
            state = targetState;
            closed = null;
        }
    }

    public String getState() {
        return state;
    }

    public OrgRange getScheduled() {
        return scheduled;
    }

    public OrgRange getDeadline() {
        return deadline;
    }

    public OrgRange getClosed() {
        return closed;
    }
}