package cn.iocoder.yudao.module.system.service.permission.bo;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class SystemEntitlementSyncCommand {

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
