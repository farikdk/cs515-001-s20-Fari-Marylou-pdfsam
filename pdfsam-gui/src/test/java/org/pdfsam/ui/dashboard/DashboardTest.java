/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 03/set/2014
 * Copyright 2017 by Sober Lemur S.a.s. di Vacondio Andrea (info@pdfsam.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pdfsam.ui.dashboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pdfsam.eventstudio.StaticStudio.eventStudio;

import java.util.Arrays;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.pdfsam.ConfigurableProperty;
import org.pdfsam.Pdfsam;
import org.pdfsam.injector.Components;
import org.pdfsam.injector.Injector;
import org.pdfsam.injector.Provides;
import org.pdfsam.test.ClearEventStudioRule;
import org.pdfsam.test.InitializeAndApplyJavaFxThreadRule;
import org.pdfsam.ui.dashboard.about.AboutDashboardPane;
import org.pdfsam.ui.event.SetActiveDashboardItemRequest;
import org.pdfsam.ui.event.SetTitleEvent;
import org.pdfsam.eventstudio.Listener;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * @author Andrea Vacondio
 *
 */
public class DashboardTest {
    @ClassRule
    public static ClearEventStudioRule STUDIO_RULE = new ClearEventStudioRule();
    @Rule
    public InitializeAndApplyJavaFxThreadRule javaFxThread = new InitializeAndApplyJavaFxThreadRule();

    @Components({ AboutDashboadItem.class })
    static class Config {
        @Provides
        public AboutDashboardPane aboutPane() {
            Pdfsam pdfsam = mock(Pdfsam.class);
            when(pdfsam.name()).thenReturn("PDFsam");
            when(pdfsam.property(ConfigurableProperty.VERSION)).thenReturn("3.0.0");
            when(pdfsam.property(ConfigurableProperty.HOME_URL)).thenReturn("http://www.pdfsam.org");
            when(pdfsam.property(ConfigurableProperty.HOME_LABEL)).thenReturn("home");
            when(pdfsam.property(ConfigurableProperty.FEED_URL)).thenReturn("http://www.pdfsam.org/feed/");
            when(pdfsam.property(ConfigurableProperty.DOCUMENTATION_URL))
                    .thenReturn("http://www.pdfsam.org/documentation");
            when(pdfsam.property(ConfigurableProperty.SUPPORT_URL)).thenReturn("http://www.pdfsam.org/support");
            when(pdfsam.property(ConfigurableProperty.SCM_URL)).thenReturn("http://www.pdfsam.org/scm");
            when(pdfsam.property(ConfigurableProperty.TRANSLATE_URL)).thenReturn("http://www.pdfsam.org/translate");
            when(pdfsam.property(ConfigurableProperty.TWITTER_URL)).thenReturn("http://www.pdfsam.org/twitter");
            when(pdfsam.property(ConfigurableProperty.DONATE_URL)).thenReturn("http://www.pdfsam.org/donate");
            when(pdfsam.property(ConfigurableProperty.FACEBOOK_URL)).thenReturn("http://www.pdfsam.org/facebook");
            when(pdfsam.property(ConfigurableProperty.LICENSE_NAME)).thenReturn("agpl3");
            when(pdfsam.property(ConfigurableProperty.LICENSE_URL))
                    .thenReturn("http://www.gnu.org/licenses/agpl-3.0.html");
            when(pdfsam.property(ConfigurableProperty.TRACKER_URL)).thenReturn("http://www.pdfsam.org/issue_tracker");
            when(pdfsam.property(ConfigurableProperty.THANKS_URL)).thenReturn("http://www.pdfsam.org/issue_tracker");
            AboutDashboardPane about = new AboutDashboardPane(pdfsam);
            about.setId("aboutPane");
            return about;
        }

    }

    static class DisabledItem implements DashboardItem {

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public Pane pane() {
            return new HBox();
        }

        @Override
        public String name() {
            return "disabled";
        }

        @Override
        public String id() {
            return "disabled";
        }

        @Override
        public Node graphic() {
            return null;
        }

        @Override
        public boolean disabled() {
            return true;
        }
    }

    static class EnabledItem implements DashboardItem {

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public Pane pane() {
            return new HBox();
        }

        @Override
        public String name() {
            return "enabled";
        }

        @Override
        public String id() {
            return "enabled";
        }

        @Override
        public Node graphic() {
            return null;
        }
    }

    @Test
    public void allItemsArePicked() {
        Injector.addConfig(new Config());
        Injector.add(EnabledItem.class);
        try (Injector injector = Injector.start()) {
            Dashboard victim = injector.instance(Dashboard.class);
            assertEquals(2, victim.getChildren().size());
        }
    }

    @Test
    public void disabledItemsAreNotPicked() {
        List<DashboardItem> items = Arrays.asList(new EnabledItem(), new DisabledItem());
        Dashboard victim = new Dashboard(items, new QuickbarDashboardButtonsPane(items));
        assertTrue(victim.hasItem("enabled"));
        assertFalse(victim.hasItem("disabled"));
    }

    @Test
    public void wrongModuleDoesntBoom() {
        try (Injector injector = Injector.start(new Config())) {
            Dashboard victim = injector.instance(Dashboard.class);
            victim.onSetActiveDashboardItem(new SetActiveDashboardItemRequest("chuck norris"));
        }
    }

    @Test
    public void eventIsSent() {
        try (Injector injector = Injector.start(new Config())) {
            Dashboard victim = injector.instance(Dashboard.class);
            assertTrue(((StackPane) victim.getCenter()).getChildren().isEmpty());
            Listener<SetTitleEvent> listener = mock(Listener.class);
            eventStudio().add(SetTitleEvent.class, listener);
            victim.onSetActiveDashboardItem(
                    new SetActiveDashboardItemRequest(injector.instance(AboutDashboadItem.class).id()));
            verify(listener).onEvent(any());
            assertFalse(((StackPane) victim.getCenter()).getChildren().isEmpty());
        }
    }

}
