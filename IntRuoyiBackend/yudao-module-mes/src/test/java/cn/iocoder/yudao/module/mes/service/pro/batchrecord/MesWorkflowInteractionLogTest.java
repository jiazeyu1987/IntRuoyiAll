package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesWorkflowInteractionLogTest {
    private final MesProEdhrOperationAuditService audit=mock(MesProEdhrOperationAuditService.class);
    private final AdminUserService users=mock(AdminUserService.class);
    private final MesProEdhrOperationAuditEventMapper mapper=mock(MesProEdhrOperationAuditEventMapper.class);
    private final MesWorkflowInteractionLog log=new MesWorkflowInteractionLog(audit,mapper,users);
    @BeforeEach void setup(){TenantContextHolder.setTenantId(168L);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(),"workflow-log"),
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO.class);
        var user=new AdminUserDO().setId(7L).setNickname("真实操作人");user.setTenantId(168L);when(users.getUser(7L)).thenReturn(user);}
    @AfterEach void clear(){TenantContextHolder.clear();}
    @Test void cancellationAndValidationUseServerActorAndTimeWithoutBusinessSuccess(){
        LocalDateTime before=LocalDateTime.now();
        log.recordClient(7L,"PRODUCTION","CANCELED","关闭操作确认或编辑窗口");
        log.recordClient(7L,"PQC_RELEASE","BLOCKED","页面校验未通过");
        var commands=ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);verify(audit,times(2)).record(commands.capture());
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
        doThrow(new IllegalStateException("存储失败")).when(audit).record(any());
        assertThrows(IllegalStateException.class,()->log.recordClient(7L,"PQC","CANCELED","取消"));
    }
    @Test void requestFailureHasIndependentAuditAndNoPayload(){
        log.recordRequestFailure(7L,"确认上市放行","POST","ReleaseController#approve",403);
        verify(audit).record(argThat(c->"确认上市放行失败".equals(c.getActionName())&&"FAILED".equals(c.getResultStatus())
                &&"DENY".equals(c.getPermissionDecision())
                &&c.getMetadataJson().contains("403")&&!c.getMetadataJson().contains("password")));
        log.ownRecent(7L);
        verify(mapper).selectList(argThat(q->q.getSqlSegment().contains("actor_user_id")&&q.getSqlSegment().contains("LIMIT 100")));
    }
}
