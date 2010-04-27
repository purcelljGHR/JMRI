/**
 * XNetTurnout.java
 *
 * Description:		extend jmri.AbstractTurnout for XNet layouts
 * <P>
 * Turnout opperation on XPressNet based systems goes through the 
 * following sequence:
 * <UL>
 * <LI> set the commanded state, and, Send request to command station to 
 *      start sending DCC operations packet to track</LI>
 * <LI> Wait for response message from command station.  (valid response 
 *      list follows)</LI>
 * <LI> Send request to command station to stop sending DCC operations
 *      packet to track</LI>
 * <LI> Wait for response from command station
 *      <UL> 
 *       <LI>If Success Message, set Known State to Commanded State</LI>
 *       <LI>If error message, repeat previous step</LI>
 *      </UL>
 * </LI>
 * </UL>
 * <P>
 * NOTE: Some XPressNet Command stations take no action when the message 
 * generated durring the third step is received
 * <P>
 * Valid response messages are command station dependent,  but there are 
 * 4 possibilities:
 * <UL>
 * <LI> a "Command Successfully Recieved..." (aka "OK") message</LI>
 * <LI> a "Feedback Response Message" indicating the message is for a 
 *      turnout with feedback</LI>
 * <LI> a "Feedback Response Message" indicating the message is for a 
 *      turnout without feedback</LI>
 * <LI> The XPressNet protocol allows for no response. </LI>
 * </UL>
 * <P>
 * Response NOTE 1: The "Command Successfully Received..." message is 
 * generated by the lenz LIxxx interfaces when it successfully transfers 
 * the command to the command station.  When this happens, the command 
 * station generates no useable response message.
 * <P>
 * Response NOTE 2: Currently the only command stations known to generate
 * Feedback response messages are the Lenz LZ100 and LZV100.
 * <P>
 * Response NOTE 3: Software version 3.2 and above LZ100 and LZV100 may 
 * send either a Feedback response or no response at all.  All other 
 * known command stations generate no response.
 * <P>
 * Response NOTE 4: The Feedback response messages may be generated 
 * asynchronously 
 * <P>
 * Response NOTE 5: Feedback response messages may contain feedback for more
 * than one device.  The devices included in the response may or may not 
 * be stationary decoders (they can also be feedback encoders see 
 * {@link XNetSensor}).
 * <P>
 * Response NOTE 6: The last situation situation is not currently handled.
 * The supported interfaces garantee at least an "OK" message will be sent 
 * to the computer
 * <P>
 * What is done with each of the response messages depends on which 
 * feedback mode is in use.  "DIRECT,"MONITORING", and "EXACT" feedback 
 * mode are supported directly by this class.  
 * <P>
 * "DIRECT" mode instantly triggers step 3 when any valid response 
 * message for this turnout is recieved from the command station or 
 * computer interface.
 * <P>
 * "SIGNAL" mode is identical to "DIRECT" mode, except it skips step 2.
 * i.e. it triggers step 3 without receiving any reply from the 
 * command station.
 * <P>
 * "MONITORING" mode is an extention to direct mode. In monitoring mode, 
 * a feedback response message (for a turnout with or without feedback)
 * is interpreted to set the known state of the turnout based on  
 * information provided by the command station.
 * <P>
 * "MONITORING" mode will interpret the feedback response messages when 
 * they are generated by external sources (fascia controls or other 
 * XPressNet devices) and that information is recieved by the computer.
 * <P>
 * "EXACT" mode is an extention of "MONITORING" mode.  In addition to 
 * interpretting all feedback messages from the command station, "EXACT" 
 * mode will monitor the "motion complete" bit of the feedback response.
 * <P>
 * For turnouts without feedback, the motion complete bit is always set,
 * so "EXACT" mode handles these messages as though the specified feedback 
 * mode is "MONITORING" mode.
 * <P>
 * For turnouts with feedback, "EXACT" mode polls the command station 
 * until the motion complete bit is set before triggering step 3 of the 
 * turnout operation sequence.
 * <P>
 * "EXACT" mode will interpret the feedback response messages when 
 * they are generated by external sources (fascia controls or other 
 * XPressNet devices) and that information is recieved by the computer.
 * <P> 
 * NOTE: For LZ100 and LZV100 command stations prior to version 3.2, it 
 * may be necessary to poll for the feedback response data.
 * </P>
 * @author			Bob Jacobsen Copyright (C) 2001
 * @author                      Paul Bender Copyright (C) 2003-2010 
 * @version			$Revision: 2.26 $
 */

