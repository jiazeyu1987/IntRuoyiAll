package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrLabelInstanceRespVO {

    private Long id;

    private String labelCode;

    private Long templateId;

    private String templateCode;

    private String templateVersion;

    private String businessType;

    private Long businessObjectId;

    private String businessObjectCode;

    private String renderSnapshotJson;

    private String parserVersion;

    private String status;

    private String printStatus;

    private String businessKeyHash;

    private Long generatedBy;

    private LocalDateTime generatedAt;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
