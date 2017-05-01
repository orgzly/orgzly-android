package com.orgzly.android.uiautomator;

import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests the ListWIdget with the UI-Automator framework
 * tested on an Nexus 5 Emulator with Android 25
 * Must have an instance of the ListWidget on the Homescreen, when starting this test
 * Also the default version of Getting Started with Orgzly must be the only notebook
 */
public class ListWidgetTest {

    private static final String HEADER_FILTER = "com.orgzly:id/widget_list_header_filter";
    private static final String HEADER_ADD = "com.orgzly:id/widget_list_header_add";
    private static final String ITEM_DONE = "com.orgzly:id/item_list_widget_done";
    private static final String ITEM_TITLE = "com.orgzly:id/item_list_widget_title";
    private UiDevice device;

    @Before
    public void addWidget() throws Exception {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        device.pressHome();
        device.pressHome();
//        // simulate long click on background
//        device.swipe(0, device.getDisplayHeight() / 2, 0, device.getDisplayHeight() / 2, 100);
//
//        UiObject2 widgetsButton = device.findObject(By.text("WIDGETS"));
//        widgetsButton.click();
//
//        UiScrollable widgetsView = new UiScrollable(new UiSelector().resourceId("com.google.android.apps.nexuslauncher:id/widgets_list_view"));
//        widgetsView.scrollTextIntoView("Search");
//
//        UiObject2 listWidgetButton = device.findObject(By.text("Search"));
//        listWidgetButton.swipe(Direction.UP, 0, 300);
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

    private UiObject2 findObject(BySelector by) {
        return device.wait(Until.findObject(by), 1000);
    }
    private List<UiObject2> findObjects(BySelector by) {
        return device.wait(Until.findObjects(by), 1000);
    }
}
