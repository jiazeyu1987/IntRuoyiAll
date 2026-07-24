package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MesProEdhrDhrTemplateRespVO {

    private Long id;

    private Long catalogId;

    private String templateCode;

    private String templateName;

    private String currentVersion;

    private String status;

    private String reviewStatus;

    private String signoffStatus;

    private Integer bindingCount;

    private Integer integrityIssueCount;

    private String integrityIssueJson;

    private String signoffEvidenceHash;

    private LocalDateTime effectiveAt;

    private LocalDateTime retiredAt;

    private LocalDateTime voidedAt;

    private String remark;

    private LocalDateTime createTime;

    private List<MesProEdhrDhrTemplateVersionRespVO> versions;

    private List<MesProEdhrDhrTemplateBindingRespVO> bindings;
}
