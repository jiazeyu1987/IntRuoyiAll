package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MesProcessPoolActiveOrderMapperTest {

    @BeforeAll
    static void initializeMyBatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), MesProcessPoolActiveOrderDO.class.getName()),
                MesProcessPoolActiveOrderDO.class);
    }

    @Test
    void selectActiveByWorkOrderAndRouteUsesLatestActiveOrderQueryInsteadOfSelectOne() {
        MesProcessPoolActiveOrderMapper mapper = mock(MesProcessPoolActiveOrderMapper.class, CALLS_REAL_METHODS);
        MesProcessPoolActiveOrderDO latest = activeOrder(48L, LocalDateTime.of(2026, 8, 8, 11, 18));
        doReturn(List.of(latest)).when(mapper).selectList(any(Wrapper.class));

        MesProcessPoolActiveOrderDO result = mapper.selectActiveByWorkOrderAndRoute(980019L, 922119L);

        assertSame(latest, result);
        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper, times(1)).selectList(wrapperCaptor.capture());
        verify(mapper, never()).selectOne(any(Wrapper.class));
        assertSqlContainsInOrder(wrapperCaptor.getValue(),
                "work_order_id =",
                "route_id =",
                "active_status =",
                "ORDER BY joined_at DESC,id DESC",
                "LIMIT 1");
    }

    private static MesProcessPoolActiveOrderDO activeOrder(Long id, LocalDateTime joinedAt) {
        return MesProcessPoolActiveOrderDO.builder()
                .id(id)
                .workOrderId(980019L)
                .routeId(922119L)
                .routeVersionId(627L)
                .activeStatus("ACTIVE")
                .businessStatus("ACTIVE")
                .joinedAt(joinedAt)
                .build();
    }

    private static void assertSqlContainsInOrder(Wrapper<?> wrapper, String... fragments) {
        String sqlSegment = wrapper.getSqlSegment().replace("`", "").replaceAll("\\s+", " ").trim();
        int cursor = 0;
        for (String fragment : fragments) {
            int index = sqlSegment.indexOf(fragment, cursor);
            assertTrue(index >= cursor, () -> "Missing SQL fragment `" + fragment + "` in " + sqlSegment);
            cursor = index + fragment.length();
        }
    }
}
