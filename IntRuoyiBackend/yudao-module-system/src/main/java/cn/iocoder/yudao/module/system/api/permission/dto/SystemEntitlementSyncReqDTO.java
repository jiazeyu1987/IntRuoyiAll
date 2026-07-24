package cn.iocoder.yudao.module.system.api.permission.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class SystemEntitlementSyncReqDTO {

    private Long tenantId;

    private String sourceType;

    private String sourceKey;

    private String sourceVersion;

    private String sourceDigest;

    private String policyCode;

    private Set<Long> resolvedUserIds;

    private Long operatorUserId;

    private String operatorUsername;

}
