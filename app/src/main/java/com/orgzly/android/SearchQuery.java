package com.orgzly.android;

import com.orgzly.android.util.QuotedStringTokenizer;
import com.orgzly.org.datetime.OrgInterval;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * Inspired by:
 *
 * Advanced searching (org-mode)
 * http://orgmode.org/worg/org-tutorials/advanced-searching.html
 *
 * Advanced search (GMail)
 * https://support.google.com/mail/answer/7190?hl=en
 *
 * Using . as a separator as it's available without
 * using a modifier key on most keyboards.
 *
 */

public class SearchQuery {

    // A "group" of search criteria that is AND-ed together when searching.
    public class SearchQueryGroup {
        private SearchQueryInterval scheduled;
        private SearchQueryInterval deadline;
        private Set<String> textSearch = new LinkedHashSet<>();
        private String bookName;
        private Set<String> notBookName = new TreeSet<>();
        private Set<String> noteTags = new TreeSet<>();
        private Set<String> tags = new TreeSet<>();
        private Set<String> notTags = new TreeSet<>();
        private String priority;
        private String notPriority;
        private String state;
        private Set<String> notState = new TreeSet<>();
        private String stateType;
        private String notStateType;


        public boolean hasScheduled() {
            return scheduled != null;
        }

        public SearchQueryInterval getScheduled() {
            return scheduled;
        }

        public boolean hasDeadline() {
            return deadline != null;
        }

        public SearchQueryInterval getDeadline() {
            return deadline;
        }

        public Set<String> getTextSearch() {
            return textSearch;
        }

        public boolean hasTextSearch() {
            return !textSearch.isEmpty();
        }

        public Set<String> getNoteTags() {
            return noteTags;
        }

        public boolean hasNoteTags() {
            return !noteTags.isEmpty();
        }

        public Set<String> getTags() {
            return tags;
        }

        public boolean hasTags() {
            return !tags.isEmpty();
        }

        public Set<String> getNotTags() {
            return notTags;
        }

        /* Priority. */

        public String getPriority() {
            return priority;
        }

        public boolean hasPriority() {
            return priority != null;
        }

        public String getNotPriority() {
            return notPriority;
        }

        public boolean hasNotPriority() {
            return notPriority != null;
        }

        /* State. */

        public String getState() {
            return state;
        }

        public boolean hasState() {
            return state != null;
        }

        public Set<String> getNotState() {
            return notState;
        }

        public boolean hasNotState() {
            return ! notState.isEmpty();
        }

        /* State type. */

        public String getStateType() {
            return stateType;
        }

        public boolean hasStateType() {
            return stateType != null;
        }

        public String getNotStateType() {
            return notStateType;
        }

        public boolean hasNotStateType() {
            return notStateType != null;
        }

        /* Book name */

        public String getBookName() {
            return bookName;
        }

        public void setBookName(String bookName) {
            this.bookName = bookName;
        }

        public boolean hasBookName() {
            return bookName != null;
        }

        public Set<String> getNotBookName() {
            return notBookName;
        }

        public boolean hasNotBookName() {
            return ! notBookName.isEmpty();
        }


