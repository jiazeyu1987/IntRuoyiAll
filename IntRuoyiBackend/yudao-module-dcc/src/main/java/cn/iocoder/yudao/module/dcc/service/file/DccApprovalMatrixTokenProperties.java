package cn.iocoder.yudao.module.dcc.service.file;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Dedicated technical confirmation key. No embedded production value and no electronic-signature reuse. */
@Data
@Component
@ConfigurationProperties(prefix="dcc.approval-matrix.baseline-token")
public class DccApprovalMatrixTokenProperties {
    private String signingSecret;
    public byte[] requireSigningKey(){
        if(signingSecret==null || signingSecret.isBlank())
            throw new IllegalStateException("dcc.approval-matrix.baseline-token.signing-secret is required");
        byte[] key=signingSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if(key.length<32)throw new IllegalStateException("matrix baseline signing key must contain at least 32 UTF-8 bytes");
        return key;
    }
}
