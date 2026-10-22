package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** SQL wrapper contract only. Does not certify MySQL's runtime locking or transaction rollback. */
class MesProEdhrProductionFreezeNowaitR3Test {
    @Test
    void eventFreezeUsesAnExactNowaitLock() throws Exception {
        assertExactNowait(MesProProcessPoolEventMapper.class, MesProProcessPoolEventDO.class, 401L);
    }

    @Test
    void reviewFreezeUsesAnExactNowaitLock() throws Exception {
        assertExactNowait(MesProcessPoolSubmissionReviewMapper.class, MesProcessPoolSubmissionReviewDO.class, 601L);
    }

    private <T> void assertExactNowait(Class<T> mapperType, Class<?> entityType, Long id) throws Exception {
        // Reflection keeps this pre-production test compilable; absence is an explicit failing contract assertion.
        Method method = assertDoesNotThrow(() -> mapperType.getMethod("selectByIdForUpdateNowait", Long.class),
                "freeze requires a dedicated exact NOWAIT mapper method");
        var assistant = new MapperBuilderAssistant(new Configuration(), mapperType.getName());
        assistant.setCurrentNamespace(mapperType.getName());
        TableInfoHelper.initTableInfo(assistant, entityType);
        List<Wrapper<?>> queries = new ArrayList<>();
        T mapper = mock(mapperType, call -> {
            if ("selectOne".equals(call.getMethod().getName())) {
                queries.add(call.getArgument(0));
                return null;
            }
            return call.callRealMethod();
        });
        method.invoke(mapper, id);
        assertEquals(1, queries.size(), "exactly one lock query; no retry or fallback query");
        var query = (AbstractWrapper<?, ?, ?>) queries.get(0);
        String sql = query.getSqlSegment();
        assertTrue(sql.matches("(?s).*\\bid\\s*=.*"), sql);
        assertTrue(sql.endsWith("FOR UPDATE NOWAIT"), sql);
        assertFalse(sql.contains("SKIP LOCKED"), sql);
        assertEquals(List.of(id), new ArrayList<>(query.getParamNameValuePairs().values()),
                "must bind the requested identity, never select latest or all event reviews");
    }
}
