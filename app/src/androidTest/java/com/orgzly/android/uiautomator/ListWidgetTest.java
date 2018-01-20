package com.orgzly.android.uiautomator;

import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests the ListWidget with the UI-Automator framework.
 * Tested on an Nexus 5 Emulator with Android 25
 * Must have an instance of the ListWidget on the Homescreen, when starting this test.
 * Also the default version of Getting Started with Orgzly must be the only notebook.
 *
 * See https://developer.android.com/training/testing/ui-testing/uiautomator-testing.html
 */
@Ignore
public class ListWidgetTest {

    private static final String HEADER_FILTER = "com.orgzly:id/list_widget_header_filter";
    private static final String HEADER_ICON = "com.orgzly:id/list_widget_header_icon";
    private static final String HEADER_ADD = "com.orgzly:id/list_widget_header_add";
    private static final String ITEM_DONE = "com.orgzly:id/item_list_widget_done";
    private static final String ITEM_TITLE = "com.orgzly:id/item_list_widget_title";
    private static final String ORGZLY_SEARCH = "com.orgzly:id/activity_action_search";
    private UiDevice device;

    @Before
    public void addWidget() throws Exception {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        device.pressHome();
        device.pressHome();
    }

    @Test
    public void testSelectFilterAndMarkDone() throws Exception{
        findObject(By.res(HEADER_FILTER)).click();
        findObject(By.text("i.todo")).click();

        assertThat(findObject(By.res(HEADER_FILTER)).getText(), is("To Do"));

        List<UiObject2> doneButtons = findObjects(By.res(ITEM_DONE));
        doneButtons.get(0).click();
        SystemClock.sleep(100);
        assertThat(findObjects(By.res(ITEM_DONE)).size(), is(doneButtons.size() - 1));
    }

    @Test
    public void testAddButton() throws Exception{
        findObject(By.res(HEADER_ADD)).click();

        assertThat(findObject(By.text("New note")), notNullValue());
    }

    @Test
    public void openNote() throws Exception {
        findObject(By.res(ITEM_TITLE)).click();

        assertThat(findObject(By.text("Getting Started with Orgzly")), notNullValue());
    }

    @Test
    public void openOrgzly() throws Exception {
        findObject(By.res(HEADER_ICON)).click();

        assertThat(findObject(By.res(ORGZLY_SEARCH)), notNullValue());
    }

    private UiObject2 findObject(BySelector by) {
        return device.wait(Until.findObject(by), 1000);
    }
    private List<UiObject2> findObjects(BySelector by) {
        return device.wait(Until.findObjects(by), 1000);
    }
}
