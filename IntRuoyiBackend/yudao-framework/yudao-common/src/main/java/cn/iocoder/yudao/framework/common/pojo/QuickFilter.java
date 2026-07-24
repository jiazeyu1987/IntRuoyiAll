package cn.iocoder.yudao.framework.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "通用快速过滤条件")
@Data
public class QuickFilter {

    @Schema(description = "字段标识", example = "productName")
    private String fieldKey;

    @Schema(description = "操作符：contains、eq、between", example = "contains")
    private String operator;

    @Schema(description = "过滤值", example = "压力泵")
    private String value;

    @Schema(description = "结束值，日期范围使用", example = "2026-07-31")
    private String valueEnd;

}
