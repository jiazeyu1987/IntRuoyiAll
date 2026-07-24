package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO {

    @NotNull(message = "executionId 不能为空")
    private Long executionId;

    @NotBlank(message = "fieldPath 不能为空")
    private String fieldPath;

    @NotBlank(message = "fieldKey 不能为空")
    private String fieldKey;

    @NotNull(message = "rowIndex 不能为空")
    @Min(value = 0, message = "rowIndex 不能小于 0")
    private Integer rowIndex;

    @NotNull(message = "columnIndex 不能为空")
    @Min(value = 0, message = "columnIndex 不能小于 0")
    private Integer columnIndex;

    @NotNull(message = "pageSize 不能为空")
    @Min(value = 1, message = "pageSize 必须大于等于 1")
    @Max(value = 200, message = "pageSize 不能超过 200")
    private Integer pageSize = 50;

    private Long cursorFieldAuditRevision;

    private Long cursorAuditItemId;

    @JsonIgnore
    @AssertTrue(message = "cursorFieldAuditRevision 与 cursorAuditItemId 必须同时提供")
    public boolean isCursorPairValid() {
        return (cursorFieldAuditRevision == null) == (cursorAuditItemId == null);
    }
}
