package cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class GxpAuditEventRelationRespVO {

    private Long eventId;
    private String relationType;
    private String targetType;
    private String targetId;
    private String targetVersion;
    private String targetHash;
    @JsonSerialize(using = GxpAuditUtcTimestampSerializer.class)
    private LocalDateTime createdAtUtc;
}
