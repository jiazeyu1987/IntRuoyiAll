package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

final class DccMessageDeliveryTestSupport {
    static Map<Long, DccControlledFileMessageJobDO> wire(DccControlledFileMessageDeliveryService service,
                                                       DccControlledFileMessageJobMapper mapper) {
        TenantContextHolder.setTenantId(1L);
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        lenient().when(manager.getTransaction(any())).thenAnswer(ignored -> new SimpleTransactionStatus());
        ReflectionTestUtils.setField(service, "transactionManager", manager);
        Map<Long, DccControlledFileMessageJobDO> jobs = new LinkedHashMap<>();
        lenient().when(mapper.selectByIdAndTenantForUpdate(any(), any()))
                .thenAnswer(invocation -> jobs.get(invocation.getArgument(1)));
        lenient().when(mapper.updateById(any(DccControlledFileMessageJobDO.class))).thenReturn(1);
        lenient().doAnswer(invocation -> {
            DccControlledFileMessageJobDO job = invocation.getArgument(0);
            if (job.getId() == null) job.setId(8000L + jobs.size());
            jobs.put(job.getId(), job);
            return 1;
        }).when(mapper).insert(any(DccControlledFileMessageJobDO.class));
        return jobs;
    }
}
