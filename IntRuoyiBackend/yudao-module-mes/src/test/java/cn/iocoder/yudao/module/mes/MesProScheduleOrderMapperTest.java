package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderDailyCompareMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProScheduleOrderMapperTest {

    @Test
    void scheduleOrderMapper_shouldExposeRouteAndProgressQueryContract() throws NoSuchMethodException {
        Method selectWithoutRoute = MesProScheduleOrderMapper.class.getMethod("selectListWithoutRoute");
        Method selectAutoSchedulable = MesProScheduleOrderMapper.class.getMethod("selectAutoSchedulableByIds", Collection.class);
        Method updateProgress = MesProScheduleOrderMapper.class.getMethod("updateProgress",
                Long.class, BigDecimal.class, BigDecimal.class, BigDecimal.class);

        assertNotNull(selectWithoutRoute);
        assertNotNull(selectAutoSchedulable);
        assertEquals(int.class, updateProgress.getReturnType());
    }

    @Test
    void scheduleOrderMapper_pageQueryShouldSortByPromiseDateThenPriority() throws Exception {
        String source = Files.readString(Path.of("src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/scheduleorder/MesProScheduleOrderMapper.java"),
                StandardCharsets.UTF_8);
        int methodStart = source.indexOf("default PageResult<MesProScheduleOrderDO> selectPage");
        int methodEnd = source.indexOf("default MesProScheduleOrderDO selectEffectiveByWorkOrderId", methodStart);
        String methodSource = source.substring(methodStart, methodEnd);

        int promiseOrderIndex = methodSource.indexOf(".orderByAsc(MesProScheduleOrderDO::getPromiseDate)");
        int priorityOrderIndex = methodSource.indexOf(".orderByAsc(MesProScheduleOrderDO::getPriorityNo)");
        int idOrderIndex = methodSource.indexOf(".orderByAsc(MesProScheduleOrderDO::getId)");

        assertTrue(promiseOrderIndex >= 0, "排产工单分页查询必须先按承诺交期排序");
        assertTrue(priorityOrderIndex > promiseOrderIndex, "排产工单分页查询必须在承诺交期后按优先级排序");
        assertTrue(idOrderIndex > priorityOrderIndex, "排产工单分页查询必须使用 ID 升序保持稳定顺序");
    }

    @Test
    void scheduleOrderProcessMapper_shouldExposeRouteVersionAndProgressContract() throws NoSuchMethodException {
        Method selectByRouteVersion = MesProScheduleOrderProcessMapper.class.getMethod("selectListByRouteVersionId", Long.class);
        Method updateProgress = MesProScheduleOrderProcessMapper.class.getMethod("updateProgress",
                Long.class, BigDecimal.class, BigDecimal.class, BigDecimal.class);

        assertNotNull(selectByRouteVersion);
        assertEquals(int.class, updateProgress.getReturnType());
    }

    @Test
    void dailyCompareMapper_shouldExposeDailyVarianceContract() throws NoSuchMethodException {
        Method selectByOrderDate = MesProScheduleOrderDailyCompareMapper.class.getMethod("selectListByScheduleOrderIdAndDateRange",
                Long.class, LocalDate.class, LocalDate.class);
        Method selectByProcessDates = MesProScheduleOrderDailyCompareMapper.class.getMethod("selectListByProcessIdsAndDateRange",
                Collection.class, LocalDate.class, LocalDate.class);

        assertNotNull(selectByOrderDate);
        assertNotNull(selectByProcessDates);
    }
}
