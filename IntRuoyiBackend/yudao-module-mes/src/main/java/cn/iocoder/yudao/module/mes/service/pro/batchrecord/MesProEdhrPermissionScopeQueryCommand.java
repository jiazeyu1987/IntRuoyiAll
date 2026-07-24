package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrPermissionScopeQueryCommand {

    private Long scopeId;

    private String objectType;

    private String objectId;
}
