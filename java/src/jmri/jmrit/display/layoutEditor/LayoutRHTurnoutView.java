package jmri.jmrit.display.layoutEditor;

import java.awt.Graphics2D;
import java.awt.geom.*;
import java.util.*;
import javax.annotation.*;
import javax.swing.JPopupMenu;
import jmri.*;
import jmri.util.*;

/**
 * MVC View component for the LayoutRHTurnout class.
 *
 * @author Bob Jacobsen  Copyright (c) 2020
 * 
 */
public class LayoutRHTurnoutView extends LayoutTurnoutView {

    /**
     * Constructor method.
     * @param turnout the turnout to view.
     */
    public LayoutRHTurnoutView(@Nonnull LayoutRHTurnout turnout, 
            @Nonnull Point2D c, double rot,
            double xFactor, double yFactor,
            @Nonnull LayoutEditor layoutEditor) {
        super(turnout, c, rot, xFactor, yFactor, layoutEditor);
        
        // this.turnout = turnout;
    }
        
    // final private LayoutRHTurnout turnout;

    // private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LayoutRHTurnoutView.class);
}
