package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrTravelerTemplateRespVO {

    private Long id;

    private String templateCode;

    private String templateName;

    private String templateVersion;

    private String status;

    private String applicableProductCode;

    private Long applicableRouteId;

    private String applicableRouteCode;

    private Long applicableProcessId;

    private String applicableProcessCode;

    private String applicableProcessName;

    private LocalDateTime activeAt;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
