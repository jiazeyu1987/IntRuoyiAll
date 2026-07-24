package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProEdhrPermissionScopeDetailRespVO {

    private Long scopeId;

    private String scopeName;

    private String objectType;

    private String objectId;

    private Long parentScopeId;

    private String status;

    private Integer version;

    private List<MesProEdhrPermissionRuleRespVO> rules;

    private Long operationAuditEventId;
}
