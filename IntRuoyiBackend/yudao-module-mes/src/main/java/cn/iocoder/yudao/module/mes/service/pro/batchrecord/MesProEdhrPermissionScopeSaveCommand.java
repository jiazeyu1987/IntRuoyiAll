package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProEdhrPermissionScopeSaveCommand {

    private Long scopeId;

    private String scopeName;

    private String objectType;

    private String objectId;

    private Long parentScopeId;

    private Integer expectedVersion;

    private List<MesProEdhrPermissionRuleCommand> rules;

    private Long actorUserId;

    private String actorUsername;
}
