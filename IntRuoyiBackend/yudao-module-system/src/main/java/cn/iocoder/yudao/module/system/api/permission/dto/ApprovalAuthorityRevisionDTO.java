package cn.iocoder.yudao.module.system.api.permission.dto;

public record ApprovalAuthorityRevisionDTO(Long tenantId, Long revision) {
    public ApprovalAuthorityRevisionDTO {
        if (tenantId == null || tenantId <= 0 || revision == null || revision <= 0) {
            throw new IllegalArgumentException("APPROVAL_AUTHORITY_REVISION_INVALID");
        }
    }
}
