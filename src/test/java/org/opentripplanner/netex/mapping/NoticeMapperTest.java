package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Notice;

import static org.junit.Assert.*;

public class NoticeMapperTest {

    private static final String NOTICE_ID = "RUT:Notice:1";
    private static final String NOTICE_TEXT = "Something is happening on this line";
    private static final String PUBLIC_CODE = "Public Code";

    @Test public void mapNotice() {
        Notice netexNotice = new Notice();
        netexNotice.setId(NOTICE_ID);
        netexNotice.setText(new MultilingualString().withValue(NOTICE_TEXT));
        netexNotice.setPublicCode(PUBLIC_CODE);

        org.opentripplanner.model.Notice otpNotice = NoticeMapper.map(netexNotice);

        assertEquals(NOTICE_ID, otpNotice.getId().getId());
        assertEquals(NOTICE_TEXT, otpNotice.getText());
        assertEquals(PUBLIC_CODE, otpNotice.getPublicCode());
    }
}
