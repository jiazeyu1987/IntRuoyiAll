package cn.iocoder.yudao.module.dcc.service.file;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/** User-confirmed policy: twenty calendar years from the actual obsolete timestamp. */
@Service
public class DccObsoleteRetentionService {
    @Resource private DccControlledFileNameClaimService identities;
    public LocalDateTime retainedUntil(LocalDateTime obsoleteAt) {
        if(obsoleteAt==null) throw new IllegalArgumentException("作废时间缺失");
        return obsoleteAt.plusYears(20);
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void retain(Long tenantId,Long masterId,LocalDateTime obsoleteAt) {
        identities.retainObsoleteIdentity(tenantId,masterId,obsoleteAt,retainedUntil(obsoleteAt));
    }
}
