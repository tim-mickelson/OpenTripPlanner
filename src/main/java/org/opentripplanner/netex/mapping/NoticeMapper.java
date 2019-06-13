package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Notice;

import static org.opentripplanner.netex.mapping.FeedScopedIdFactory.createFeedScopedId;

class NoticeMapper {

    Notice mapNotice(org.rutebanken.netex.model.Notice netexNotice){
        Notice otpNotice = new Notice();

        otpNotice.setId(createFeedScopedId(netexNotice.getId()));
        otpNotice.setText(netexNotice.getText().getValue());
        otpNotice.setPublicCode(netexNotice.getPublicCode());

        return otpNotice;
    }
}
