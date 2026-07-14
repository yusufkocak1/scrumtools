package com.scrumtools.service.scm;

import com.scrumtools.dto.UserScmAccountRequest;
import com.scrumtools.dto.UserScmAccountResponse;
import com.scrumtools.entity.User;
import com.scrumtools.entity.UserScmAccount;
import com.scrumtools.entity.enums.ScmConnectionStatus;
import com.scrumtools.repository.UserRepository;
import com.scrumtools.repository.UserScmAccountRepository;
import com.scrumtools.service.scm.client.ScmClient;
import com.scrumtools.service.scm.client.ScmClientFactory;
import com.scrumtools.service.scm.client.ScmUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Kullanıcının kişisel SCM hesapları (PAT). Org bağlantısından bağımsızdır;
 * branch açarken varsa kullanıcının tokenı kullanılır (işlem sağlayıcıda
 * kullanıcının adına görünür). Aynı provider+baseUrl için tek kayıt tutulur —
 * tekrar bağlanırsa token güncellenir (upsert).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserScmAccountService {

    private final UserScmAccountRepository accountRepository;
    private final UserRepository userRepository;
    private final ScmTokenCrypto tokenCrypto;
    private final ScmUrlValidator urlValidator;
    private final ScmClientFactory clientFactory;

    @Transactional(readOnly = true)
    public List<UserScmAccountResponse> list(String email) {
        return accountRepository.findByUserEmail(email).stream()
                .map(UserScmAccountResponse::from)
                .toList();
    }

    @Transactional
    public UserScmAccountResponse connect(String email, UserScmAccountRequest request) {
        requireCryptoConfigured();
        if (request.provider() == null) throw new IllegalArgumentException("Sağlayıcı seçilmeli.");
        if (isBlank(request.token())) throw new IllegalArgumentException("Erişim token'ı (PAT) zorunlu.");
        urlValidator.validate(request.baseUrl());

        String token = request.token().trim();
        String baseUrl = normalizeBaseUrl(request.baseUrl());

        // Token kaydedilmeden önce sağlayıcıda doğrulanır
        ScmClient client = clientFactory.forRawToken(request.provider(), baseUrl, token);
        ScmUserInfo userInfo = client.validateToken();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));

        UserScmAccount account = accountRepository
                .findExisting(user.getId(), request.provider(), baseUrl)
                .orElseGet(() -> UserScmAccount.builder()
                        .user(user)
                        .provider(request.provider())
                        .baseUrl(baseUrl)
                        .build());

        account.setEncryptedToken(tokenCrypto.encrypt(token));
        account.setTokenHint(tokenCrypto.hint(token));
        account.setUsername(userInfo.username());
        account.setProviderEmail(userInfo.email());
        account.setStatus(ScmConnectionStatus.ACTIVE);

        account = accountRepository.save(account);
        log.info("Kişisel SCM hesabı bağlandı: {} → {} ({})",
                email, request.provider(), userInfo.username());
        return UserScmAccountResponse.from(account);
    }

    @Transactional
    public void delete(String email, UUID accountId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
        UserScmAccount account = accountRepository.findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("SCM hesabı bulunamadı."));
        accountRepository.delete(account);
        log.info("Kişisel SCM hesabı silindi: {} → {}", email, account.getProvider());
    }

    private void requireCryptoConfigured() {
        if (!tokenCrypto.isEnabled()) {
            throw new IllegalStateException(
                    "Git entegrasyonu bu sunucuda yapılandırılmamış (SCM_CRYPTO_KEY tanımlı değil).");
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (isBlank(baseUrl)) return null;
        return baseUrl.trim().replaceAll("/+$", "");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
