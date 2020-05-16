package jmri.jmrit.display.layoutEditor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.*;
import javax.annotation.*;
import javax.swing.JPopupMenu;
import jmri.*;
import jmri.util.*;

/**
 * MVC View component for the LayoutLHTurnout class.
 *
 * @author Bob Jacobsen  Copyright (c) 2020
 * 
 */
public class LayoutLHTurnoutView extends LayoutTurnoutView {

    /**
     * Constructor method.
     * @param turnout the layout left hand turnout to create view for.
     * @param c 2D point.
     * @param rot rotation.
     * @param xFactor horizontal factor.
     * @param yFactor vertical factor.
     * @param layoutEditor main layout editor.
     */
    public LayoutLHTurnoutView(@Nonnull LayoutLHTurnout turnout, 
            @Nonnull Point2D c, double rot,
            double xFactor, double yFactor,
            @Nonnull LayoutEditor layoutEditor) {
        super(turnout, c, rot, xFactor, yFactor, layoutEditor);
        
        // this.turnout = turnout;
    }
        
    // final private LayoutLHTurnout turnout;

    // private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LayoutLHTurnoutView.class);
}
