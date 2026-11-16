package cn.iocoder.yudao.module.erp.controller.admin.config;

import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.service.job.JobService;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobSaveReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ErpKingdeeJobPermissionTest {
    public static class PermissionGate {
        private final java.util.Set<String> permissions;
        PermissionGate(String... permissions) { this.permissions=java.util.Set.of(permissions); }
        public boolean hasPermission(String permission) { return permissions.contains(permission); }
    }
    private boolean canSync(String type,String... permissions) throws Exception {
        var controller=cn.iocoder.yudao.module.erp.controller.admin.sync.ErpKingdeeSyncController.class;
        var request=new cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeFullSyncReqVO();request.setSyncType(type);
        var method=controller.getMethod("runFullSync",request.getClass());
        String expression=method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value();
        var context=new org.springframework.expression.spel.support.StandardEvaluationContext();
        context.setVariable("reqVO",request);
        context.setBeanResolver((c,name)->new PermissionGate(permissions));
        return Boolean.TRUE.equals(new org.springframework.expression.spel.standard.SpelExpressionParser().parseExpression(expression).getValue(context,Boolean.class));
    }
    @Test void syncAuthorizationEvaluatesTheActualControllerGuard() throws Exception {
        assertTrue(canSync("PRODUCT","erp:data-product:operate"));
        assertFalse(canSync("PRODUCTION_ORDER","erp:data-product:operate"));
        assertFalse(canSync("STOCK","erp:data-stock:operate"));
        assertTrue(canSync("STOCK","erp:data-stock:operate","erp:data-inventory:operate"));
        assertTrue(canSync("PRODUCT","erp:data-config:operate"));
    }
    @Test void configurationCannotReadOrUpdateUnrelatedJobs() throws Exception {
        JobService jobs=mock(JobService.class);
        var controller=new ErpKingdeeJobController();
        ReflectionTestUtils.setField(controller,"jobService",jobs);
        var page=new JobPageReqVO(); page.setHandlerName("unrelatedJob");
        assertThrows(IllegalArgumentException.class,()->controller.getPage(page));
        when(jobs.getJob(3L)).thenReturn(new JobDO().setId(3L).setHandlerName("unrelatedJob"));
        assertThrows(IllegalArgumentException.class,()->controller.updateStatus(3L,1));
        verify(jobs,never()).getJobPage(any());
        verify(jobs,never()).updateJobStatus(any(),any());
    }
    @Test void scheduleUpdatePreservesHandlerParametersAndRetrySettings() throws Exception {
        JobService jobs=mock(JobService.class);
        var controller=new ErpKingdeeJobController();
        ReflectionTestUtils.setField(controller,"jobService",jobs);
        when(jobs.getJob(4L)).thenReturn(new JobDO().setId(4L).setName("ERP product")
                .setHandlerName("kingdeeProductItemSyncJob").setHandlerParam("original")
                .setRetryCount(3).setRetryInterval(5).setCronExpression("0 0 2 * * ?"));
        var request=new ErpKingdeeJobController.ScheduleRequest();request.setId(4L);request.setCronExpression("0 0 3 * * ?");
        controller.update(request);
        var saved=ArgumentCaptor.forClass(JobSaveReqVO.class);verify(jobs).updateJob(saved.capture());
        assertEquals("0 0 3 * * ?",saved.getValue().getCronExpression());
        assertEquals("original",saved.getValue().getHandlerParam());
        assertEquals("kingdeeProductItemSyncJob",saved.getValue().getHandlerName());
        assertEquals(3,saved.getValue().getRetryCount());
    }
}
