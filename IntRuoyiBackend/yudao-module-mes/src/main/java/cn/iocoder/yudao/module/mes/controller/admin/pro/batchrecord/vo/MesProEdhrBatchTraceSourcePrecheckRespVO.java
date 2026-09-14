package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Formal, server-read source snapshot consumed by the four-material gate.
 * The identifiers and hashes are loaded from persisted Origin/TraceLink rows.
 */
@Data
@Accessors(chain = true)
public class MesProEdhrBatchTraceSourcePrecheckRespVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long batchExecutionId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long originLinkId;
    private String traceLinkHash;
    private String sourceSnapshotHash;
    private Integer sourceVersion;
    private String relationStatus;
    private LocalDateTime readAt;
}
