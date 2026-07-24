package cn.iocoder.yudao.module.mes.service.pro.scheduleorder;

import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProScheduleOrderServiceImplInjectionTest {

    @Test
    void workstationMapperMustBeInjectedForResourceSnapshots() throws NoSuchFieldException {
        Field field = MesProScheduleOrderServiceImpl.class.getDeclaredField("workstationMapper");

        assertEquals(MesMdWorkstationMapper.class, field.getType());
        assertTrue(field.isAnnotationPresent(Resource.class),
                "workstationMapper must be injected before building schedule resource snapshots");
    }
}
