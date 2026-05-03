package com.microservices.payment_service.service;

import com.iyzipay.Options;
import com.iyzipay.model.*;
import com.iyzipay.request.CreatePaymentRequest;
import com.iyzipay.request.CreateRefundRequest;
import com.microservices.payment_service.config.IyzicoConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * iyzico resmi Java SDK (iyzipay-java 2.0.141) üzerinden ödeme işlemleri.
 * İmzalama, PKI string üretimi tamamen SDK tarafından yönetilir.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IyzicoHttpClient {

    private final IyzicoConfig config;

    private Options buildOptions() {
        Options options = new Options();
        options.setApiKey(config.getApiKey());
        options.setSecretKey(config.getSecretKey());
        options.setBaseUrl(config.getBaseUrl());
        return options;
    }

    // ──────────────────────────────────────────────
    // ÖDEME OLUŞTUR  →  /payment/auth
    // ──────────────────────────────────────────────
    public IyzicoPaymentResult createPayment(Long orderId, Long customerId, BigDecimal amount,
                                              String cardNumber, String cardHolder,
                                              String expireMonth, String expireYear, String cvc) {
        String amountStr = formatAmount(amount);
        String conversationId = orderId.toString();

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setLocale(Locale.TR.getValue());
        request.setConversationId(conversationId);
        request.setPrice(new BigDecimal(amountStr));
        request.setPaidPrice(new BigDecimal(amountStr));
        request.setCurrency(Currency.TRY.name());
        request.setInstallment(1);
        request.setBasketId("ORDER-" + orderId);
        request.setPaymentChannel(PaymentChannel.WEB.name());
        request.setPaymentGroup(PaymentGroup.PRODUCT.name());

        PaymentCard paymentCard = new PaymentCard();
        paymentCard.setCardHolderName(cardHolder);
        paymentCard.setCardNumber(cardNumber);
        paymentCard.setExpireMonth(expireMonth);
        paymentCard.setExpireYear(expireYear);
        paymentCard.setCvc(cvc);
        paymentCard.setRegisterCard(0);
        request.setPaymentCard(paymentCard);

        Buyer buyer = new Buyer();
        buyer.setId(customerId.toString());
        buyer.setName("Marketplace");
        buyer.setSurname("Customer");
        buyer.setEmail("customer-" + customerId + "@marketplace.com");
        buyer.setIdentityNumber("74300864791");
        buyer.setLastLoginDate("2025-01-01 10:00:00");
        buyer.setRegistrationDate("2024-01-01 10:00:00");
        buyer.setRegistrationAddress("Marketplace Platform");
        buyer.setIp("127.0.0.1");
        buyer.setCity("Istanbul");
        buyer.setCountry("Turkey");
        buyer.setZipCode("34000");
        request.setBuyer(buyer);

        Address address = new Address();
        address.setContactName("Marketplace Customer");
        address.setCity("Istanbul");
        address.setCountry("Turkey");
        address.setAddress("Marketplace Platform");
        address.setZipCode("34000");
        request.setShippingAddress(address);
        request.setBillingAddress(address);

        BasketItem basketItem = new BasketItem();
        basketItem.setId("ORDER-" + orderId);
        basketItem.setName("Siparis #" + orderId);
        basketItem.setCategory1("Genel");
        basketItem.setItemType(BasketItemType.PHYSICAL.name());
        basketItem.setPrice(new BigDecimal(amountStr));
        List<BasketItem> basketItems = new ArrayList<>();
        basketItems.add(basketItem);
        request.setBasketItems(basketItems);

        log.info("iyzico ödeme isteği — orderId: {}, amount: {}", orderId, amountStr);

        Payment payment = Payment.create(request, buildOptions());

        log.info("iyzico yanıt — status: {}, paymentId: {}, error: {}",
                payment.getStatus(), payment.getPaymentId(), payment.getErrorMessage());

        boolean success = "success".equalsIgnoreCase(payment.getStatus());
        String transactionId = null;
        if (success && payment.getPaymentItems() != null && !payment.getPaymentItems().isEmpty()) {
            transactionId = payment.getPaymentItems().get(0).getPaymentTransactionId();
        }

        return new IyzicoPaymentResult(success, payment.getPaymentId(), transactionId,
                success ? null : payment.getErrorMessage());
    }

    // ──────────────────────────────────────────────
    // İADE  →  /payment/refund
    // ──────────────────────────────────────────────
    public IyzicoRefundResult refundPayment(String paymentTransactionId, BigDecimal amount, String conversationId) {
        CreateRefundRequest request = new CreateRefundRequest();
        request.setLocale(Locale.TR.getValue());
        request.setConversationId(conversationId);
        request.setPaymentTransactionId(paymentTransactionId);
        request.setPrice(amount.setScale(2, RoundingMode.HALF_UP));
        request.setCurrency(Currency.TRY.name());
        request.setIp("127.0.0.1");

        Refund refund = Refund.create(request, buildOptions());

        boolean success = "success".equalsIgnoreCase(refund.getStatus());
        return new IyzicoRefundResult(success, refund.getPaymentTransactionId(),
                success ? null : refund.getErrorMessage());
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public record IyzicoPaymentResult(
            boolean success,
            String paymentId,
            String transactionId,
            String errorMessage
    ) {}

    public record IyzicoRefundResult(
            boolean success,
            String transactionId,
            String errorMessage
    ) {}
}
