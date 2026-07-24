package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class MesProEdhrPermissionEvaluateRespVO {

    private Long scopeId;

    private String objectType;

    private String objectId;

    private Map<String, String> decisions;

    private List<Long> matchedRuleIds;

    private Long operationAuditEventId;
}
