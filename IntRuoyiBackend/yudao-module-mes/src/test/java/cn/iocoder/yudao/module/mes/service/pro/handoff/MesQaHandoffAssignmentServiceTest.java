package cn.iocoder.yudao.module.mes.service.pro.handoff;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrCandidateResolver;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MesQaHandoffAssignmentServiceTest {
 MesQaHandoffAssignmentService service;MesProEdhrWorkTaskAssignmentRuleMapper rules;MesProRouteMapper routes;MesProEdhrCandidateResolver candidates;MesActiveOrderHandoffOwnerResolver owners;
 @BeforeEach void fixture(){TenantContextHolder.setTenantId(1L);service=new MesQaHandoffAssignmentService();rules=mock(MesProEdhrWorkTaskAssignmentRuleMapper.class);routes=mock(MesProRouteMapper.class);candidates=mock(MesProEdhrCandidateResolver.class);owners=mock(MesActiveOrderHandoffOwnerResolver.class);for(var e:Map.of("rules",rules,"routes",routes,"candidates",candidates,"owners",owners).entrySet())ReflectionTestUtils.setField(service,e.getKey(),e.getValue());when(routes.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(MesProRouteDO.builder().id(98L).status(0).build());}
 @AfterEach void clear(){TenantContextHolder.clear();}
 @Test void missingExactRouteRuleFailsWithoutScanningQaUsers(){assertThrows(RuntimeException.class,()->service.resolve(98L));verify(rules).selectListByScopeAndType("ROUTE",98L,"QA_REVIEW");verifyNoInteractions(candidates,owners);}
 @Test void otherRouteNeverSharesOwnerAndDisabledOrDuplicateRulesFail(){var r=rule();when(rules.selectListByScopeAndType("ROUTE",98L,"QA_REVIEW")).thenReturn(List.of(r));assertThrows(RuntimeException.class,()->service.resolve(99L));r.setEnabled(false);assertThrows(RuntimeException.class,()->service.resolve(98L));r.setEnabled(true);when(rules.selectListByScopeAndType("ROUTE",98L,"QA_REVIEW")).thenReturn(List.of(r,rule()));assertThrows(RuntimeException.class,()->service.resolve(98L));verifyNoInteractions(candidates,owners);}
 @Test void disabledOrUnqualifiedResolvedOwnerIsRejectedBeforeTaskCreation(){var r=rule();when(rules.selectListByScopeAndType("ROUTE",98L,"QA_REVIEW")).thenReturn(List.of(r));when(candidates.resolveAssignmentRule(r)).thenReturn(new MesProEdhrCandidateResolver.MesProEdhrCandidateContract("USER",345L,"345"));doThrow(new IllegalStateException("disabled or no permission")).when(owners).user(345L,MesQaHandoffAssignmentService.DISPOSE_PERMISSION);assertThrows(RuntimeException.class,()->service.resolve(98L));}
 @Test void exactFormalRuleFreezesOnlyValidatedSystemCandidates(){var r=rule();when(rules.selectListByScopeAndType("ROUTE",98L,"QA_REVIEW")).thenReturn(List.of(r));when(candidates.resolveAssignmentRule(r)).thenReturn(new MesProEdhrCandidateResolver.MesProEdhrCandidateContract("USER",345L,"345"));assertEquals("345",service.resolve(98L));verify(owners).user(345L,MesQaHandoffAssignmentService.DISPOSE_PERMISSION);}
 static MesProEdhrWorkTaskAssignmentRuleDO rule(){return new MesProEdhrWorkTaskAssignmentRuleDO().setId(901L).setScopeType("ROUTE").setScopeId(98L).setTaskType("QA_REVIEW").setCandidateSourceType("USER").setCandidateSourceId(345L).setEnabled(true);}
}
