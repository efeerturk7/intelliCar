package com.efeerturk.intelliCar.notification;

import com.efeerturk.intelliCar.enums.OfferStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplatesTest {

    @Test
    void offerReceived_formatsPriceTheTurkishWay() {
        EmailTemplates.EmailContent mail =
                EmailTemplates.offerReceived("Ali", "Toyota Corolla", new BigDecimal("1050000"));

        assertThat(mail.subject()).isEqualTo("Yeni teklif: Toyota Corolla");
        assertThat(mail.body()).contains("Merhaba Ali,").contains("1.050.000 TL");
    }

    @Test
    void subject_cannotBeUsedForHeaderInjection() {
        EmailTemplates.EmailContent mail =
                EmailTemplates.offerReceived("Ali", "Toyota\r\nBcc: attacker@evil.com", BigDecimal.TEN);

        assertThat(mail.subject()).doesNotContain("\r").doesNotContain("\n");
    }

    @Test
    void veryLongCarTitle_isShortened() {
        EmailTemplates.EmailContent mail =
                EmailTemplates.offerReceived("Ali", "x".repeat(300), BigDecimal.ONE);

        assertThat(mail.subject().length()).isLessThan(120);
    }

    @Test
    void acceptedOffer_usesAcceptedWording() {
        EmailTemplates.EmailContent mail =
                EmailTemplates.offerResponded("Ayşe", "Honda Civic", new BigDecimal("850000"), OfferStatus.ACCEPTED);

        assertThat(mail.subject()).isEqualTo("Teklifiniz kabul edildi: Honda Civic");
        assertThat(mail.body()).contains("kabul etti").contains("850.000 TL");
    }

    @Test
    void rejectedOffer_usesRejectedWording() {
        EmailTemplates.EmailContent mail =
                EmailTemplates.offerResponded("Ayşe", "Ford Focus", new BigDecimal("500000"), OfferStatus.REJECTED);

        assertThat(mail.subject()).isEqualTo("Teklifiniz reddedildi: Ford Focus");
        assertThat(mail.body()).contains("reddetti");
    }

    @Test
    void blankNameAndMissingValues_doNotBreakTheTemplate() {
        EmailTemplates.EmailContent mail = EmailTemplates.offerResponded("  ", "Kia Ceed", null, null);

        assertThat(mail.body()).startsWith("Merhaba,");
        assertThat(mail.subject()).contains("Kia Ceed");
    }
}
