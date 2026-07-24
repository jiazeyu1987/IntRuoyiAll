package cn.iocoder.yudao.module.infra.framework.file.core.client;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;

/**
 * 存储侧 Retention/Object Lock/legal hold 证据。
 */
@Data
@Accessors(chain = true)
public class StorageRetentionEvidence implements Serializable {

    private Long fileId;
    private Long clientId;
    private String provider;
    private String storageType;
    private String bucket;
    private String path;
    private String key;
    private String url;
    private String objectVersionId;
    private String retentionMode;
    private Instant retainUntil;
    private String legalHoldStatus;
    private Instant verifiedAt;
    private String checksumSha256;
    private String eTag;

}
