package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MesReportAllocationFoundationContractTest {

    @Test
    void allocationPoolQuantityMustComeFromFullSubmittedOutput() throws Exception {
        Class<?> service = Class.forName(
                "cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationPoolQuantityService");
        assertNotNull(service.getMethod("requirePoolQuantity", MesProProcessPoolEventDO.class));
        assertEquals(BigDecimal.class,
                service.getMethod("requirePoolQuantity", MesProProcessPoolEventDO.class).getReturnType());
    }

    @Test
    void releaseStateMustExposeHistoricalReleasedExistenceQueries() throws Exception {
        Class<?> service = Class.forName(
                "cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationReleaseStateService");
        assertEquals(Set.class, service.getMethod("findReleasedActiveOrderIds", Collection.class).getReturnType());
        assertEquals(Set.class,
                service.getMethod("findReleasedActiveOrderIdsForUpdate", Collection.class).getReturnType());
    }
}
