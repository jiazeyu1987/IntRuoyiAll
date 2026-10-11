package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.bpm.approval.service.provider.*;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureRecordService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real Spring transaction advice and registry dispatch; this is not proof of a deployed MySQL connection. */
class MesEdhrApprovalCenterSnapshotProxyTest {
    @Test
    void centerRegistryExternalCallKeepsWorkAndHandoffCountListUnderOneReadOnlyRepeatableReadBoundary() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "work"), MesProEdhrWorkTaskDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "handoff"), MesActiveOrderHandoffTaskDO.class);
        var work = mock(MesProEdhrWorkTaskMapper.class, Answers.CALLS_REAL_METHODS);
        var handoff = mock(MesActiveOrderHandoffTaskMapper.class, Answers.CALLS_REAL_METHODS);
        var reads = new ArrayList<String>();
        doAnswer(call -> { record(reads,"work-count"); return 0L; }).when(work).selectCount(any(Wrapper.class));
        doAnswer(call -> { record(reads,"work-prefix"); return List.of(); }).when(work).selectList(any(Wrapper.class));
        doAnswer(call -> { record(reads,"handoff-count"); return 0L; }).when(handoff).selectCount(any(Wrapper.class));
        doAnswer(call -> { record(reads,"handoff-prefix"); return List.of(); }).when(handoff).selectList(any(Wrapper.class));
        var workService = mock(MesProEdhrWorkTaskService.class);
        when(workService.getApprovalCenterAuthorizedPrefix(any(),eq(false),eq(false),eq(200L))).thenAnswer(call -> {
            var page = work.selectApprovalCenterPrefix(call.getArgument(0),12L,1L,false,"TODO",call.getArgument(3, Long.class));
            return new PageResult<>(List.of(),page.getTotal());
        });
        var projection = new MesActiveOrderHandoffApprovalProjection();
        ReflectionTestUtils.setField(projection,"tasks",handoff);
        var reader = new MesEdhrApprovalOutcomeReader(work,
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper.class),
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseDecisionMapper.class),
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper.class),
                mock(MesManagerReleaseCompletedActorResolver.class),
                mock(cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService.class),
                mock(cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseSignoffService.class),
                mock(MesEdhrExecutionFinalOutcomeReader.class));
        ReflectionTestUtils.setField(projection,"outcomeReader",reader);
        var target = new MesProEdhrApprovalTaskAdapter(workService,mock(MesProEdhrReleaseService.class),mock(MesManagerReleaseCompletedActorResolver.class));
        ReflectionTestUtils.setField(target,"handoffProjection",projection);
        ReflectionTestUtils.setField(target,"outcomeReader",reader);
        var manager = new RecordingManager();
        var factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));
        var proxy = (ApprovalTaskProvider) factory.getProxy();
        var center = new ApprovalCenterServiceImpl(new ApprovalTaskProviderRegistry(List.of(proxy)),
                mock(PermissionApi.class),mock(AdminUserApi.class),mock(ApprovalSignatureRecordService.class));
        TenantContextHolder.setTenantId(1L);
        try {
            assertEquals(0L,center.getTaskPage(12L,new ApprovalTaskQuery().setModuleCode(ApprovalModuleCode.EDHR).setPageSize(20)).getTotal());
        } finally { TenantContextHolder.clear(); }
        assertEquals(List.of("work-count","work-prefix","handoff-count","handoff-count","handoff-prefix"),reads);
        assertEquals(1,manager.beginCount);
        assertEquals(1,manager.commitCount);
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    }

    private static void record(List<String> reads,String stage) {
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive(),stage);
        assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly(),stage);
        assertEquals(TransactionDefinition.ISOLATION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel(),stage);
        reads.add(stage);
    }

    private static class RecordingManager extends AbstractPlatformTransactionManager {
        int beginCount;
        int commitCount;
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction,TransactionDefinition definition) { beginCount++; }
        @Override protected void doCommit(DefaultTransactionStatus status) { commitCount++; }
        @Override protected void doRollback(DefaultTransactionStatus status) { fail("query should not roll back"); }
    }
}