package jmri.jmrix.lenz;

import jmri.implementation.AbstractTurnout;

public class XNetTurnout extends AbstractTurnout implements XNetListener {

    /* State information */
    protected static final int OFFSENT = 1;
    protected static final int COMMANDSENT = 2;
    protected static final int IDLE = 0;
    protected int InternalState = IDLE;

    /* Static arrays to hold Lenz specific feedback mode information */
    static String[] modeNames = null;
    static int[] modeValues = null;

    protected int _mThrown = jmri.Turnout.THROWN;
    protected int _mClosed = jmri.Turnout.CLOSED;

    public XNetTurnout(int pNumber) {  // a human-readable turnout number must be specified!
        super("XT"+pNumber);
        mNumber = pNumber;

        /* Add additiona feedback types information */
	_validFeedbackTypes |= MONITORING | EXACT |SIGNAL;

	// Default feedback mode is MONITORING
	_activeFeedbackType = MONITORING;

	// if it hasn't been done already, create static arrays to hold 
        // the Lenz specific feedback information.
	if(modeNames == null) {
           if (_validFeedbackNames.length != _validFeedbackModes.length)
                log.error("int and string feedback arrays different length");
           modeNames  = new String[_validFeedbackNames.length+3];  
           modeValues = new int[_validFeedbackNames.length+3];
           for (int i = 0; i<_validFeedbackNames.length; i++) {
               modeNames[i] = _validFeedbackNames[i];
               modeValues[i] = _validFeedbackModes[i];
           }
           modeNames[_validFeedbackNames.length] = "MONITORING";
           modeValues[_validFeedbackNames.length] = MONITORING;
           modeNames[_validFeedbackNames.length+1] = "EXACT";
           modeValues[_validFeedbackNames.length+1] = EXACT;  
           modeNames[_validFeedbackNames.length+2] = "SIGNAL";
           modeValues[_validFeedbackNames.length+2] = SIGNAL;  
        }

        // set the mode names and values based on the static values.
        _validFeedbackNames = modeNames;
        _validFeedbackModes = modeValues;

        // At construction, register for messages
        // XNetTrafficController.instance().addXNetListener(XNetInterface.FEEDBACK|XNetInterface.COMMINFO|XNetInterface.CS_INFO, this);
	// And to get property change information from the superclass
	_stateListener=new XNetTurnoutStateListener(this);
	this.addPropertyChangeListener(_stateListener);
	// Finally, request the current state from the layout.
    	requestUpdateFromLayout();
    }

    public int getNumber() { return mNumber; }

    // Set the Commanded State.   This method overides setCommandedState in 
    // the Abstract Turnout class.
    public void setCommandedState(int s){
        if(log.isDebugEnabled()) log.debug("set commanded state for turnout "+getSystemName()+" to "+s);
        synchronized(this){
           newCommandedState(s);
        }
        myOperator = getTurnoutOperator();        // MUST set myOperator before starting the thread
        if (myOperator==null) {
                forwardCommandChangeToLayout(s);
                synchronized(this) {
	           newKnownState(INCONSISTENT);
                }
        } else
        {       myOperator.start();
        }

    }   

    // Handle a request to change state by sending an XPressNet command
    synchronized protected void forwardCommandChangeToLayout(int s) {
        if(s!=_mClosed && s!=_mThrown) {
             log.warn("Turnout " + mNumber + ": state " + s + " not forwarded to layout.");
             return;
        }
        // find the command station
        //LenzCommandStation cs = XNetTrafficController.instance().getCommandStation();
        // get the right packet
        XNetMessage msg = XNetMessage.getTurnoutCommandMsg(mNumber,
                                                  (s & _mClosed )!=0,
                                                  (s & _mThrown )!=0,
                                                  true );
        if(getFeedbackMode()==SIGNAL)
        {  
           msg.setTimeout(0); // Set the timeout to 0, so the off message can
    			      // be sent imediately.
           XNetTrafficController.instance().sendXNetMessage(msg, null);
           sendOffMessage();
         } else {
           XNetTrafficController.instance().sendXNetMessage(msg, this);
           InternalState=COMMANDSENT;
         }
    }
    
