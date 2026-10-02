package cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Schema(description = "管理后台 - GxP 审计事件分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class GxpAuditEventPageReqVO extends PageParam {

    @Schema(description = "查询范围类型", example = "ACTIVE_ORDER")
    private String scopeType;
    @Schema(description = "查询范围编号", example = "100")
    private Long scopeId;
    @Schema(description = "业务域", example = "MES")
    private String domain;
    @Schema(description = "对象类型", example = "MES_ACTIVE_ORDER")
    private String subjectType;
    @Schema(description = "对象编号", example = "ACTIVE_ORDER:100")
    private String subjectId;
    @Schema(description = "操作编号", example = "mes.active-order.complete")
    private String operationId;
    @Schema(description = "动作", example = "COMPLETE")
    private String action;
    @Schema(description = "结果状态", example = "SUCCESS")
    private String resultStatus;
    @Schema(description = "操作人编号", example = "1")
    private Long actorId;
    @Schema(description = "是否存在电子签名")
    private Boolean signaturePresent;
    @Schema(description = "服务器审计时间范围，恰好两个epoch毫秒值（含起止时间）")
    private Long[] occurredAt;

    @JsonIgnore
    @AssertTrue(message = "审计时间范围必须为两个有效且升序的毫秒时间戳")
    public boolean isOccurredAtRangeValid() {
        if (occurredAt == null) {
            return true;
        }
        if (occurredAt.length != 2 || occurredAt[0] == null || occurredAt[1] == null
                || occurredAt[0] > occurredAt[1]) {
            return false;
        }
        try {
            for (Long timestamp : occurredAt) {
                int year = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC).getYear();
                if (year < 1000 || year > 9999) {
                    return false;
                }
            }
            return true;
        } catch (DateTimeException exception) {
            return false;
        }
    }

    /** Converts the transport range to the ledger's UTC column contract. */
    @JsonIgnore
    public LocalDateTime[] toUtcOccurredAt() {
        if (!isOccurredAtRangeValid()) {
            throw new IllegalArgumentException("审计时间范围必须为两个有效且升序的毫秒时间戳");
        }
        if (occurredAt == null) {
            return null;
        }
        return new LocalDateTime[]{
                LocalDateTime.ofInstant(Instant.ofEpochMilli(occurredAt[0]), ZoneOffset.UTC),
                LocalDateTime.ofInstant(Instant.ofEpochMilli(occurredAt[1]), ZoneOffset.UTC)
        };
    }
}
