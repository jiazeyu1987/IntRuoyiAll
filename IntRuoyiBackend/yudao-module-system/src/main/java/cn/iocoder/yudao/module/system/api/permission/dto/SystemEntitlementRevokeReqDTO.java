package cn.iocoder.yudao.module.system.api.permission.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemEntitlementRevokeReqDTO {

    private Long tenantId;

    private String sourceType;

    private String sourceKey;

    private String policyCode;

    private Long operatorUserId;

    private String operatorUsername;

}
