package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MesFeedbackFormalApprovalQueryContractTest {
    @BeforeAll static void metadata() {
        for (Class<?> type : List.of(MesProFeedbackDO.class, MesProProcessPoolEventDO.class,
                MesProcessPoolSubmissionReviewDO.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), type.getName()), type);
        }
    }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void formalEventBatchIsExplicitlyTenantTypeSourceAndIdentityBoundWithoutFirstRowSelection() {
        var mapper = mock(MesProProcessPoolEventMapper.class, CALLS_REAL_METHODS);
        doAnswer(call -> {
            LambdaQueryWrapper<?> query = call.getArgument(0);
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("tenant_id =")); assertTrue(sql.contains("event_type ="));
            assertTrue(sql.contains("feedback_source_type =")); assertTrue(sql.contains(" IN "));
            assertFalse(sql.contains("LIMIT")); assertFalse(sql.contains("FOR UPDATE"));
            assertTrue(query.getParamNameValuePairs().values().containsAll(List.of(1L, "PRODUCTION_SUBMIT", "MES_PRO_FEEDBACK")));
            return List.of();
        }).when(mapper).selectList(any());
        mapper.selectFormalProductionByFeedbackIds(1L, List.of(5660L, 5661L));
        mapper.selectFormalProductionByEventIds(1L, List.of(282240L, 282241L));
        verify(mapper, times(2)).selectList(any());
        verify(mapper, never()).selectOne(any());
    }

    @Test void reviewBatchKeepsAllRowsForAmbiguityDetectionAndReviewerDiscoveryUsesActualActor() {
        var mapper = mock(MesProcessPoolSubmissionReviewMapper.class, CALLS_REAL_METHODS);
        doAnswer(call -> {
            LambdaQueryWrapper<?> query = call.getArgument(0);
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("tenant_id =")); assertFalse(sql.contains("LIMIT"));
            assertTrue(query.getParamNameValuePairs().containsValue(1L));
            if (sql.contains("event_id IN")) {
                assertFalse(sql.contains("leader_type"), "wrong-type or multiple reviews must remain visible for validation");
                assertTrue(query.getParamNameValuePairs().values().containsAll(List.of(282240L, 282241L)));
            } else {
                assertTrue(sql.contains("leader_user_id =")); assertTrue(sql.contains("leader_type ="));
                assertTrue(query.getParamNameValuePairs().values().containsAll(List.of(341L, "PRODUCTION")));
            }
            return List.of();
        }).when(mapper).selectList(any());
        mapper.selectFormalReviewsByEventIds(1L, List.of(282240L, 282241L));
        mapper.selectProductionReviewsByReviewer(1L, 341L);
        verify(mapper, times(2)).selectList(any());
    }

    @Test void feedbackSourcesRemainTenantAndKeywordBoundEvenWhenTenantInterceptorIsIgnored() {
        TenantContextHolder.setTenantId(1L); TenantContextHolder.setIgnore(true);
        var mapper = mock(MesProFeedbackMapper.class, CALLS_REAL_METHODS);
        doAnswer(call -> {
            LambdaQueryWrapper<?> query = call.getArgument(0);
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("tenant_id =")); assertTrue(sql.contains("code LIKE"));
            assertTrue(sql.contains("ORDER BY id DESC"));
            assertTrue(query.getParamNameValuePairs().values().containsAll(List.of(1L, "%FB-000855%")));
            if (sql.contains("approve_user_id")) assertTrue(query.getParamNameValuePairs().containsValue(341L));
            else assertTrue(sql.contains("id IN"));
            return List.of();
        }).when(mapper).selectList(any());
        mapper.selectUnifiedApprovalList(341L, null, List.of(2), "FB-000855");
        mapper.selectUnifiedFormalApprovalSources(List.of(5660L), "FB-000855");
        verify(mapper, times(2)).selectList(any());
    }

    @Test void emptyIdentityBatchesNeverTurnIntoUnboundedQueriesAndMissingTenantFails() {
        var eventMapper = mock(MesProProcessPoolEventMapper.class, CALLS_REAL_METHODS);
        var reviewMapper = mock(MesProcessPoolSubmissionReviewMapper.class, CALLS_REAL_METHODS);
        var feedbackMapper = mock(MesProFeedbackMapper.class, CALLS_REAL_METHODS);
        assertTrue(eventMapper.selectFormalProductionByFeedbackIds(1L, List.of()).isEmpty());
        assertTrue(eventMapper.selectFormalProductionByEventIds(1L, List.of()).isEmpty());
        assertTrue(reviewMapper.selectFormalReviewsByEventIds(1L, List.of()).isEmpty());
        assertTrue(feedbackMapper.selectUnifiedFormalApprovalSources(List.of(), null).isEmpty());
        verify(eventMapper, never()).selectList(any()); verify(reviewMapper, never()).selectList(any());
        verify(feedbackMapper, never()).selectList(any());
        assertThrows(NullPointerException.class,
                () -> feedbackMapper.selectUnifiedFormalApprovalSources(List.of(5660L), null));
    }
}
