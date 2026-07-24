package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProBatchRecordParsedCell {

    private String text;

    @Builder.Default
    private int rowSpan = 1;

    @Builder.Default
    private int colSpan = 1;

    private Integer columnIndex;

    private Integer logicalColumnIndex;

    private Integer logicalColSpan;

    @Builder.Default
    private boolean bold = false;

    @Builder.Default
    private int fontSize = 10;

    @Builder.Default
    private String horizontalAlign = "left";

    @Builder.Default
    private String verticalAlign = "middle";

    @Builder.Default
    private int widthPx = 120;

    @Builder.Default
    private int heightPx = 36;

    @Builder.Default
    private boolean fillable = false;

    @Builder.Default
    private boolean visualBlank = false;

    @Builder.Default
    private boolean borderless = false;

    @Builder.Default
    private boolean diagonalSlash = false;

    @Builder.Default
    private boolean reviewedCellRule = false;

    private String cellRuleSource;

    private String topBorderStyle;

    private String bottomBorderStyle;

    private String leftBorderStyle;

    private String rightBorderStyle;

    private String backgroundColor;

    private String documentFrameRole;

    @Builder.Default
    private String placeholder = "请填写";

    @Builder.Default
    private String inputType = "Input";
}
