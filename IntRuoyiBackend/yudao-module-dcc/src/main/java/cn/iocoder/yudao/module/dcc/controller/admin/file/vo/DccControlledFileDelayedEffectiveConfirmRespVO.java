package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder
public class DccControlledFileDelayedEffectiveConfirmRespVO {
    @JsonFormat(shape=JsonFormat.Shape.STRING) private Long controlledFileId;
    private String taskId;
    private String processInstanceId;
    private Integer attributeRound;
    private String processDefinitionId;
    private String definitionDigest;
    private String versionNo;
    private String effectiveMode;
    private String stageCode;
    private Boolean canConfirm;
    private String unavailableReason;
    private String timeZone;
    private LocalDateTime serverTime;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="Asia/Shanghai") private LocalDateTime selectedEffectiveTime;
    @JsonFormat(shape=JsonFormat.Shape.STRING) private Long signatureId;
    private String evidenceHash;
    private LocalDateTime signedAt;
    private String status;
}
