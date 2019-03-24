package com.orgzly.android.util;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class AgendaUtilsTest {
    private String rangeStr;
    private int days;
    private List<DateTime> dates;
    private boolean overdueToday;

    // now is 2017 May 5, 13:00:00
    private GregorianCalendar now = new GregorianCalendar(2017, Calendar.MAY, 5, 13, 0);

    public AgendaUtilsTest(String rangeStr, int days, boolean overdueToday, List<DateTime> dates) {
        this.rangeStr = rangeStr;
        this.days = days;
        this.overdueToday = overdueToday;
        this.dates = dates;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "<2017-05-03 Wed>--<2017-05-11 Do>", 2, true, Arrays.asList(
                        new DateTime(2017, 5, 5, 0, 0),
                        new DateTime(2017, 5, 6, 0, 0))
                },
                { "<2017-05-03 Wed>--<2017-05-11 Do>", 1, true, Arrays.asList(
                        new DateTime(2017, 5, 5, 0, 0))
                },
                { "<2017-05-06 Sat>--<2017-05-08 Mon>", 10, true, Arrays.asList(
                        new DateTime(2017, 5, 6, 0, 0),
                        new DateTime(2017, 5, 7, 0, 0),
                        new DateTime(2017, 5, 8, 0, 0))
                },
                { "<2017-05-02 Tue ++3d>", 5, true, Arrays.asList(
                        new DateTime(2017, 5, 5, 0, 0),
                        new DateTime(2017, 5, 8, 0, 0))
                },
                { "<2017-05-04 Do>", 5, true, Arrays.asList(
                        new DateTime(2017, 5, 5, 0, 0))
                },
                { "<2017-05-05 Do>", 5, true, Arrays.asList(
                        new DateTime(2017, 5, 5, 0, 0))
                },
                { "<2017-05-06 Do>", 5, true, Arrays.asList(
                        new DateTime(2017, 5, 6, 0, 0))
                },
                { "<2017-05-03 Wed 09:00 ++12h>", 2, true, Arrays.asList(
                        new DateTime(2017, 5, 5, 0, 0),  // overdue
                        new DateTime(2017, 5, 5, 21, 0),
                        new DateTime(2017, 5, 6, 9, 0),
                        new DateTime(2017, 5, 6, 21, 0))
                },
                { "<2017-05-05 Fri 09:00 ++12h>", 2, true, Arrays.asList(
                        new DateTime(2017, 5, 5, 0, 0), // overdue
                        new DateTime(2017, 5, 5, 21, 0),
                        new DateTime(2017, 5, 6, 9, 0),
                        new DateTime(2017, 5, 6, 21, 0))
                },
                { "<2017-05-07 Sun 09:00 ++6h>", 4, true, Arrays.asList(
                        new DateTime(2017, 5, 7, 9, 0),
                        new DateTime(2017, 5, 7, 15, 0),
                        new DateTime(2017, 5, 7, 21, 0),
                        new DateTime(2017, 5, 8, 3, 0),
                        new DateTime(2017, 5, 8, 9, 0),
                        new DateTime(2017, 5, 8, 15, 0),
                        new DateTime(2017, 5, 8, 21, 0))
                },
                { "<2017-05-03 Wed 09:00 .+12h>", 2, true, Arrays.asList(
                        new DateTime(2017, 5, 5, 0, 0),  // overdue
                        new DateTime(2017, 5, 5, 21, 0),
                        new DateTime(2017, 5, 6, 9, 0),
                        new DateTime(2017, 5, 6, 21, 0))
                },
                { "<2017-05-03 Wed 09:00 +12h>", 3, true, Arrays.asList(
                        new DateTime(2017, 5, 5, 0, 0),  // overdue
                        new DateTime(2017, 5, 5, 21, 0),
                        new DateTime(2017, 5, 6, 9, 0),
                        new DateTime(2017, 5, 6, 21, 0),
                        new DateTime(2017, 5, 7, 9, 0),
                        new DateTime(2017, 5, 7, 21, 0))
                },
                { "<2017-05-08 Mon 09:00 +12h>", 5, true, Arrays.asList(
                        new DateTime(2017, 5, 8, 9, 0),
                        new DateTime(2017, 5, 8, 21, 0),
                        new DateTime(2017, 5, 9, 9, 0),
                        new DateTime(2017, 5, 9, 21, 0))
                },
                { "<2017-05-06 Sat +1w>", 5, true, Arrays.asList(
                        new DateTime(2017, 5, 6, 0, 0))
                },
                { "<2017-05-06 Sat +1w>", 10, true, Arrays.asList(
                        new DateTime(2017, 5, 6, 0, 0),
                        new DateTime(2017, 5, 13, 0, 0))
                },
                { "<2017-05-04 Sat +1d>", 3, false, Arrays.asList(
                        new DateTime(2017, 5, 5, 0, 0),
                        new DateTime(2017, 5, 6, 0, 0),
                        new DateTime(2017, 5, 7, 0, 0))
                }
        });
    }

    @Test
    public void testExpander() {
        List<DateTime> expandedDates = AgendaUtils.expandOrgDateTime(rangeStr, now, days, overdueToday);
        assertEquals(dates.size(), expandedDates.size());
        assertThat(toStringArray(expandedDates), is(toStringArray(dates)));
    }

    private List<String> toStringArray(List<DateTime> times) {
        List<String> result = new ArrayList<>();

        for (DateTime time: times) {
            result.add(time.toString());
        }

        return result;
    }
}
