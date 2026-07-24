package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProEdhrDhrTemplatePageReqVO extends PageParam {

    private Long catalogId;

    private String templateCode;

    private String templateName;

    private String status;

    private String reviewStatus;

    private String signoffStatus;

    private LocalDateTime[] createTime;
}
