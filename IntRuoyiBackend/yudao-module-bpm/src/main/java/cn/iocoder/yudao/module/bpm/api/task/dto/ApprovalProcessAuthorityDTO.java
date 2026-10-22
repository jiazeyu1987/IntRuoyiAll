package cn.iocoder.yudao.module.bpm.api.task.dto;

/** Server-created formal start authority. Never exposed as an HTTP request field. */
public record ApprovalProcessAuthorityDTO(Long tenantId, String projectionCode, String authorityType,
        String authorityId, String authorityVersion, String writerCode, String expectedBusinessKey) {
}
