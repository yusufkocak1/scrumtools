package com.scrumtools.service.ci.client;

import com.scrumtools.entity.CiConnection;
import com.scrumtools.entity.enums.CiProvider;
import com.scrumtools.service.scm.ScmTokenCrypto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Bağlantı kaydından token'ı çözüp uygun CiClient üretir.
 * Client'lar durumsuz ve hafiftir — çağrı başına yeni instance sorun değil.
 * Token şifrelemesi SCM ile aynı anahtarı kullanır (ScmTokenCrypto).
 */
@Component
@RequiredArgsConstructor
public class CiClientFactory {

    private final ScmTokenCrypto tokenCrypto;

    public CiClient forConnection(CiConnection connection) {
        return create(connection.getProvider(), connection.getBaseUrl(), connection.getUsername(),
                tokenCrypto.decrypt(connection.getEncryptedToken()),
                Boolean.TRUE.equals(connection.getCrumbEnabled()));
    }

    /** Henüz kaydedilmemiş (düz) token ile — bağlantı eklerken ilk doğrulama için. */
    public CiClient forRawToken(CiProvider provider, String baseUrl, String username, String token) {
        return create(provider, baseUrl, username, token, false);
    }

    private CiClient create(CiProvider provider, String baseUrl, String username,
                            String token, boolean crumbEnabled) {
        return switch (provider) {
            case JENKINS -> new JenkinsClient(baseUrl, username, token, crumbEnabled);
        };
    }
}
