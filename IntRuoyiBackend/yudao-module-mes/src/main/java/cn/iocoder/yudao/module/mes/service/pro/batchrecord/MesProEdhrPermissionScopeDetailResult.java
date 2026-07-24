package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProEdhrPermissionScopeDetailResult {

    private Long scopeId;

    private String scopeName;

    private String objectType;

    private String objectId;

    private Long parentScopeId;

    private String status;

    private Integer version;

    private List<MesProEdhrPermissionRuleResult> rules;

    private Long operationAuditEventId;
}
