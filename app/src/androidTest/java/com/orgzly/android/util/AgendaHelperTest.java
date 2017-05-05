package com.orgzly.android.util;

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
                        new GregorianCalendar(2017, Calendar.MAY, 5).getTime(),
                        new GregorianCalendar(2017, Calendar.MAY, 6).getTime())
                }
        });
    }

    @Test
    public void testExpander() {
        List<Date> expandedDates = AgendaHelper.expandOrgRange(rangeStr, now, days);
        Assert.assertEquals(dates.size(), expandedDates.size());
        Assert.assertThat(expandedDates, containsInAnyOrder(dates.toArray()));
    }
}
