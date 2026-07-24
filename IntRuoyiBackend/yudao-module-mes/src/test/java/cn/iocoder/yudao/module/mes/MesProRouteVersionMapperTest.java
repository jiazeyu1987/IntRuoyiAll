package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProRouteVersionMapperTest {

    @Test
    void routeVersionMapper_shouldExposeVersionLookupContract() throws NoSuchMethodException {
        Method selectActive = MesProRouteVersionMapper.class.getMethod("selectActiveByRouteId", Long.class);
        Method selectByRouteIdAndVersionNo = MesProRouteVersionMapper.class.getMethod("selectByRouteIdAndVersionNo",
                Long.class, String.class);
        Method selectByRouteIds = MesProRouteVersionMapper.class.getMethod("selectListByRouteIds", Collection.class);
        Method selectMaxVersionNo = MesProRouteVersionMapper.class.getMethod("selectMaxVersionNoByRouteId", Long.class);

        assertNotNull(selectActive);
        assertNotNull(selectByRouteIdAndVersionNo);
        assertNotNull(selectByRouteIds);
        assertEquals(String.class, selectMaxVersionNo.getReturnType());
    }

    @Test
    void selectMaxVersionNoByRouteId_shouldOrderVersionSuffixNumerically() throws NoSuchMethodException {
        Method selectMaxVersionNo = MesProRouteVersionMapper.class.getMethod(
                "selectMaxVersionNoByRouteId", Long.class);
        Select select = selectMaxVersionNo.getAnnotation(Select.class);
        assertNotNull(select);

        String sql = String.join(" ", select.value()).replaceAll("\\s+", " ").toUpperCase();
        assertFalse(sql.contains("MAX(VERSION_NO)"));
        assertTrue(sql.contains("ORDER BY CAST(SUBSTRING_INDEX(VERSION_NO, 'V', -1) AS UNSIGNED) DESC"));
        assertTrue(sql.contains("LIMIT 1"));
    }

    @Test
    void routeScheduleConfigMapper_shouldExposeCopyAndProcessLookupContract() throws NoSuchMethodException {
        Method selectByVersion = MesProRouteScheduleConfigMapper.class.getMethod("selectListByRouteVersionId", Long.class);
        Method selectByProcess = MesProRouteScheduleConfigMapper.class.getMethod("selectByRouteVersionIdAndRouteProcessId",
                Long.class, Long.class);
        Method updateCopied = MesProRouteScheduleConfigMapper.class.getMethod("updateCopiedConfigVersion",
                Long.class, String.class, BigDecimal.class);

        assertNotNull(selectByVersion);
        assertNotNull(selectByProcess);
        assertEquals(int.class, updateCopied.getReturnType());
    }
}
