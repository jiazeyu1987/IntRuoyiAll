package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledPreviewWatermarkRespVO {

    private String label;
    private String text;
    private String actorName;
    private String actorAccount;
    private String timestamp;
    private String purpose;
    private DccControlledPreviewWatermarkOverlayRespVO overlay;
}