    protected void turnoutPushbuttonLockout(boolean _pushButtonLockout){
		if (log.isDebugEnabled()) log.debug("Send command to " + (_pushButtonLockout ? "Lock" : "Unlock")+ " Pushbutton XT"+mNumber);
    }

    /**
     * request an update on status by sending an XPressNet message
     */
    public void requestUpdateFromLayout() {
       // To do this, we send an XpressNet Accessory Decoder Information
       // Request.
       // The generated message works for Feedback modules and turnouts
       // with feedback, but the address passed is translated as though it
       // is a turnout address.  As a result, we substitute our base
       // address in for the address. after the message is returned.
       XNetMessage msg = XNetMessage.getFeedbackRequestMsg(mNumber,
                                                 (mNumber%4)<2); 
       XNetTrafficController.instance().sendXNetMessage(msg, null);
    }

        synchronized public void setInverted(boolean inverted) {
                if(log.isDebugEnabled()) log.debug("Inverting Turnout State for turnout xt"+mNumber);
                boolean oldInverted = _inverted;
                _inverted = inverted;
                if(inverted){
                            _mThrown=jmri.Turnout.CLOSED;
                            _mClosed=jmri.Turnout.THROWN;
                            }
                else {
                            _mThrown=jmri.Turnout.THROWN;
                            _mClosed=jmri.Turnout.CLOSED;
                     }
                if (oldInverted != _inverted)
                        firePropertyChange("inverted", Boolean.valueOf(oldInverted),
                                        Boolean.valueOf(_inverted));
        }

    public boolean canInvert() {
                return true;
        }


    /*
     *  Handle an incoming message from the XPressNet
     */
    synchronized public void message(XNetReply l) {
	if(log.isDebugEnabled()) log.debug("recieved message: " +l);
        if(InternalState==OFFSENT) {
	  if(l.isOkMessage()) {
	    /* the command was successfully recieved */
            synchronized(this) {
	       newKnownState(getCommandedState());
	       InternalState=IDLE;
            }
	    return;
	  } else {
            /* Default Behavior: If anything other than an OK message
               is received, Send another OFF message. */
            if(log.isDebugEnabled()) log.debug("Message is not OK message.  Message received was: " + l);
            sendOffMessage();
          }
        }

	switch(getFeedbackMode()) {
	    case EXACT:
		handleExactModeFeedback(l);
		break;
	    case MONITORING:
		handleMonitoringModeFeedback(l);
		break;
            case DIRECT:
	    default:
		// Default is direct mode
		handleDirectModeFeedback(l);
        }
    }

    // listen for the messages to the LI100/LI101
    public void message(XNetMessage l) {
    }

    // Handle a timeout notification
    public void notifyTimeout(XNetMessage msg)
    {
       if(log.isDebugEnabled()) log.debug("Notified of timeout on message" + msg.toString());
       // If we're in the OFFSENT state, we need to send another OFF message.
       if(InternalState==OFFSENT) sendOffMessage();

    }

