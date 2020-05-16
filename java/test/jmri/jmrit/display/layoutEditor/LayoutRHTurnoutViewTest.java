package jmri.jmrit.display.layoutEditor;

import java.awt.GraphicsEnvironment;
import java.awt.geom.Point2D;

import jmri.util.JUnitUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Test simple functioning of LayoutRHTurnoutView
 *
 * @author Bob Jacobsen Copyright (C) 2020
 */
public class LayoutRHTurnoutViewTest extends LayoutTurnoutViewTest {

    @Test
    public void testCtor() {
        Point2D point = new Point2D.Double(150.0, 100.0);
        
        new LayoutRHTurnoutView(turnout, point, 99.0, 1.5, 1.6, layoutEditor);
    }


    LayoutEditor layoutEditor;
    LayoutRHTurnout turnout;
    
    @Before
    public void setUp() {
        super.setUp();
        if (!GraphicsEnvironment.isHeadless()) {
            JUnitUtil.resetProfileManager();

            layoutEditor = new LayoutEditor();
             
            turnout = new LayoutRHTurnout("Wye", layoutEditor);
        }
    }

    @After
    public void tearDown() {
        if (layoutEditor != null) {
            JUnitUtil.dispose(layoutEditor);
        }
        layoutEditor = null;
        turnout = null;
        JUnitUtil.deregisterBlockManagerShutdownTask();
        super.tearDown();
    }
}
