package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Agency;
import org.rutebanken.netex.model.Authority;

import static org.opentripplanner.netex.support.NetexObjectDecorator.withOptional;

/**
 * NeTEx authority is mapped to OTP agency. An authority is defined as "A company or organisation which is responsible
 * for the establishment of a public transport service." In NeTEx this is not the same as an operator. A default
 * authority can be created if none is present.
 */
class AuthorityToAgencyMapper {

    /** private to prevent creating new instance of utility class with static methods only */
    private AuthorityToAgencyMapper() {}

    /**
     * Map authority and time zone to OTP agency.
     */
    static Agency mapAuthority(Authority source, String timeZone){
        Agency target = new Agency();

        target.setId(source.getId());
        target.setName(source.getName().getValue());
        target.setTimezone(timeZone);

        withOptional(source.getContactDetails(), c -> {
            target.setUrl(c.getUrl());
            target.setPhone(c.getPhone());
        });
        return target;
    }

    /**
     * Create a new dummy agency with time zone set. All other values are set to
     * "N/A" and id set to {@code "Dummy-" + timeZone}.
     */
    static Agency createDummyAgency(String timeZone){
        Agency agency = new Agency();
        agency.setId("Dummy-" + timeZone);
        agency.setName("N/A");
        agency.setTimezone(timeZone);
        agency.setUrl("N/A");
        agency.setPhone("N/A");
        return agency;
    }
}
