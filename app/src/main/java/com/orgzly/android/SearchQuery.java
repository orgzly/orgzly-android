package com.orgzly.android;

import com.orgzly.android.util.QuotedStringTokenizer;
import com.orgzly.org.datetime.OrgInterval;

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
    private SearchQueryInterval scheduled;
    private SearchQueryInterval deadline;
    private Set<String> textSearch = new LinkedHashSet<>();
    private String bookName;
    private Set<String> notBookName = new TreeSet<>();
    private Set<String> noteTags = new TreeSet<>();
    private Set<String> tags = new TreeSet<>();
    private Set<String> notTags = new TreeSet<>();
    private String priority;
    private String state;
    private Set<String> notState = new TreeSet<>();

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
                    scheduled = SearchQueryInterval.getInstance(token.substring(2).toLowerCase());
                }

            } else if (token.startsWith("d.")) { // deadline
                if (token.length() > 2) {
                    deadline = SearchQueryInterval.getInstance(token.substring(2).toLowerCase());
                }

            } else if (token.startsWith("p.")) { // priority
                if (token.length() > 2) {
                    priority = token.substring(2).toLowerCase();
                }

            } else if (token.startsWith("i.")) { // is (state)
                if (token.length() > 2) {
                    state = token.substring(2).toUpperCase();
                }

            } else if (token.startsWith(".i.")) { // is not (state)
                if (token.length() > 3) {
                    notState.add(token.substring(3).toUpperCase());
                }

            } else if (token.startsWith("b.")) { // book name
                if (token.length() > 2) {
                    bookName = QuotedStringTokenizer.unquote(token.substring(2));
                }

            } else if (token.startsWith(".b.")) { // book name
                if (token.length() > 3) {
                    notBookName.add(QuotedStringTokenizer.unquote(token.substring(3)));
                }

            } else if (token.startsWith("tn.")) { // note tag
                if (token.length() > 3) {
                    noteTags.add(token.substring(3));
                }

            } else if (token.startsWith(".t.")) { // has no tag
                if (token.length() > 3) {
                    notTags.add(token.substring(3));
                }

            } else if (token.startsWith("t.")) { // tag
                if (token.length() > 2) {
                    tags.add(token.substring(2));
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

            } else {
                textSearch.add(token);
            }
        }
    }

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

    public String getPriority() {
        return priority;
    }

    /**
     * Lowercase priority.
     */
    public boolean hasPriority() {
        return priority != null;
    }

    public String getState() {
        return state;
    }

    public boolean hasState() {
        return state != null;
    }

    public void setState(String value) {
        state = value;
    }

    public Set<String> getNotState() {
        return notState;
    }

    public boolean hasNotState() {
        return ! notState.isEmpty();
    }

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


    public boolean hasSortOrder() {
        return sortOrder.size() > 0;
    }

    public List<SortOrder> getSortOrder() {
        return sortOrder;
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

        for (SortOrder order: sortOrder) {
            s.append(" ");

            if (! order.isAscending) {
                s.append(".");
            }

            s.append("o.").append(order.name);
        }

        return s.toString().trim();
    }

    public static class SortOrder {
        public enum Type {
            SCHEDULED,
            DEADLINE,
            CREATED,
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
            } else if ("created".equals(name) || "create".equals(name) || "c".equals(name)) {
                return Type.CREATED;
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
        public static final Pattern PATTERN = Pattern.compile("^(\\d+)(h|d|w|m|y)$");

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
}
