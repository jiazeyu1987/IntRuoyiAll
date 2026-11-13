package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.dal.mysql.logger.OperateLogMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.logger.OperateLogDO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOperationAuditRespVO;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesWorkflowInteractionLogTest {
    private final MesProEdhrOperationAuditService audit=mock(MesProEdhrOperationAuditService.class);
    private final AdminUserService users=mock(AdminUserService.class);
    private final MesProEdhrOperationAuditEventMapper mapper=mock(MesProEdhrOperationAuditEventMapper.class);
    private final OperateLogMapper operateLogs=mock(OperateLogMapper.class);
    private final MesWorkflowInteractionLog log=new MesWorkflowInteractionLog(audit,mapper,users,operateLogs);
    @BeforeEach void setup(){TenantContextHolder.setTenantId(168L);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(),"workflow-log"),
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO.class);
        var user=new AdminUserDO().setId(7L).setNickname("真实操作人");user.setTenantId(168L);when(users.getUser(7L)).thenReturn(user);
        when(audit.recordInCallerTransaction(any())).thenAnswer(invocation -> {
            MesProEdhrOperationAuditCommand command = invocation.getArgument(0);
            return new MesProEdhrOperationAuditRespVO().setId(81L).setOccurredAt(command.getOccurredAt());
        });}
    @AfterEach void clear(){TenantContextHolder.clear();}
    @Test void cancellationAndValidationUseServerActorAndTimeWithoutBusinessSuccess(){
        LocalDateTime before=LocalDateTime.now();
        log.recordClient(7L,"PRODUCTION","CANCELED","关闭操作确认或编辑窗口");
        log.recordClient(7L,"PQC_RELEASE","BLOCKED","页面校验未通过");
        var commands=ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);verify(audit,times(2)).recordInCallerTransaction(commands.capture());
        assertEquals("CANCELED",commands.getAllValues().get(0).getResultStatus());
        assertEquals("BLOCKED",commands.getAllValues().get(1).getResultStatus());
        for(var c:commands.getAllValues()){
            assertEquals(7L,c.getActorUserId());assertEquals("真实操作人",c.getActorUsername());
            assertTrue(c.getActionName().matches(".*[\\p{IsHan}].*"));assertFalse(c.getOccurredAt().isBefore(before));
            assertFalse(c.getMetadataJson().contains("activeOrderId"));assertNull(c.getBatchExecutionId());
        }
        assertThrows(IllegalArgumentException.class,()->log.recordClient(7L,"PRODUCTION","SUCCESS","提交"));
        assertThrows(IllegalArgumentException.class,()->log.recordClient(7L,"PRODUCTION","CANCELED","password=secret"));
    }
    @Test void tenantMismatchAndAuditFailureAreExplicit(){
        users.getUser(7L).setTenantId(1L);
        assertThrows(IllegalStateException.class,()->log.recordClient(7L,"PQC","CANCELED","取消"));
        verifyNoInteractions(audit);
        users.getUser(7L).setTenantId(168L);
        doThrow(new IllegalStateException("存储失败")).when(audit).recordInCallerTransaction(any());
        assertThrows(IllegalStateException.class,()->log.recordClient(7L,"PQC","CANCELED","取消"));
    }
    @Test void requestFailureHasIndependentAuditAndNoPayload(){
        log.recordRequestFailure(7L,"确认上市放行","POST","ReleaseController#approve",403);
        verify(audit).recordInCallerTransaction(argThat(c->"确认上市放行失败".equals(c.getActionName())&&"FAILED".equals(c.getResultStatus())
                &&"DENY".equals(c.getPermissionDecision())
                &&c.getMetadataJson().contains("403")&&!c.getMetadataJson().contains("password")));
        log.ownRecent(7L);
        verify(mapper).selectList(argThat(q->q.getSqlSegment().contains("actor_user_id")&&q.getSqlSegment().contains("LIMIT 100")));
    }
    @Test void allOutcomesBecomeSearchableOperationLogRows() {
        log.recordClient(7L,"PRODUCTION","CANCELED","取消");
        log.recordClient(7L,"PRODUCTION","BLOCKED","页面校验未通过");
        log.recordRequestFailure(7L,"提交报工","POST","FeedbackController#submit",403);
        var rows=ArgumentCaptor.forClass(OperateLogDO.class);
        verify(operateLogs,times(3)).insert(rows.capture());
        assertEquals(java.util.List.of("已取消","已拦截","失败"),
                rows.getAllValues().stream().map(OperateLogDO::getSubType).toList());
        for(var row:rows.getAllValues()) {
            assertEquals(7L,row.getUserId());
            assertEquals(81L,row.getBizId());
            assertEquals("生产业务交互",row.getType());
            assertEquals(false,row.getSuccess());
            assertNotNull(row.getCreateTime());
            assertTrue(row.getExtra().contains("auditEventId"));
            assertFalse(row.getExtra().contains("password"));
        }
    }
    @Test void operationLogWriteFailureIsReported() {
        doThrow(new IllegalStateException("系统操作日志写入失败")).when(operateLogs).insert(any(OperateLogDO.class));
        assertThrows(IllegalStateException.class,()->log.recordClient(7L,"PQC","CANCELED","取消"));
    }
}
