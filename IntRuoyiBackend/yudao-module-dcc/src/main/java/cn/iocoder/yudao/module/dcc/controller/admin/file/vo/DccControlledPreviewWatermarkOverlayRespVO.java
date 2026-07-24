package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledPreviewWatermarkOverlayRespVO {

    private String textColor;
    private Double opacity;
    private Integer rotationDeg;
    private Integer gapX;
    private Integer gapY;
    private Integer fontSize;
}
