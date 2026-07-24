package cn.iocoder.yudao.module.dcc.signature.service.retention;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SignatureGovernanceRetentionS3Properties.class)
public class SignatureGovernanceRetentionS3Configuration {

    @Bean
    @ConditionalOnProperty(prefix = "signature.governance.retention.s3", name = "enabled", havingValue = "true")
    public SignatureGovernanceRetentionObjectStore signatureGovernanceRetentionObjectStore(
            SignatureGovernanceRetentionS3Properties properties) {
        validate(properties);
        return new SignatureGovernanceRetentionS3ObjectStore(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "signature.governance.retention.s3", name = "enabled", havingValue = "true")
    public SignatureGovernanceRetentionVerificationService signatureGovernanceRetentionVerificationService(
            SignatureGovernanceRetentionS3Properties properties,
            SignatureGovernanceRetentionObjectStore objectStore) {
        validate(properties);
        return new SignatureGovernanceRetentionObjectStoreVerificationService(properties, objectStore);
    }

    private static void validate(SignatureGovernanceRetentionS3Properties properties) {
        if (isBlank(properties.getEndpoint())) {
            throw new IllegalStateException("signature.governance.retention.s3.endpoint is required");
        }
        if (isBlank(properties.getBucketName())) {
            throw new IllegalStateException("signature.governance.retention.s3.bucket-name is required");
        }
        if (isBlank(properties.getRegion())) {
            throw new IllegalStateException("signature.governance.retention.s3.region is required");
        }
        if (isBlank(properties.getAccessKey())) {
            throw new IllegalStateException("signature.governance.retention.s3.access-key is required");
        }
        if (isBlank(properties.getSecretKey())) {
            throw new IllegalStateException("signature.governance.retention.s3.secret-key is required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
