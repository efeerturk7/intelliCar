package com.efeerturk.intelliCar.notification;

import com.efeerturk.intelliCar.enums.OfferStatus;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * E-posta konu ve metinleri. Spring'e bağımlı değildir, tek başına test edilebilir.
 *
 * Metinler düz yazıdır (HTML yok), bu yüzden kullanıcı girdisi (marka/model) içinde HTML enjeksiyonu riski yoktur.
 * Konu satırına giren değerler tek satıra indirilir; satır sonu karakterleriyle başlık (header) enjeksiyonu yapılamaz.
 */
public final class EmailTemplates {

    private static final Locale TR = Locale.forLanguageTag("tr-TR");
    private static final int MAX_TEXT_LENGTH = 80;

    private EmailTemplates() {
    }

    public record EmailContent(String subject, String body) {
    }

    /** Satıcıya: ilanına yeni teklif geldi. */
    public static EmailContent offerReceived(String sellerName, String carTitle, BigDecimal offeredPrice) {
        String title = singleLine(carTitle);
        String body = """
                %s

                "%s" ilanınıza yeni bir teklif geldi.

                Teklif edilen fiyat: %s TL

                Teklifi kabul etmek veya reddetmek için intelliCar'a giriş yapabilirsiniz.

                intelliCar
                """.formatted(greeting(sellerName), title, money(offeredPrice));
        return new EmailContent("Yeni teklif: " + title, body);
    }

    /** Alıcıya: teklifi kabul edildi / reddedildi. */
    public static EmailContent offerResponded(String buyerName, String carTitle, BigDecimal offeredPrice, OfferStatus status) {
        String title = singleLine(carTitle);

        String subject;
        String outcome;
        if (status == OfferStatus.ACCEPTED) {
            subject = "Teklifiniz kabul edildi: " + title;
            outcome = "Satıcı teklifinizi kabul etti.";
        } else if (status == OfferStatus.REJECTED) {
            subject = "Teklifiniz reddedildi: " + title;
            outcome = "Satıcı teklifinizi reddetti. Dilerseniz farklı bir fiyatla yeni bir teklif verebilirsiniz.";
        } else {
            subject = "Teklifiniz güncellendi: " + title;
            outcome = "Teklifinizin durumu güncellendi: " + status + ".";
        }

        String body = """
                %s

                "%s" ilanı için verdiğiniz %s TL tutarındaki teklif hakkında bir güncelleme var.

                %s

                intelliCar
                """.formatted(greeting(buyerName), title, money(offeredPrice), outcome);
        return new EmailContent(subject, body);
    }

    private static String greeting(String name) {
        String clean = singleLine(name);
        return clean.isEmpty() ? "Merhaba," : "Merhaba " + clean + ",";
    }

    /** 1.050.000 gibi Türkçe biçimde. */
    private static String money(BigDecimal price) {
        return price == null ? "-" : String.format(TR, "%,.0f", price);
    }

    /** Satır sonu ve kontrol karakterlerini temizler, boşlukları sadeleştirir ve uzun metni kısaltır. */
    static String singleLine(String text) {
        if (text == null) {
            return "";
        }
        String s = text.replaceAll("[\\p{Cntrl}\\s]+", " ").trim();
        return s.length() > MAX_TEXT_LENGTH ? s.substring(0, MAX_TEXT_LENGTH) + "…" : s;
    }
}
