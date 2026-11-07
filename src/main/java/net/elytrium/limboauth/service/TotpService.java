package net.elytrium.limboauth.service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import net.elytrium.limboauth.Settings;

public final class TotpService {

    private static final CodeVerifier CODE_VERIFIER =
            new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
    private static final SecretGenerator SECRET_GENERATOR = new DefaultSecretGenerator();
    private static final RecoveryCodeGenerator RECOVERY_GENERATOR = new RecoveryCodeGenerator();

    private TotpService() {
    }

    public static boolean isValidCode(String secret, String code) {
        return CODE_VERIFIER.isValidCode(secret, code);
    }

    public static String generateSecret() {
        return SECRET_GENERATOR.generate();
    }

    public static String generateRecoveryCodes() {
        return String.join(", ", RECOVERY_GENERATOR.generateCodes(Settings.IMP.MAIN.TOTP_RECOVERY_CODES_AMOUNT));
    }

    public static String buildQrUrl(String username, String secret) {
        QrData data = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer(Settings.IMP.MAIN.TOTP_ISSUER)
                .build();
        return Settings.IMP.MAIN.QR_GENERATOR_URL.replace(
                "{data}", URLEncoder.encode(data.getUri(), StandardCharsets.UTF_8));
    }
}