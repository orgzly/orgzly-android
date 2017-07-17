package com.orgzly.android.util;

import com.orgzly.org.OrgStringUtils;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.*;


/**
 * Created by pxsalehi on 04.05.17.
 */

@RunWith(Parameterized.class)
public class AgendaHelperTest {
    private String rangeStr;
    private int days;
    private List<Date> dates;
    // now is 2017 May 5, 13:00:00
    private GregorianCalendar now = new GregorianCalendar(2017, Calendar.MAY, 5, 13, 0);

    public AgendaHelperTest(String rangeStr, int days, List<Date> dates) {
        this.rangeStr = rangeStr;
        this.days = days;
        this.dates = dates;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "<2017-05-03 Wed>--<2017-05-11 Do>", 2, Arrays.asList(
                        new DateTime(2017, 5, 5, 0, 0),
                        new DateTime(2017, 5, 6, 0, 0))
                },
                { "<2017-05-03 Wed>--<2017-05-11 Do>", 1, Arrays.asList(
                        new DateTime(2017, 5, 5, 0, 0))
                },
                { "<2017-05-06 Sat>--<2017-05-08 Mon>", 10, Arrays.asList(
                        new DateTime(2017, 5, 6, 0, 0),
                        new DateTime(2017, 5, 7, 0, 0),
                        new DateTime(2017, 5, 8, 0, 0))
                },
                { "<2017-05-02 Tue ++3d>", 5, Arrays.asList(
                        new DateTime(2017, 5, 5, 0, 0),
                        new DateTime(2017, 5, 8, 0, 0))
                },
                { "<2017-05-04 Do>", 5, Arrays.asList(
                        new DateTime(2017, 5, 5, 0, 0))
                },
                { "<2017-05-05 Do>", 5, Arrays.asList(
                        new DateTime(2017, 5, 5, 0, 0))
                },
                { "<2017-05-06 Do>", 5, Arrays.asList(
                        new DateTime(2017, 5, 6, 0, 0))
                },
                { "<2017-05-03 Wed 09:00 ++12h>", 2, Arrays.asList(
                        new DateTime(2017, 5, 5, 0, 0),
                        new DateTime(2017, 5, 5, 21, 0, 0, 0),
                        new DateTime(2017, 5, 6, 9, 0, 0, 0),
                        new DateTime(2017, 5, 6, 21, 0, 0, 0))
                },
//                { "<2017-05-05 Fri 09:00 ++12h>", 2, Arrays.asList(
//                        new DateTime(2017, 5, 5, 0, 0),
//                        new DateTime(2017, 5, 6, 0, 0),
//                        new DateTime(2017, 5, 7, 0, 0),
//                        new DateTime(2017, 5, 8, 0, 0),
//                        new DateTime(2017, 5, 9, 0, 0))
//                },
//                { "<2017-05-07 Sun 09:00 ++6h>", 2, Arrays.asList(
//                        new DateTime(2017, 5, 7, 0, 0),
//                        new DateTime(2017, 5, 8, 0, 0),
//                        new DateTime(2017, 5, 9, 0, 0))
//                },
//                { "<2017-05-03 Wed 09:00 .+12h>", 2, Arrays.asList(
//                        new DateTime(2017, 5, 5, 0, 0),
//                        new DateTime(2017, 5, 6, 0, 0),
//                        new DateTime(2017, 5, 7, 0, 0),
//                        new DateTime(2017, 5, 8, 0, 0),
//                        new DateTime(2017, 5, 9, 0, 0))
//                },
//                { "<2017-05-03 Wed 09:00 +12h>", 2, Arrays.asList(
//                        new DateTime(2017, 5, 5, 0, 0),
//                        new DateTime(2017, 5, 6, 0, 0),
//                        new DateTime(2017, 5, 7, 0, 0),
//                        new DateTime(2017, 5, 8, 0, 0),
//                        new DateTime(2017, 5, 9, 0, 0))
//                },
//                { "<2017-05-08 Mon 09:00 +12h>", 2, Arrays.asList(
//                        new DateTime(2017, 5, 8, 0, 0),
//                        new DateTime(2017, 5, 9, 0, 0))
//                },
                { "<2017-05-06 Sat +1w>", 5, Arrays.asList(
                        new DateTime(2017, 5, 6, 0, 0))
                },
                { "<2017-05-06 Sat +1w>", 10, Arrays.asList(
                        new DateTime(2017, 5, 6, 0, 0),
                        new DateTime(2017, 5, 13, 0, 0))
                }
        });
    }

    @Test
    public void testExpander() {
        List<DateTime> expandedDates = AgendaHelper.expandOrgDateTime(rangeStr, now, days);
        Assert.assertEquals(dates.size(), expandedDates.size());
        Assert.assertThat(expandedDates, containsInAnyOrder(dates.toArray()));
    }
}
