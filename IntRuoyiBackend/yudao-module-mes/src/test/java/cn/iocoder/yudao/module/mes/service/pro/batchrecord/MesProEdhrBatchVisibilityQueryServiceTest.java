package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchVisibilityMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesProEdhrBatchVisibilityQueryServiceTest {
    public static void main(String[] args) {
        for (String test : List.of("sources", "malformed", "overview", "scope")) {
            var instance = new MesProEdhrBatchVisibilityQueryServiceTest();
            instance.setup();
            try {
                switch (test) {
                    case "sources" -> instance.resolvesDistinctRuleSourcesOncePerRequestWithoutReadingBatchRows();
                    case "malformed" -> instance.malformedReachableRuleIsAnError();
                    case "overview" -> instance.overviewDoesNotInspectUnreachableRuleErrors();
                    case "scope" -> instance.sameAuthorizedIdentityScopeIsAppliedToEveryProbeAndRuleQuery();
                }
            } finally { instance.teardown(); }
        }
        System.out.println("PASS visibility query preparation: distinct sources, request isolation, explicit errors, overview short circuit");
    }
    private final MesProEdhrBatchVisibilityQueryService service = new MesProEdhrBatchVisibilityQueryService();
    private final MesProEdhrBatchExecutionMapper batches = mock(MesProEdhrBatchExecutionMapper.class);
    private final MesProEdhrBatchVisibilityMapper mapper = mock(MesProEdhrBatchVisibilityMapper.class);
    private final MesProEdhrBatchExecutionVisibilityService visibility = mock(MesProEdhrBatchExecutionVisibilityService.class);
    private final MesProEdhrCandidateResolver candidates = mock(MesProEdhrCandidateResolver.class);
    @BeforeEach void setup() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "visibility-query-test"),
                MesProEdhrBatchExecutionDO.class);
        TenantContextHolder.setTenantId(1L);
        ReflectionTestUtils.setField(service,"batchMapper",batches);
        ReflectionTestUtils.setField(service,"visibilityMapper",mapper);
        ReflectionTestUtils.setField(service,"visibilityService",visibility);
        ReflectionTestUtils.setField(service,"candidateResolver",candidates);
        when(batches.buildPageQuery(any())).thenAnswer(ignored -> new LambdaQueryWrapperX<>());
    }
    @AfterEach void teardown() { TenantContextHolder.clear(); }
    @Test void resolvesDistinctRuleSourcesOncePerRequestWithoutReadingBatchRows() {
        var first = rule(1L,"ROLES","10,20");
        var second = rule(2L,"ROLES","10,20");
        var third = rule(3L,"DEPT_LEADER","30");
        when(mapper.selectEffectiveRules(any())).thenReturn(List.of(first,second,third));
        when(candidates.resolveProcessFormRule(first)).thenReturn(new MesProEdhrCandidateResolver.MesProEdhrCandidateContract("ROLES",null,"71,72"));
        when(candidates.resolveProcessFormRule(third)).thenReturn(new MesProEdhrCandidateResolver.MesProEdhrCandidateContract("DEPT_LEADER",null,"72"));
        var result=service.prepare(new EdhrBatchExecutionPageReqVO(),71L);
        assertEquals(List.of(1L,2L),result.ruleIds());
        verify(candidates).resolveProcessFormRule(first);
        verify(candidates).resolveProcessFormRule(third);
        verifyNoMoreInteractions(candidates);
        verify(batches,times(5)).buildPageQuery(any());
        verifyNoMoreInteractions(batches);
        service.prepare(new EdhrBatchExecutionPageReqVO(),71L);
        verify(candidates,times(2)).resolveProcessFormRule(first);
    }
    @Test void malformedReachableRuleIsAnError() {
        var rule=rule(1L,"INVALID","x");
        when(mapper.selectEffectiveRules(any())).thenReturn(List.of(rule));
        var failure=new IllegalStateException("invalid source");
        when(candidates.resolveProcessFormRule(rule)).thenThrow(failure);
        assertSame(failure,assertThrows(IllegalStateException.class,()->service.prepare(new EdhrBatchExecutionPageReqVO(),71L)));
    }
    @Test void overviewDoesNotInspectUnreachableRuleErrors() {
        when(visibility.hasOverviewPermission(71L)).thenReturn(true);
        assertTrue(service.prepare(new EdhrBatchExecutionPageReqVO(),71L).overview());
        verifyNoInteractions(batches,mapper,candidates);
    }
    @Test void sameAuthorizedIdentityScopeIsAppliedToEveryProbeAndRuleQuery() {
        var request = new EdhrBatchExecutionPageReqVO(); request.setTracePerson("operator");
        assertThrows(IllegalArgumentException.class, () -> service.prepare(request,71L));
        service.prepare(request,71L,new MesProEdhrTraceSearchScope(Set.of(99L)));
        @SuppressWarnings("unchecked") var probe = ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(mapper,times(4)).exists(probe.capture());
        verify(mapper).selectEffectiveRules(probe.capture());
        for (var query : probe.getAllValues()) {
            String sql=query.getSqlSegment();
            assertFalse(sql.contains("system_users"));
            assertTrue(sql.contains("JSON_TABLE"));
            assertTrue(query.getParamNameValuePairs().containsValue(99L));
            assertTrue(query.getParamNameValuePairs().containsValue("operator"));
        }
    }
    private static MesProEdhrProcessFormPermissionRuleDO rule(Long id,String type,String ids) {
        return new MesProEdhrProcessFormPermissionRuleDO().setId(id).setCandidateSourceType(type).setCandidateSourceIds(ids);
    }
}
