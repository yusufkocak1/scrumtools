package com.scrumtools.service.scm.client;

import com.scrumtools.entity.ScmConnection;
import com.scrumtools.entity.UserScmAccount;
import com.scrumtools.entity.enums.ScmProvider;
import com.scrumtools.service.scm.ScmTokenCrypto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Bağlantı/hesap kaydından token'ı çözüp uygun ScmClient üretir.
 * Client'lar durumsuz ve hafiftir — her çağrıda yeni instance oluşturmak sorun değil.
 */
@Component
@RequiredArgsConstructor
public class ScmClientFactory {

    private final ScmTokenCrypto tokenCrypto;

    public ScmClient forConnection(ScmConnection connection) {
        return create(connection.getProvider(), connection.getBaseUrl(),
                tokenCrypto.decrypt(connection.getEncryptedToken()));
    }

    public ScmClient forUserAccount(UserScmAccount account) {
        return create(account.getProvider(), account.getBaseUrl(),
                tokenCrypto.decrypt(account.getEncryptedToken()));
    }

    /** Henüz kaydedilmemiş (düz) token ile — bağlantı eklerken ilk doğrulama için. */
    public ScmClient forRawToken(ScmProvider provider, String baseUrl, String token) {
        return create(provider, baseUrl, token);
    }

    private ScmClient create(ScmProvider provider, String baseUrl, String token) {
        return switch (provider) {
            case GITHUB -> new GitHubClient(baseUrl, token);
            case GITLAB -> new GitLabClient(baseUrl, token);
            // Faz 3: BitbucketClient (Basic auth + workspace), GiteaClient
            case BITBUCKET, GITEA -> throw new IllegalArgumentException(
                    "Bu sağlayıcı desteği henüz aktif değil: " + provider);
        };
    }
}
