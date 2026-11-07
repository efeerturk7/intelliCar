package com.efeerturk.intelliCar.notification;

import com.efeerturk.intelliCar.event.OfferEvent;
import com.efeerturk.intelliCar.model.User;
import com.efeerturk.intelliCar.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Teklif olaylarını e-postaya çevirir. Kafka consumer'ı tarafından çağrılır.
 *
 * Event'ler e-posta adresi taşımaz (yalnızca kullanıcı id'si); adres, gönderim anında veritabanından okunur.
 * Gönderim başarısız olursa istisna yukarı fırlatılır, Kafka hata yöneticisi (KafkaConfig) kaydı yeniden dener.
 */
@Slf4j
@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final boolean enabled;
    private final String from;

    public EmailNotificationService(
            JavaMailSender mailSender,
            UserRepository userRepository,
            @Value("${intellicar.notification.email.enabled:true}") boolean enabled,
            @Value("${intellicar.notification.email.from:no-reply@intellicar.local}") String from
    ) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
        this.enabled = enabled;
        this.from = from;
    }

    public void notifySellerOfNewOffer(OfferEvent event) {
        findUser(event.sellerId()).ifPresent(seller ->
                send(seller, EmailTemplates.offerReceived(
                        seller.getFirstName(), event.carTitle(), event.offeredPrice())));
    }

    public void notifyBuyerOfDecision(OfferEvent event) {
        findUser(event.buyerId()).ifPresent(buyer ->
                send(buyer, EmailTemplates.offerResponded(
                        buyer.getFirstName(), event.carTitle(), event.offeredPrice(), event.status())));
    }

    private java.util.Optional<User> findUser(UUID userId) {
        if (userId == null) {
            log.warn("Bildirim atlandı: olayda kullanıcı id'si yok");
            return java.util.Optional.empty();
        }
        java.util.Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            // Kullanıcı silinmiş olabilir. Yeniden denemenin anlamı yok, sadece atla.
            log.warn("Bildirim atlandı: kullanıcı bulunamadı. userId={}", userId);
        }
        return user;
    }

    private void send(User to, EmailTemplates.EmailContent content) {
        if (!enabled) {
            log.info("E-posta bildirimi kapalı, gönderilmedi. userId={}", to.getId());
            return;
        }
        if (to.getEmail() == null || to.getEmail().isBlank()) {
            log.warn("Bildirim atlandı: kullanıcının e-postası yok. userId={}", to.getId());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to.getEmail());
        message.setSubject(content.subject());
        message.setText(content.body());

        mailSender.send(message);
        // E-posta adresi kişisel veri olduğu için loga yazılmaz, sadece kullanıcı id'si.
        log.info("E-posta gönderildi. userId={}, konu={}", to.getId(), content.subject());
    }
}
