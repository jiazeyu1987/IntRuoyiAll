package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationAuditDO;

public interface DccElectronicSignatureAuthorizationAuditService {

    void recordAuthorizationChange(DccElectronicSignatureAuthorizationAuditDO audit);
}