    /*
     *  With Direct Mode feedback, if we see ANY valid response to our
     *  request, we ask the command station to stop sending information 
     *  to the stationary decoder.
     *  <p>
     *  No effort is made to interpret feedback when using direct mode
     *
     *  @param l an {@link XNetReply} message
     *
     */ 
    synchronized private void handleDirectModeFeedback(XNetReply l) {
       /* If commanded state does not equal known state, we are 
          going to check to see if one of the following conditions 
	  applies:
	  1) The recieved message is a feedback message for a turnout
             and one of the two addresses to which it applies is our 
             address
          2) We recieve an "OK" message, indicating the command was 
	     successfully sent
           
          If either of these two cases occur, we trigger an off message
       */

       if(log.isDebugEnabled()) log.debug("Handle Message for turnout " + 
	  mNumber + " in DIRECT feedback mode ");
       if(getCommandedState()!=getKnownState() || InternalState==COMMANDSENT) {
          if(l.isFeedbackBroadcastMessage()) {
	     int numDataBytes=l.getElement(0)&0x0f;
	     for(int i=1;i<numDataBytes;i+=2) {
                int messageType= l.getFeedbackMessageType(i);
	        if(messageType==0 || messageType == 1) {
                   if ((mNumber%2==1 && 
                       (l.getTurnoutMsgAddr(i) == mNumber)) ||
                      (((mNumber%2)==0) && 
                       (l.getTurnoutMsgAddr(i) == mNumber-1))) {
		      // This message includes feedback for this turnout  
                      if(log.isDebugEnabled()) log.debug("Turnout " + mNumber + " DIRECT feedback mode - directed reply received."); 
		      sendOffMessage();
                      // Explicitly send two off messages in Direct Mode
		      sendOffMessage();
		      break;
                   }
                }  
             }     
          } else if(l.isOkMessage()) {
             // Finally, we may just recieve an OK message.
             if(log.isDebugEnabled()) log.debug("Turnout " + mNumber + " DIRECT feedback mode - OK message triggering OFF message."); 
	     sendOffMessage();
             // Explicitly send two off messages in Direct Mode
	     sendOffMessage();
	  } else return;
       }
    }

    /*
     *  With Monitoring Mode feedback, if we see a feedback message, we 
     *  interpret that message and use it to display our feedback. 
     *  <P> 
     *  After we send a request to operate a turnout, We ask the command 
     *  station to stop sending information to the stationary decoder
     *  when the either a feedback message or an "OK" message is recieved.
     *
     *  @param l an {@link XNetReply} message
     *
     */ 
    synchronized private void handleMonitoringModeFeedback(XNetReply l){
       /* In Monitoring Mode, We have two cases to check if CommandedState 
          does not equal KnownState, otherwise, we only want to check to 
	  see if the messages we recieve indicate this turnout chagned 
          state
       */
       if(log.isDebugEnabled()) log.debug("Handle Message for turnout " + 
	                   mNumber + " in MONITORING feedback mode "); 
       //if(getCommandedState()==getKnownState() && InternalState==IDLE) {
       if(InternalState==IDLE) {
	  if(l.isFeedbackBroadcastMessage()) {
             // This is a feedback message, we need to check and see if it
             // indicates this turnout is to change state or if it is for 
             // another turnout.
	     int numDataBytes=l.getElement(0)&0x0f;
	     for(int i=1;i<numDataBytes;i+=2) {
	        if(parseFeedbackMessage(l,i)!=-1){
                   if(log.isDebugEnabled()) log.debug("Turnout " + mNumber + " MONITORING feedback mode - state change from feedback."); 
	           break;
                 }
             }
          }
       } else if(getCommandedState()!=getKnownState() || 
                 InternalState==COMMANDSENT) {
          if(l.isFeedbackBroadcastMessage()) {
	     int numDataBytes=l.getElement(0)&0x0f;
	     for(int i=1;i<numDataBytes;i+=2) {
                int messageType= l.getFeedbackMessageType(i);
	        if(messageType==0 || messageType == 1) {
	           // In Monitoring mode, treat both turnouts with feedback 
                   // and turnouts without feedback as turnouts without 
                   // feedback.  i.e. just interpret the feedback 
                   // message, don't check to see if the motion is complete
	           if(parseFeedbackMessage(l,i)!=-1) {
                   // We need to tell the turnout to shut off the output.
                      if(log.isDebugEnabled()) log.debug("Turnout " + mNumber + " MONITORING feedback mode - state change from feedback, CommandedState != KnownState."); 
	              sendOffMessage();
                      break;
                   }
                }
             }       
	  } else if (l.isOkMessage()) {
             // Finally, we may just recieve an OK message.
             if(log.isDebugEnabled()) log.debug("Turnout " + mNumber + " MONITORING feedback mode - OK message triggering OFF message."); 
	     sendOffMessage();
          } else return;
       }
    }
   

