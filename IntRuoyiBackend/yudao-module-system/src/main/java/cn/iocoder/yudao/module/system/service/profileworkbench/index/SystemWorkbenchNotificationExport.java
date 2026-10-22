package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import cn.iocoder.yudao.module.system.api.workbench.index.WorkbenchNotificationExport;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyMessageMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;

@Service @ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class SystemWorkbenchNotificationExport implements WorkbenchNotificationExport {
    private final NotifyMessageMapper mapper;
    public SystemWorkbenchNotificationExport(NotifyMessageMapper mapper){this.mapper=mapper;}
    @Override public String requireContent(long tenant,long message) {
        if(!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Notification export requires frozen transaction");
        var row=mapper.selectById(message);
        if(row==null||row.getTenantId()==null||row.getTenantId()!=tenant||row.getTemplateContent()==null) throw SOURCE_CONTRACT_INVALID.exception();
        return row.getTemplateContent();
    }
}