        public String toString() {
            StringBuilder s = new StringBuilder();

            if (hasBookName()) {
                s.append("b.").append(QuotedStringTokenizer.quote(bookName, " "));
            }

            if (hasNotBookName()) {
                for (String name : notBookName) {
                    s.append(" .b.").append(QuotedStringTokenizer.quote(name, " "));
                }
            }

            if (stateType != null) {
                s.append(" it.").append(stateType.toLowerCase());
            }

            if (notStateType != null) {
                s.append(" .it.").append(notStateType.toLowerCase());
            }

            if (state != null) {
                s.append(" i.").append(state.toLowerCase());
            }

            if (! notState.isEmpty()) {
                for (String state : notState) {
                    s.append(" .i.").append(state.toLowerCase());
                }
            }

            if (priority != null) {
                s.append(" p.").append(priority.toLowerCase());
            }

            if (notPriority != null) {
                s.append(" .p.").append(notPriority.toLowerCase());
            }

            if (! noteTags.isEmpty()) {
                for (String tag: noteTags) {
                    s.append(" tn.").append(tag);
                }
            }

            if (! notTags.isEmpty()) {
                for (String tag : notTags) {
                    s.append(" .t.").append(tag);
                }
            }

            if (! tags.isEmpty()) {
                for (String tag: tags) {
                    s.append(" t.").append(tag);
                }
            }

            if (hasScheduled()) {
                s.append(" s.").append(scheduled.toString());
            }

            if (hasDeadline()) {
                s.append(" d.").append(deadline.toString());
            }

            for (String ts: textSearch) {
                s.append(" ").append(ts);
            }

            return s.toString().trim();
        }
    }

    public static class SortOrder {
        public enum Type {
            SCHEDULED,
            DEADLINE,
            NOTEBOOK,
            PRIORITY
        }

        private String name;
        private boolean isAscending;

        SortOrder(String name, boolean isAscending) {
            this.name = name;
            this.isAscending = isAscending;
        }

        public boolean isAscending() {
            return isAscending;
        }

        public boolean isDescending() {
            return !isAscending;
        }

        public Type getType() {
            if ("scheduled".equals(name) || "sched".equals(name) || "s".equals(name)) {
                return Type.SCHEDULED;
            } else if ("deadline".equals(name) || "dead".equals(name) || "d".equals(name)) {
                return Type.DEADLINE;
            } else if ("priority".equals(name) || "prio".equals(name) || "pri".equals(name) || "p".equals(name)) {
                return Type.PRIORITY;
            } else if ("notebook".equals(name) || "book".equals(name) || "b".equals(name)) {
                return Type.NOTEBOOK;
            } else {
                return null;
            }
        }
    }

    /**
     * {@link OrgInterval} with support for today (0d) and tomorrow (1d).
     *
     * TODO: Cleanup. For example tomorrow, tmws, tom should be constants (set) etc.
     */
    public static class SearchQueryInterval extends OrgInterval {
        private static final Pattern PATTERN = Pattern.compile("^(\\d+)(h|d|w|m|y)$");

        private boolean none = false;

        public static SearchQueryInterval getInstance(String str) {
            SearchQueryInterval interval = null;

            if (str != null) {
                str = str.toLowerCase();

                if ("none".equals(str) || "no".equals(str)) {
                    interval = new SearchQueryInterval();
                    interval.none = true;
                } else if ("today".equals(str) || "tod".equals(str)) {
                    interval = new SearchQueryInterval();
                    interval.setValue(0);
                    interval.setUnit(Unit.DAY);

                } else if ("tomorrow".equals(str) || "tmrw".equals(str) || "tom".equals(str)) {
                    interval = new SearchQueryInterval();
                    interval.setValue(1);
                    interval.setUnit(Unit.DAY);

                } else {
                    Matcher m = PATTERN.matcher(str);

                    if (m.find()) {
                        interval = new SearchQueryInterval();
                        interval.setValue(m.group(1));
                        interval.setUnit(m.group(2));

                    } else {
                        return null;
                    }
                }
            }

            return interval;
        }

        public String toString() {
            if (none()) {
                return "none";
            } else if (unit == Unit.DAY && value == 0) {
                return "today";

            } else if (unit == Unit.DAY && value == 1) {
                return "tomorrow";
            }

            return super.toString();
        }

        public boolean none() {
            return none;
        }
    }

    // The most recent group added. (If there is no "or" in the search, this
    // will be the only group.) Adding this field should make it simpler to
    // integrate this feature into the existing code.
    public SearchQueryGroup currentGroup = new SearchQueryGroup();
    // A list of grouped search criteria. The criteria inside a group is AND-ed
    // together. The different groups are all OR-ed together.
    public List<SearchQueryGroup> groups = new ArrayList<>(Arrays.asList(currentGroup));

