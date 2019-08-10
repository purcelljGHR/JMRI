package jmri.jmrix.pi;

import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioProvider;
import jmri.Turnout;
import jmri.util.JUnitUtil;
import jmri.util.junit.annotations.ToDo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests for RaspberryPiTurnoutManager.
 *
 * @author Paul Bender Copyright (C) 2016
 */
public class RaspberryPiTurnoutManagerTest extends jmri.managers.AbstractTurnoutMgrTestBase {

    @Override
    public String getSystemName(int i) {
        return l.getSystemPrefix() + "T" + i;
    }

    @Test
    public void ConstructorTest() {
        Assert.assertNotNull(l);
    }

    @Test
    public void checkPrefix() {
        Assert.assertEquals("Prefix", "P", l.getSystemPrefix());
    }

    @Override
    @Test
    public void testTurnoutPutGet() {
        // create
        Turnout t = l.newTurnout(getSystemName(18), "mine");
        // check
        Assert.assertNotNull("real object returned ", t);
        Assert.assertEquals("user name correct ", t, l.getByUserName("mine"));
        Assert.assertEquals("system name correct ", t, l.getBySystemName(getSystemName(18)));
    }

    @Test
    @Override
    public void testProvideName() {
        // create
        Turnout t = l.provide(getSystemName(20));
        // check
        Assert.assertTrue("real object returned ", t != null);
        Assert.assertEquals("system name correct ", t, l.getBySystemName(getSystemName(20)));
    }

    @Override
    @Test
    public void testDefaultSystemName() {
        // create
        Turnout t = l.provideTurnout(getSystemName(getNumToTest1()));
        // check
        Assert.assertNotNull("real object returned ", t);
        Assert.assertEquals("system name correct ", t, l.getBySystemName(getSystemName(getNumToTest1())));
    }

    @Override
    @Test
    public void testSingleObject() {
        // test that you always get the same representation
        Turnout t1 = l.newTurnout(getSystemName(16), "mine");
        Assert.assertNotNull("t1 real object returned ", t1);
        Assert.assertEquals("same by user ", t1, l.getByUserName("mine"));
        Assert.assertEquals("same by system ", t1, l.getBySystemName(getSystemName(16)));

        Turnout t2 = l.newTurnout(getSystemName(16), "mine");
        Assert.assertNotNull("t2 real object returned ", t2);
        // check
        Assert.assertEquals("same new ", t1, t2);
    }

    @Override
    @Test
    public void testRename() {
        // get turnout
        Turnout t1 = l.newTurnout(getSystemName(15), "before");
        Assert.assertNotNull("t1 real object ", t1);
        t1.setUserName("after");
        Turnout t2 = l.getByUserName("after");
        Assert.assertEquals("same object", t1, t2);
        Assert.assertNull("no old object", l.getByUserName("before"));
    }

    @Test
    @Ignore("This test doesn't work for this class")
    @ToDo("RaspberryPiSensor.init throws the error: com.pi4j.io.gpio.exception.GpioPinExistsException: This GPIO pin already exists: GPIO 1")
    @Override
    public void testRegisterDuplicateSystemName() {
    }

    @Test
    @Override
    public void testSetAndGetOutputInterval() {
        Turnout t1 = l.newTurnout(getSystemName(17), "mine");
        Assert.assertEquals("default outputInterval", 250, l.getOutputInterval(t1.getSystemName())); // only the prefix of t1 is used to find the manager
        l.setOutputInterval(t1.getSystemName(), 50);
        Assert.assertEquals("new outputInterval from manager", 250, l.getOutputInterval(t1.getSystemName())); // only the prefix of t1 is used to find manager, interval is not stored in AbstractTurnoutManager
    }

    @Override
    protected int getNumToTest1() {
        return 19;
    }

    @Override
    protected int getNumToTest2() {
        return 5;
    }

    @Override
    @Before
    public void setUp() {
        JUnitUtil.setUp();
        GpioProvider myprovider = new PiGpioProviderScaffold();
        GpioFactory.setDefaultProvider(myprovider);
        jmri.util.JUnitUtil.resetInstanceManager();
        l = new RaspberryPiTurnoutManager(new RaspberryPiSystemConnectionMemo());
    }

    @After
    public void tearDown() {
        JUnitUtil.clearShutDownManager();
        JUnitUtil.resetInstanceManager();
        JUnitUtil.tearDown();
    }

}
