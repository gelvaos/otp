package com.ng.otp;

/*
 * Imported packages
 */
import sim.toolkit.*;
import javacard.framework.*;
import javacard.security.*;

public class otp extends javacard.framework.Applet implements ToolkitInterface,
        ToolkitConstants {
    
    private short KEY_LENGTH = 20; // 160 bits
    private short KEY_LENGTH_ASCII = (short) (KEY_LENGTH * 2);
    
    // Mandatory variables
    private ToolkitRegistry reg;
    // Main Menu
    private byte            idMenuGetNextPasswd;
    private byte[]          menuGetNextPasswd;
    private byte            idMenuInitKey;
    private byte[]          menuInitKey;
    // Key variables
    private boolean         isKeyInitialized;
    private byte[]          key;
    // Crypto stuff
    private MessageDigest   hash;

    // Messages
    private byte[]          msgEnterNewKey        = new byte[] { (byte) 'E',
            (byte) 'n', (byte) 't', (byte) 'e', (byte) 'r', (byte) ' ',
            (byte) 'n', (byte) 'e', (byte) 'w', (byte) ' ', (byte) 'k',
            (byte) 'e', (byte) 'y', (byte) ':'   };
    private byte[]          msgWrongKeyLen        = new byte[] { (byte) 'I',
            (byte) 'n', (byte) 'c', (byte) 'o', (byte) 'r', (byte) 'r',
            (byte) 'e', (byte) 'c', (byte) 't', (byte) ' ', (byte) 'k',
            (byte) 'e', (byte) 'y', (byte) ' ', (byte) 'l', (byte) 'e',
            (byte) 'n', (byte) 'g', (byte) 't', (byte) 'h', (byte) '.',
            (byte) ' ', (byte) 'E', (byte) 'x', (byte) 'p', (byte) 'e',
            (byte) 'c', (byte) 't', (byte) 'e', (byte) 'd', (byte) ' ',
            (byte) '1', (byte) '6', (byte) ' ', (byte) 'b', (byte) 'y',
            (byte) 't', (byte) 'e', (byte) 's', (byte) 's' };
    private byte[]          msgKeyNotInited       = new byte[] { (byte) 'K',
            (byte) 'e', (byte) 'y', (byte) ' ', (byte) 'n', (byte) 'o',
            (byte) 't', (byte) ' ', (byte) 'i', (byte) 'n', (byte) 'i',
            (byte) 't', (byte) 'i', (byte) 'a', (byte) 'l', (byte) 'i',
            (byte) 'z', (byte) 'e', (byte) 'd'   };
    private byte[]          msgKeyInited          = new byte[] { (byte) 'N',
            (byte) 'e', (byte) 'w', (byte) ' ', (byte) 'k', (byte) 'e',
            (byte) 'y', (byte) ' ', (byte) 'i', (byte) 'n', (byte) 'i',
            (byte) 't', (byte) 'i', (byte) 'a', (byte) 'l', (byte) 'i',
            (byte) 'z', (byte) 'e', (byte) 'd'   };
    private byte[]          msgToolkitExp         = new byte[] { (byte) 'T',
            (byte) 'o', (byte) 'o', (byte) 'l', (byte) 'k', (byte) 'i',
            (byte) 't', (byte) ' ', (byte) 'e', (byte) 'r', (byte) 'r',
            (byte) 'o', (byte) 'r'               };
    private byte[]          msgArrayOutOfBoundExp = new byte[] { (byte) 'A',
            (byte) 'r', (byte) 'r', (byte) ' ', (byte) 'O', (byte) 'o',
            (byte) 'B', (byte) ' ', (byte) 'e', (byte) 'r', (byte) 'r',
            (byte) 'o', (byte) 'r'               };
    private byte[]          msgUnknownErr         = new byte[] { (byte) 'U',
            (byte) 'n', (byte) 'k', (byte) 'n', (byte) 'o', (byte) 'w',
            (byte) 'n', (byte) ' ', (byte) 'e', (byte) 'r', (byte) 'r',
            (byte) 'o', (byte) 'r'               };
    private byte[]          tmpBuf;


    private byte[]          hexNibbles = new byte[] { 0x00, 0x0A, 0x0B,
                                                      0x0C, 0x0D, 0x0E, 0x0F };

    /**
     * Constructor of the applet
     */
    public otp() {
        // Get the reference of the applet ToolkitRegistry object
        reg = ToolkitRegistry.getEntry ();

        menuGetNextPasswd = new byte[] { (byte) 'G', (byte) 'e', (byte) 't',
                (byte) ' ', (byte) 'n', (byte) 'e', (byte) 'x', (byte) 't',
                (byte) ' ', (byte) 'p', (byte) 'a', (byte) 's', (byte) 's',
                (byte) 'w', (byte) 'o', (byte) 'r', (byte) 'd' };
        menuInitKey = new byte[] { (byte) 'I', (byte) 'n', (byte) 'i',
                (byte) 't', (byte) ' ', (byte) 'k', (byte) 'e', (byte) 'y' };
        // Define the applet Menu Entry
        idMenuGetNextPasswd = reg
                .initMenuEntry (menuGetNextPasswd, (short) 0,
                                (short) menuGetNextPasswd.length,
                                PRO_CMD_SELECT_ITEM, false, (byte) 0, (short) 0);
        idMenuInitKey = reg
                .initMenuEntry (menuInitKey, (short) 0,
                                (short) menuInitKey.length, (byte) 0, false,
                                (byte) 0, (short) 0);

        // during instantiation of the applet key isn't initialized yet
        isKeyInitialized = false;
        // fixing key length to 20 bytes
        key = new byte[KEY_LENGTH];

        tmpBuf = new byte[KEY_LENGTH_ASCII];
    }

    /**
     * Method called by the JCRE at the installation of the applet
     * 
     * @param bArray
     *            the byte array containing the AID bytes
     * @param bOffset
     *            the start of AID bytes in bArray
     * @param bLength
     *            the length of the AID bytes in bArray
     */
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        // Create the Java SIM toolkit applet
        otp StkCommandsExampleApplet = new otp ();
        // Register this applet
        StkCommandsExampleApplet.register (bArray, (short) (bOffset + 1),
                                           (byte) bArray[bOffset]);
    }

    /**
     * Method called by the SIM Toolkit Framework
     * 
     * @param event
     *            the byte representation of the event triggered
     */
    public void processToolkit(byte event) {
        EnvelopeHandler envHdlr = EnvelopeHandler.getTheHandler ();

        // Manage the request following the MENU SELECTION event type
        if (event == EVENT_MENU_SELECTION) {
            // Get the selected item
            byte selectedItemId = envHdlr.getItemIdentifier ();

            // Perform the required service following the menuGetNextPasswd
            // selected item
            if (selectedItemId == idMenuGetNextPasswd) {
                getNextPasswd ();
            }

            // Perform the required service following the menuInitKey selected
            // item
            if (selectedItemId == idMenuInitKey) {
                initKey ();
            }
        }
    }

    /**
     * Method called by the JCRE, once selected
     * 
     * @param apdu
     *            the incoming APDU object
     */
    public void process(APDU apdu) {
        // ignore the applet select command dispatched to the process
        if (selectingApplet ()) {
            return;
        }
    }

    /**
     * Manage the menuGetNextPasswd selection
     */
    private void getNextPasswd() {
        if (isKeyInitialized) {

            try {
                hash = MessageDigest.getInstance (MessageDigest.ALG_SHA, true);
                hash.doFinal (key, (short) 0, (short) key.length, tmpBuf,
                              (short) 0);
                Util.arrayCopyNonAtomic (tmpBuf, (short) 0, key, (short) 0,
                                         (short) key.length);
                displayHexBuffer (key, (short) key.length);
            } catch (CryptoException e) {
            }
        } else {
            displayText(msgKeyNotInited);
        }
        return;
    }

    /**
     * Manage the menuInitKey selection
     */
    private void initKey() {
        // Get the received envelope
        ProactiveHandler proHdlr = ProactiveHandler.getTheHandler ();
        // Get the response handler
        ProactiveResponseHandler proRespHdlr = ProactiveResponseHandler
                .getTheHandler ();
        // Get input for new key
        proHdlr.initGetInput ((byte) 0x01, DCS_8_BIT_DATA, msgEnterNewKey,
                              (short) 0, (short) msgEnterNewKey.length,
                              KEY_LENGTH_ASCII, KEY_LENGTH_ASCII);

        try {
            proHdlr.send ();
            // Get content of Get Input
            proRespHdlr.copyTextString (tmpBuf, (short) 0);
            // Clean up key array
            Util.arrayFillNonAtomic (key, (short) 0, (short) key.length,
                                     (byte) 0x00);
            
            // FIXME: Not optimal. Need to fix with better solution!
            // Convert ASCII representation from GetInput to hex values
            short i2 = 0;
            short idx1 = 0;
            short idx2 = 0;
            for (short i = 0; i < tmpBuf.length; i++) {
                if (i2 == tmpBuf.length)
                    break;

                // Treat 'a'-'f'and 'A'-'F' cases
                idx1 = i2;
                idx2 = (short) (i2 + (short) 1);
                if (tmpBuf[idx1] >=0x41 && tmpBuf[idx1] <= 0x46)
                    tmpBuf[idx1] = hexNibbles[tmpBuf[idx1] - 0x40];
                if (tmpBuf[idx1] >=0x61 && tmpBuf[idx1] <= 0x66)
                    tmpBuf[idx1] = hexNibbles[tmpBuf[idx1] - 0x60];
                if (tmpBuf[idx2] >=0x41 && tmpBuf[idx2] <= 0x46)
                    tmpBuf[idx2] = hexNibbles[tmpBuf[idx2] - 0x40];
                if (tmpBuf[idx2] >=0x61 && tmpBuf[idx2] <= 0x66)
                    tmpBuf[idx2] = hexNibbles[tmpBuf[idx2] - 0x60];

                key[i] = (byte) (   ((tmpBuf[i2] & 0x0F) << 4) 
                                  | (tmpBuf[(short) (i2 + (short) 1)] & 0x0F));
                i2 += 2;
            }
            isKeyInitialized = true;
        } catch (ToolkitException te) {
            displayText (msgToolkitExp);
        } catch (ArrayIndexOutOfBoundsException aiobe) {
            displayText (msgArrayOutOfBoundExp);
        } catch (Exception e) {
            displayText (msgUnknownErr);
        }
        return;
    }

    private void displayText(byte[] msgBuf) {
        ProactiveHandler proHdlr = ProactiveHandler.getTheHandler ();
        proHdlr.initDisplayText ((byte) 0, DCS_8_BIT_DATA, msgBuf, (short) 0,
                                 (short) msgBuf.length);
        proHdlr.send ();
    }

    private void displayHexBuffer(byte[] buffer, short msgLength) {
        short aSize = (short) (msgLength * (short) 2);
        byte[] tmp = new byte[aSize];

        short inc = 0;
        byte lsb = 0;
        byte msb = 0;
        for (short i = 0; i < msgLength; i++) {
            lsb = (byte) ((buffer[i] >> 4) & 0x0F);
            msb = (byte) (buffer[i] & 0x0F);
            if (lsb >= 0xa && lsb <= 0xf) 
                lsb += 0x37;
            else
                lsb += 0x30;
            if (msb >= 0xa && msb <= 0xf)
                msb += 0x37;
            else
                msb += 0x30;
            tmp[(short) (i + inc)] = lsb;
            tmp[(short) (i + inc + (short) 1)] = msb;
            inc++;
        }

        ProactiveHandler proHdlr = ProactiveHandler.getTheHandler ();
        proHdlr.initDisplayText ((byte) 0, DCS_8_BIT_DATA, tmp, (short) 0,
                                 (short) tmp.length);
        proHdlr.send ();
    }
}
