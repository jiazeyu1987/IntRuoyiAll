package cn.iocoder.yudao.module.mdm.controller.admin.companyscope.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MdmCompanyScopeRespVO {

    private Long id;
    private String scopeType;
    private Long principalId;
    private String principalName;
    private String principalCode;
    private Long companyId;
    private String companyCode;
    private String companyName;
    private String status;
    private Integer revision;
    private LocalDateTime updateTime;
}