    /*
     *  With Exact Mode feedback, if we see a feedback message, we 
     *  interpret that message and use it to display our feedback. 
     *  <P> 
     *  After we send a request to operate a turnout, We ask the command 
     *  station to stop sending information to the stationary decoder
     *  when the either a feedback message or an "OK" message is recieved.
     *
     *  @param l an {@link XNetReply} message
     *
     */ 
    synchronized private void handleExactModeFeedback(XNetReply l) {
       // We have three cases to check if CommandedState does 
       // not equal KnownState, otherwise, we only want to check to 
       // see if the messages we recieve indicate this turnout chagned 
       // state
       if(log.isDebugEnabled()) log.debug("Handle Message for turnout " + 
				mNumber + " in EXACT feedback mode "); 
       if(getCommandedState()==getKnownState() && InternalState==IDLE) {
          if(l.isFeedbackBroadcastMessage()) {
             // This is a feedback message, we need to check and see if it
             // indicates this turnout is to change state or if it is for 
             // another turnout.
	     int numDataBytes=l.getElement(0)&0x0f;
	     for(int i=1;i<numDataBytes;i+=2) {
	        if(parseFeedbackMessage(l,i)!=-1)
                   if(log.isDebugEnabled()) log.debug("Turnout " + mNumber + " EXACT feedback mode - state change from feedback."); 
                   break;
                }
             }
       } else if(getCommandedState()!=getKnownState() || 
                 InternalState==COMMANDSENT) {
          if(l.isFeedbackBroadcastMessage()) {
             int numDataBytes=l.getElement(0)&0x0f;
             for(int i=1;i<numDataBytes;i+=2) {
                if ((mNumber%2==1 && 
                    (l.getTurnoutMsgAddr(i) == mNumber)) ||
                   (((mNumber%2)==0) && 
                    (l.getTurnoutMsgAddr(i) == mNumber-1))) {
		   // This message includes feedback for this turnout  
                   int messageType= l.getFeedbackMessageType(i);
	           if(messageType == 1) {
	              // The first case is that we recieve a message for 
                      // this turnout and this turnout provides feedback.
                      // In this case, we want to check to see if the 
                      // turnout has completed it's movement before doing 
                      // anything else.
	              if(!motionComplete(l,i)) {
                         if(log.isDebugEnabled()) log.debug("Turnout " + mNumber + " EXACT feedback mode - state change from feedback, CommandedState!=KnownState - motion not complete"); 
                         // If the motion is NOT complete, send a feedback 
                         // request for this nibble
                         XNetMessage msg = XNetMessage.getFeedbackRequestMsg(
                                            mNumber, ((mNumber%4)<=1));
                         XNetTrafficController.instance()
                                            .sendXNetMessage(msg, null);
                      } else {
                         if(log.isDebugEnabled()) log.debug("Turnout " + mNumber + " EXACT feedback mode - state change from feedback, CommandedState!=KnownState - motion complete"); 
                         // If the motion is completed, behave as though 
                         // this is a turnout without feedback.
	                 parseFeedbackMessage(l,i);
                         // We need to tell the turnout to shut off the 
                         // output.
	                 sendOffMessage();
                      }       
                   } else if (messageType == 0) {
                     if(log.isDebugEnabled()) log.debug("Turnout " + mNumber + " EXACT feedback mode - state change from feedback, CommandedState!=KnownState - Turnout does not provide feedback"); 
                     // The second case is that we recieve a message about
                     // this turnout, and this turnout does not provide 
                     // feedback. In this case, we want to check the 
                     // contents of the message and act accordingly.
	             parseFeedbackMessage(l,i);
                     // We need to tell the turnout to shut off the output.
	             sendOffMessage();
                   }
                   break;
                }
             }
	  } else if (l.isOkMessage()) {
             // Finally, we may just recieve an OK message.
             if(log.isDebugEnabled()) log.debug("Turnout " + mNumber + " EXACT feedback mode - OK message triggering OFF message."); 
	     sendOffMessage();
	  } else return;
       }
    }

