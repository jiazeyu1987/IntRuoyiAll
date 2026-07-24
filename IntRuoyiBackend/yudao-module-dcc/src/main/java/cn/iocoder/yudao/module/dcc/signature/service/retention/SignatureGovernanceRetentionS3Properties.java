package cn.iocoder.yudao.module.dcc.signature.service.retention;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "signature.governance.retention.s3")
public class SignatureGovernanceRetentionS3Properties {

    private boolean enabled;
    private String endpoint;
    private String bucketName;
    private String region = "us-east-1";
    private String accessKey;
    private String secretKey;
    private boolean pathStyleAccess = true;
}
