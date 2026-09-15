package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DccProjectProductRespVO {
    private Long projectCodeId;
    private Long productMasterId;
    private String productCode;
    private String productName;
    private String source;
}