    /* Send an "Off" message to the decoder for this output  */
    protected synchronized void sendOffMessage() {
            // We need to tell the turnout to shut off the output.
	    if(log.isDebugEnabled()) {
                log.debug("Sending off message for turnout " + mNumber + " commanded state= " +getCommandedState());
                log.debug("Current Thread ID: " + java.lang.Thread.currentThread().getId() + " Thread Name " +java.lang.Thread.currentThread().getName());
            }
            XNetMessage msg = XNetMessage.getTurnoutCommandMsg(mNumber,
                                                  getCommandedState()==_mClosed,
                                                  getCommandedState()==_mThrown,
                                                  false ); 
	    // Set the known state to the commanded state.
            synchronized(this) {
               //try{
                   // To avoid some of the command station busy 
                   // messages, add a short delay before sending the 
                   // first off message.  
                   if(InternalState != OFFSENT) {
                        new java.util.Timer().schedule(new offTask(this),30);
                        newKnownState(getCommandedState());
                        InternalState = OFFSENT;
                        return;
                   };
               //} catch(java.lang.InterruptedException ie) {
               //    log.debug("wait interrupted");
               //}
	       newKnownState(getCommandedState());
	       InternalState = OFFSENT;
            }
            // Then send the message.
            XNetTrafficController.instance().sendHighPriorityXNetMessage(msg, this);
    }


    class offTask extends java.util.TimerTask{
        XNetTurnout t;
        public offTask(XNetTurnout turnout){
           super();
           t=turnout;
        }
        public void run() {
            // We need to tell the turnout to shut off the output.
	    if(log.isDebugEnabled()) {
                log.debug("Sending off message for turnout " + mNumber + " commanded state= " +getCommandedState());
                log.debug("Current Thread ID: " + java.lang.Thread.currentThread().getId() + " Thread Name " +java.lang.Thread.currentThread().getName());
            }
            // Generate the message
            XNetMessage msg = XNetMessage.getTurnoutCommandMsg(mNumber,
                                                  getCommandedState()==_mClosed,
                                                  getCommandedState()==_mThrown,
                                                  false ); 
            // Then send the message.
            XNetTrafficController.instance().sendXNetMessage(msg, t);
        }
    }

     /*
      * parse the feedback message, and set the status of the turnout 
      * accordingly
      *
      * @param l - feedback broadcast message
      * @param startByte - first Byte of message to check
      * 
      * @return 0 if address matches our turnout -1 otherwise
      */
     synchronized private int parseFeedbackMessage(XNetReply l,int startByte) {
        // check validity & addressing
        // if this is an ODD numbered turnout, then we always get the 
        // right response from .getTurnoutMsgAddr.  If this is an even 
        // numbered turnout, we need to check the messages for the odd 
        // numbered turnout in the nibble as well.
        if (mNumber%2==1 && (l.getTurnoutMsgAddr(startByte) == mNumber)) {
            // is for this object, parse the message
            if (log.isDebugEnabled()) log.debug("Message for turnout " + mNumber);
            if(l.getTurnoutStatus(startByte,1)==THROWN) {
               synchronized(this) {
                  newCommandedState(_mThrown);
                  newKnownState(getCommandedState());
               }
	       return(0);
            } else if(l.getTurnoutStatus(startByte,1)==CLOSED) { 
               synchronized(this) {
                  newCommandedState(_mClosed);
                  newKnownState(getCommandedState());
               }
	       return(0);
            } else {
               // the state is unknown or inconsistent.  If the command state 
               // does not equal the known state, and the command repeat the 
               // last command
               if(getCommandedState()!=getKnownState())
                   forwardCommandChangeToLayout(getCommandedState());
               return -1;
            }
        } else if (((mNumber%2)==0) && 
                   (l.getTurnoutMsgAddr(startByte) == mNumber-1)) {
            // is for this object, parse message type
            if (log.isDebugEnabled()) log.debug("Message for turnout" + mNumber);
            if(l.getTurnoutStatus(startByte,0)==THROWN) {
               synchronized(this) {
                  newCommandedState(_mThrown);
                  newKnownState(getCommandedState());
               }
	       return(0);
            } else if(l.getTurnoutStatus(startByte,0)==CLOSED) { 
               synchronized(this) {
                  newCommandedState(_mClosed);
                  newKnownState(getCommandedState());
               }
	       return(0);
            } else {
               // the state is unknown or inconsistent.  If the command state 
               // does not equal the known state, and the command repeat the 
               // last command
               if(getCommandedState()!=getKnownState())
                   forwardCommandChangeToLayout(getCommandedState());
               return -1;
            }
        }    
       return(-1);
    }