    private List<SortOrder> sortOrder = new ArrayList<>();

    public SearchQuery() {
    }

    public SearchQuery(String str) {
        if (str == null) {
            str = "";
        }

        QuotedStringTokenizer tokenizer = new QuotedStringTokenizer(str, " ", false, true);

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();

            if (token.startsWith("s.")) { // scheduled
                if (token.length() > 2) {
                    currentGroup.scheduled = SearchQueryInterval.getInstance(token.substring(2).toLowerCase());
                }

            } else if (token.startsWith("d.")) { // deadline
                if (token.length() > 2) {
                    currentGroup.deadline = SearchQueryInterval.getInstance(token.substring(2).toLowerCase());
                }

            } else if (token.startsWith("p.")) { // priority
                if (token.length() > 2) {
                    currentGroup.priority = token.substring(2).toLowerCase();
                }

            } else if (token.startsWith(".p.")) { // not priority
                if (token.length() > 3) {
                    currentGroup.notPriority = token.substring(3).toLowerCase();
                }

            } else if (token.startsWith("i.")) { // is (state)
                if (token.length() > 2) {
                    currentGroup.state = token.substring(2).toUpperCase();
                }

            } else if (token.startsWith(".i.")) { // is not (state)
                if (token.length() > 3) {
                    currentGroup.notState.add(token.substring(3).toUpperCase());
                }

            } else if (token.startsWith("it.")) { // is state type
                if (token.length() > 3) {
                    currentGroup.stateType = token.substring(3).toUpperCase();
                }

            } else if (token.startsWith(".it.")) { // is not state type
                if (token.length() > 4) {
                    currentGroup.notStateType = token.substring(4).toUpperCase();
                }

            } else if (token.startsWith("b.")) { // book name
                if (token.length() > 2) {
                    currentGroup.bookName = QuotedStringTokenizer.unquote(token.substring(2));
                }

            } else if (token.startsWith(".b.")) { // book name
                if (token.length() > 3) {
                    currentGroup.notBookName.add(QuotedStringTokenizer.unquote(token.substring(3)));
                }

            } else if (token.startsWith("tn.")) { // note tag
                if (token.length() > 3) {
                    currentGroup.noteTags.add(token.substring(3));
                }

            } else if (token.startsWith(".t.")) { // has no tag
                if (token.length() > 3) {
                    currentGroup.notTags.add(token.substring(3));
                }

            } else if (token.startsWith("t.")) { // tag
                if (token.length() > 2) {
                    currentGroup.tags.add(token.substring(2));
                }

            } else if (token.startsWith(".o.")) { // sort order
                if (token.length() > 3) {
                    String sortOrderName = token.substring(3);
                    sortOrder.add(new SortOrder(sortOrderName, false));
                }

            } else if (token.startsWith("o.")) { // sort order
                if (token.length() > 2) {
                    String sortOrderName = token.substring(2);
                    sortOrder.add(new SortOrder(sortOrderName, true));
                }

            } else if (token.equals("or")) {
                currentGroup = new SearchQueryGroup();
                groups.add(currentGroup);

            } else {
                currentGroup.textSearch.add(token);
            }
        }
    }

    public List<SortOrder> getSortOrder() {
        return sortOrder;
    }

    public boolean hasSortOrder() {
        return sortOrder.size() > 0;
    }


    public String toString() {
        List<String> groupStrs = new ArrayList<>();

        for (SearchQueryGroup qg : groups) {
            groupStrs.add(qg.toString());
        }

        String groupStr = TextUtils.join(" or ", groupStrs);

        for (SortOrder order: sortOrder) {
            StringBuilder s = new StringBuilder();
            s.append(" ");

            if (! order.isAscending) {
                s.append(".");
            }

            s.append("o.").append(order.name);
            groupStr += s.toString();
        }

        return groupStr;
    }
}
