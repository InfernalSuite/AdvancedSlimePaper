package com.infernalsuite.aswm.api.utils;

/**
 * Class containing some standards of the SRF.
 */
public class SlimeFormat {

    /** First bytes of every SRF file **/
    public static final byte[] SLIME_HEADER = new byte[] { -79, 11 };

    /** Latest version of the SRF that SWM supports **/
    public static final byte SLIME_VERSION = 12;
}
