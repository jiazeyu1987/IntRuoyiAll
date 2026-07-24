package cn.iocoder.yudao.module.dcc.service.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccElectronicSignatureFailureAuditCommand {

    private Long targetUserId;
    private Long controlledFileId;
    private Long revisionId;
    private String taskId;
    private String actionType;
    private String meaningCode;
    private String failureMessage;
    private LocalDateTime failedAt;
    private String remoteIp;
    private String userAgent;

}
