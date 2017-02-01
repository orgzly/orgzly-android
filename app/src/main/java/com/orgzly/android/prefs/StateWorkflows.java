package com.orgzly.android.prefs;

import com.orgzly.org.OrgStatesWorkflow;
import com.orgzly.org.OrgStringUtils;

import java.util.ArrayList;

/**
 * List of {@link OrgStatesWorkflow}s
 */
public class StateWorkflows extends ArrayList<OrgStatesWorkflow> {
    public StateWorkflows(String s) {
        if (s == null) {
            return;
        }

        String st = s.trim();

        if (st.length() == 0) {
            return;
        }

        for (String line: st.split("\n+")) {
            add(new OrgStatesWorkflow(line));
        }
    }

    public String toString() {
        return OrgStringUtils.join(this, "\n");
    }
}
