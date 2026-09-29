package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventRelationDO;

import java.util.List;

public interface GxpAuditQueryService {

    PageResult<GxpAuditEventDO> page(GxpAuditEventPageQuery query);

    GxpAuditEventDO get(Long eventId);

    List<GxpAuditEventRelationDO> listRelations(Long eventId);
}
