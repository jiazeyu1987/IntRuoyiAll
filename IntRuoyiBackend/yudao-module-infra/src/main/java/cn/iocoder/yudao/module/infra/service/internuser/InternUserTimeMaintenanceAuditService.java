package cn.iocoder.yudao.module.infra.service.internuser;

import cn.iocoder.yudao.module.infra.dal.dataobject.internuser.InternUserTimeMaintenanceAuditDO;

import java.time.LocalDateTime;
import java.util.List;

public interface InternUserTimeMaintenanceAuditService {

    String TARGET_FILE_UPLOAD_TIME = "FILE_UPLOAD_TIME";
    String TARGET_DCC_PUBLISHED_TIME = "DCC_PUBLISHED_TIME";
    String TARGET_DCC_OBSOLETED_TIME = "DCC_OBSOLETED_TIME";

    void recordTimeChange(Long tenantId, String targetType, Long targetId, String targetName,
                          String fieldName, LocalDateTime oldTime, LocalDateTime newTime,
                          Long operatorUserId);

    List<InternUserTimeMaintenanceAuditDO> getAuditList(String targetType, Long targetId);

}
