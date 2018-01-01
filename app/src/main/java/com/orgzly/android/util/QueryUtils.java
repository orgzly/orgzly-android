package com.orgzly.android.util;

import com.orgzly.android.query.Condition;

public class QueryUtils {
    public static String extractFirstBookNameFromQuery(Condition condition) {
        if (condition instanceof Condition.And) {
            Condition.And c = (Condition.And) condition;

            for (Condition innerCondition : c.getOperands()) {
                String result = extractFirstBookNameFromQuery(innerCondition);

                if (result != null) {
                    return result;
                }
            }
        }

        if (condition instanceof Condition.InBook) {
            Condition.InBook c = (Condition.InBook) condition;

            if (!c.getNot()) {
                return c.getName();
            }
        }

        return null;
    }
}
