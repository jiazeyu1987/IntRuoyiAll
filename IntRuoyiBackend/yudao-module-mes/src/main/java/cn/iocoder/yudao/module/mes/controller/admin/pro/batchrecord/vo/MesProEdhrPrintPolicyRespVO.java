package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrPrintPolicyRespVO {

    private Long id;

    private String policyCode;

    private String policyName;

    private String businessType;

    private String templateType;

    private Integer firstPrintLimit;

    private Integer reprintLimit;

    private String reasonDictJson;

    private String watermarkTemplate;

    private String voidCopyWatermark;

    private String status;

    private LocalDateTime activeAt;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
