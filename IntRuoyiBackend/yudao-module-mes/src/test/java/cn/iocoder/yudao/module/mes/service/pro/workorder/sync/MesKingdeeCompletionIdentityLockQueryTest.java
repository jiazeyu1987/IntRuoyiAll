package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MesKingdeeCompletionIdentityLockQueryTest {
    @Test
    void locksAllHistoricalStatusesDeterministicallyBeforeCheckingCompletion() {
        var configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "erp-active-lock"),
                MesProcessPoolActiveOrderDO.class);
        var mapper = mock(MesProcessPoolActiveOrderMapper.class);
        doCallRealMethod().when(mapper).selectAllByWorkOrderIdForUpdate(501L);
        when(mapper.selectList(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<MesProcessPoolActiveOrderDO> query = invocation.getArgument(0);
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("work_order_id"));
            assertTrue(sql.contains("ORDER BY id ASC"));
            assertTrue(sql.endsWith("FOR UPDATE"));
            assertFalse(sql.contains("active_status"), "completed, closed and reworked identities must be locked");
            assertFalse(sql.contains("business_status"));
            assertTrue(query.getParamNameValuePairs().containsValue(501L));
            return List.of();
        });
        mapper.selectAllByWorkOrderIdForUpdate(501L);
        verify(mapper).selectList(any());
    }

    @Test
    void receiptLookupIsCurrentReadAndIncludesEveryCompletionCycle() {
        var configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "erp-receipt-lock"),
                MesProcessPoolActiveOrderCompletionReceiptDO.class);
        var mapper = mock(MesProcessPoolActiveOrderCompletionReceiptMapper.class);
        doCallRealMethod().when(mapper).selectFirstByWorkOrderIdForUpdate(501L);
        when(mapper.selectOne(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<MesProcessPoolActiveOrderCompletionReceiptDO> query = invocation.getArgument(0);
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("work_order_id"));
            assertTrue(sql.contains("ORDER BY id ASC"));
            assertTrue(sql.endsWith("LIMIT 1 FOR UPDATE"));
            assertFalse(sql.contains("active_order_id"), "prior completion cycles also protect batch identity");
            assertTrue(query.getParamNameValuePairs().containsValue(501L));
            return null;
        });
        mapper.selectFirstByWorkOrderIdForUpdate(501L);
        verify(mapper).selectOne(any());
    }
}