     /*
      * Determine if this feedback message says the turnout has completed 
      * it's motion or not.  Returns true for mostion complete, false 
      * otherwise. 
      *
      * @param l - feedback broadcast message
      * @param startByte - first Byte of message to check
      * 
      * @return true if motion complete, false otherwise
      */
     synchronized private boolean motionComplete(XNetReply l,int startByte) {
        // check validity & addressing
        // if this is an ODD numbered turnout, then we always get the 
        // right response from .getTurnoutMsgAddr.  If this is an even 
        // numbered turnout, we need to check the messages for the odd 
        // numbered turnout in the nibble as well.
        if (mNumber%2==1 && (l.getTurnoutMsgAddr(startByte) == mNumber)) {
            // is for this object, parse the message
            int messageType= l.getFeedbackMessageType(startByte);
	  if(messageType == 1) {
             int a2=l.getElement(startByte+1);
             if((a2 & 0x80)==0x80) { return false;
             } else { return true; }
	  } else return false;
        } else if (((mNumber%2)==0) && 
                   (l.getTurnoutMsgAddr(startByte) == mNumber-1)) {
            // is for this object, parse the message
          int messageType= l.getFeedbackMessageType(startByte);
	  if(messageType == 1) {
             int a2=l.getElement(startByte+1);
             if((a2&0x80)==0x80) { return false;
             } else { return true; }
	  } else return false;            
        }    
       return(false);
    }

    public void dispose() {
        //XNetTrafficController.instance().removeXNetListener(XNetInterface.FEEDBACK|XNetInterface.COMMINFO|XNetInterface.CS_INFO, this);
	    this.removePropertyChangeListener(_stateListener);
	    super.dispose();
    }

   
    // Internal class to use for listening to state changes
    private class XNetTurnoutStateListener implements java.beans.PropertyChangeListener {

    XNetTurnout _turnout=null;

    XNetTurnoutStateListener(XNetTurnout turnout){
	_turnout=turnout;
    }

    /*
     * If we're  not using DIRECT feedback mode, we need to listen for 
     * state changes to know when to send an OFF message after we set the 
     * known state
     * If we're using DIRECT mode, all of this is handled from the 
     * XPressNet Messages
     */
    public void propertyChange(java.beans.PropertyChangeEvent event) {
	if(log.isDebugEnabled()) log.debug("propertyChange called");
	// If we're using DIRECT feedback mode, we don't care what we see here
	if(_turnout.getFeedbackMode()!=DIRECT) {
	   if(log.isDebugEnabled()) log.debug("propertyChange Not Direct Mode property: " +event.getPropertyName()+ " old value " +event.getOldValue()+ " new value " +event.getNewValue());
	   if(event.getPropertyName().equals("KnownState")) {
		// Check to see if this is a change in the status 
		// triggered by a device on the layout, or a change in 
		// status we triggered.
		int oldKnownState=((Integer)event.getOldValue()).intValue();
		int curKnownState=((Integer)event.getNewValue()).intValue();
	        if(log.isDebugEnabled()) log.debug("propertyChange KnownState - old value " + oldKnownState + " new value " + curKnownState);
		if(curKnownState!=INCONSISTENT && 
	           _turnout.getCommandedState()==oldKnownState) {
		   // This was triggered by feedback on the layout, change 
		   // the commanded state to reflect the new Known State
	           if(log.isDebugEnabled()) log.debug("propertyChange CommandedState: " +_turnout.getCommandedState());
               	   _turnout.newCommandedState(curKnownState);
		} else {
		   // Since we always set the KnownState to 
		   // INCONSISTENT when we send a command, If the old 
		   // known state is INCONSISTENT, we just want to send 
                   // an off message
		   if(oldKnownState==INCONSISTENT){
	              if(log.isDebugEnabled()) log.debug("propertyChange CommandedState: " +_turnout.getCommandedState());
		   	_turnout.sendOffMessage();
		   }
		}
	    }
	}	
    }
    
    }

    // data members
    protected int mNumber;   // XPressNet turnout number
    XNetTurnoutStateListener _stateListener;  // Internal class object

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(XNetTurnout.class.getName());

}


/* @(#)XNetTurnout.java */

