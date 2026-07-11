package com.scrumtools.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * iyzico iyzilink API entegrasyonu (iyzico.enabled=true iken aktif).
 *
 * Endpoint'ler:
 *   POST   /v2/iyzilink/products          → link oluştur (data.token, data.url)
 *   GET    /v2/iyzilink/products/{token}  → link detayı/durumu
 *   DELETE /v2/iyzilink/products/{token}  → linki kaldır
 *
 * NOT: Alan adları ve tek-kullanımlık stok semantiği canlıya çıkmadan önce
 * güncel iyzico dokümanı + sandbox ile doğrulanmalı.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "iyzico.enabled", havingValue = "true")
public class IyzilinkPaymentProvider implements PaymentProviderService {

    private static final String IYZILINK_PATH = "/v2/iyzilink/products";

    private final String apiKey;
    private final String secretKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IyzilinkPaymentProvider(
            @Value("${iyzico.api-key}") String apiKey,
            @Value("${iyzico.secret-key}") String secretKey,
            @Value("${iyzico.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public PaymentLink createPaymentLink(CreateLinkCommand command) {
        try {
            // Tek kullanımlık link: stockEnabled + stockCount=1 (ilk ödemede tükenir)
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("conversationId", command.conversationId());
            body.put("name", command.name());
            body.put("description", command.description());
            body.put("price", command.price().toPlainString());
            body.put("currencyCode", "TRY");
            body.put("addressIgnorable", true);
            body.put("installmentRequested", false);
            body.put("stockEnabled", true);
            body.put("stockCount", 1);
            body.put("sourceType", "API");

            String json = objectMapper.writeValueAsString(body);
            Map<?, ?> response = exchange("POST", IYZILINK_PATH, json);

            Map<?, ?> data = asMap(response.get("data"));
            String token = data != null ? String.valueOf(data.get("token")) : null;
            String url = data != null ? String.valueOf(data.get("url")) : null;
            if (token == null || url == null || "null".equals(token)) {
                throw new IllegalStateException("iyzilink cevabında token/url yok: " + response);
            }
            log.info("iyzilink oluşturuldu: conversationId={} token={}", command.conversationId(), token);
            return new PaymentLink(token, url);
        } catch (Exception e) {
            log.error("iyzilink oluşturulamadı: {}", e.getMessage());
            throw new IllegalStateException("Ödeme linki oluşturulamadı. Lütfen tekrar deneyin.", e);
        }
    }

    @Override
    public LinkStatus getLinkStatus(String token) {
        try {
            Map<?, ?> response = exchange("GET", IYZILINK_PATH + "/" + token, "");
            Map<?, ?> data = asMap(response.get("data"));
            // soldCount >= 1 → tek stoklu link satılmış demektir.
            // (Alan adını iyzico dokümanından doğrula; bazı sürümlerde "soldLimit"/"stockCount" birlikte döner.)
            boolean paid = false;
            String paymentId = null;
            if (data != null) {
                Object soldCount = data.get("soldCount");
                if (soldCount instanceof Number n && n.intValue() >= 1) paid = true;
                Object status = data.get("productStatus");
                if ("PASSIVE".equals(String.valueOf(status)) && data.get("soldCount") == null) {
                    // stok tükenince pasifleşen linkler için ikincil sinyal
                    paid = true;
                }
                Object pid = data.get("paymentId");
                if (pid != null) paymentId = String.valueOf(pid);
            }
            return new LinkStatus(paid, paymentId, String.valueOf(response));
        } catch (Exception e) {
            log.warn("iyzilink durumu sorgulanamadı (token={}): {}", token, e.getMessage());
            return new LinkStatus(false, null, null);
        }
    }

    @Override
    public void disableLink(String token) {
        try {
            exchange("DELETE", IYZILINK_PATH + "/" + token, "");
            log.info("iyzilink devre dışı bırakıldı: {}", token);
        } catch (Exception e) {
            log.warn("iyzilink devre dışı bırakılamadı (token={}): {}", token, e.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private Map<?, ?> exchange(String method, String path, String body) {
        String randomKey = IyzicoAuthUtil.generateRandomKey();
        String authHeader = IyzicoAuthUtil.authorizationHeader(apiKey, secretKey, randomKey, path, body);

        RestClient.RequestBodySpec spec = restClient
                .method(org.springframework.http.HttpMethod.valueOf(method))
                .uri(path)
                .header("Authorization", authHeader)
                .header("x-iyzi-rnd", randomKey)
                .contentType(MediaType.APPLICATION_JSON);

        if (body != null && !body.isEmpty()) {
            spec = (RestClient.RequestBodySpec) spec.body(body);
        }
        Map<?, ?> response = spec.retrieve().body(Map.class);

        if (response == null || !"success".equalsIgnoreCase(String.valueOf(response.get("status")))) {
            throw new IllegalStateException("iyzico hata döndü: " +
                    (response != null ? response.get("errorMessage") : "boş cevap"));
        }
        return response;
    }

    private Map<?, ?> asMap(Object o) {
        return o instanceof Map<?, ?> m ? m : null;
    }
}
