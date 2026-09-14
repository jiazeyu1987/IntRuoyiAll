package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class BatchRecordReportAssistRowVO {

    @NotBlank(message = "辅助行标识不能为空")
    private String rowKey;

    @NotBlank(message = "辅助行描述不能为空")
    private String description;

    @NotNull(message = "辅助行排序不能为空")
    private Integer sort;

    @Valid
    @Size(min = 1, message = "辅助行至少包含一个单元格")
    private List<FieldVO> fields;

    @Data
    @Accessors(chain = true)
    public static class FieldVO {

        @NotNull(message = "rowIndex 不能为空")
        private Integer rowIndex;

        @NotNull(message = "columnIndex 不能为空")
        private Integer columnIndex;
    }
}
