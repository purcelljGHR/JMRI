package jmri.jmrit.logixng.actions.swing;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.swing.*;

import jmri.InstanceManager;
import jmri.jmrit.logixng.*;
import jmri.jmrit.logixng.actions.ActionClock;
import jmri.jmrit.logixng.actions.ActionClock.ClockState;
import jmri.jmrit.logixng.swing.SwingConfiguratorInterface;
import jmri.jmrit.logixng.util.swing.LogixNG_SelectEnumSwing;
import jmri.jmrit.logixng.util.swing.LogixNG_SelectIntegerSwing;

/**
 * Configures an ActionClock object with a Swing JPanel.
 *
 * @author Daniel Bergqvist Copyright 2021
 * @author Dave Sand Copyright 2021
 */
public class ActionClockSwing extends AbstractDigitalActionSwing {

    private LogixNG_SelectEnumSwing<ClockState> _selectEnumSwing;
    private LogixNG_SelectIntegerSwing _selectTimeSwing;


    public ActionClockSwing() {
    }

    public ActionClockSwing(JDialog dialog) {
        super.setJDialog(dialog);
    }

    @Override
    protected void createPanel(@CheckForNull Base object, @Nonnull JPanel buttonPanel) {
        ActionClock action = (ActionClock) object;

        _selectEnumSwing = new LogixNG_SelectEnumSwing<>(getJDialog(), this);
        _selectTimeSwing = new LogixNG_SelectIntegerSwing(getJDialog(), this);

        panel = new JPanel();
        JPanel tabbedPaneClockState;
        JPanel tabbedPaneTime;

        if (action != null) {
            tabbedPaneClockState = _selectEnumSwing.createPanel(action.getSelectEnum(), ClockState.values());
            tabbedPaneTime = _selectTimeSwing.createPanel(action.getSelectTime());
        } else {
            tabbedPaneClockState = _selectEnumSwing.createPanel(null, ClockState.values());
            tabbedPaneTime = _selectTimeSwing.createPanel(null);
        }

        _selectEnumSwing.addAddressingListener((evt) -> { setSelectTimeEnabled(); });
        _selectEnumSwing.addEnumListener((evt) -> { setSelectTimeEnabled(); });
        setSelectTimeEnabled();

        JComponent[] operationComponents = new JComponent[]{
            tabbedPaneClockState};

        List<JComponent> operationComponentList = SwingConfiguratorInterface.parseMessage(
                Bundle.getMessage("ActionClock_OperationComponents"), operationComponents);

        JPanel panelOperation = new JPanel();
        for (JComponent c : operationComponentList) panelOperation.add(c);

        JComponent[] timeComponents = new JComponent[]{
            tabbedPaneTime};

        List<JComponent> timeComponentList = SwingConfiguratorInterface.parseMessage(
                Bundle.getMessage("ActionClock_TimeComponents"), timeComponents);

        JPanel panelTime = new JPanel();
        for (JComponent c : timeComponentList) panelTime.add(c);

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.add(panelOperation);
        container.add(panelTime);

        panel.add(container);
    }

    private void setSelectTimeEnabled() {
        _selectTimeSwing.setEnabled(
                _selectEnumSwing.getAddressing() != NamedBeanAddressing.Direct
                        || _selectEnumSwing.getEnum() == ClockState.SetClock);
    }

    /** {@inheritDoc} */
    @Override
    public boolean validate(@Nonnull List<String> errorMessages) {
        // Create a temporary action to test formula
        ActionClock action = new ActionClock("IQDA1", null);

        _selectEnumSwing.validate(action.getSelectEnum(), errorMessages);
        _selectTimeSwing.validate(action.getSelectTime(), errorMessages);

        return errorMessages.isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public String getAutoSystemName() {
        return InstanceManager.getDefault(DigitalActionManager.class).getAutoSystemName();
    }

    /** {@inheritDoc} */
    @Override
    public MaleSocket createNewObject(@Nonnull String systemName, @CheckForNull String userName) {
        ActionClock action = new ActionClock(systemName, userName);
        updateObject(action);
        return InstanceManager.getDefault(DigitalActionManager.class).registerAction(action);
    }

    /** {@inheritDoc} */
    @Override
    public void updateObject(@Nonnull Base object) {
        if (! (object instanceof ActionClock)) {
            throw new IllegalArgumentException("object must be an ActionClock but is a: "+object.getClass().getName());
        }
        ActionClock action = (ActionClock) object;

        _selectEnumSwing.updateObject(action.getSelectEnum());
        _selectTimeSwing.updateObject(action.getSelectTime());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Bundle.getMessage("ActionClock_Short");
    }

    @Override
    public void dispose() {
        _selectEnumSwing.dispose();
        _selectTimeSwing.dispose();
    }


//    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ActionClockSwing.class);

}
