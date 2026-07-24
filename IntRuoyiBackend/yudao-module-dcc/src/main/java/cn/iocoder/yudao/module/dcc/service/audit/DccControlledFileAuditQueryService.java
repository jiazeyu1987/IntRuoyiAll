package cn.iocoder.yudao.module.dcc.service.audit;

import cn.iocoder.yudao.framework.common.pojo.PageResult;

public interface DccControlledFileAuditQueryService {

    PageResult<DccControlledFileAuditRecord> getAuditPage(DccControlledFileAuditQuery query);

}
