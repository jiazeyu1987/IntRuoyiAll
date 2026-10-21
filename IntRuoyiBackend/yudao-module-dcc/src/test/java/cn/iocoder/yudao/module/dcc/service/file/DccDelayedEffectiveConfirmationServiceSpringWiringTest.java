package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAttachmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTaskAssigneeSnapshotMapper;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DccDelayedEffectiveConfirmationServiceSpringWiringTest {

    @Test
    void contextRefreshShouldInjectDccMapperByFormalBeanName() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getBeanFactory().registerSingleton("fileMapper", mock(FileMapper.class));
            context.getBeanFactory().registerSingleton("dccControlledFileMapper", mock(DccControlledFileMapper.class));

            registerMock(context, "masterMapper", DccControlledFileMasterMapper.class);
            registerMock(context, "boundContracts", DccBoundWorkflowContractResolver.class);
            registerMock(context, "confirmationEvidence", DccDelayedEffectiveConfirmationEvidence.class);
            registerMock(context, "signatureService", DccSignatureVerificationService.class);
            registerMock(context, "electronicAuthorization", DccElectronicSignatureAuthorizationService.class);
            registerMock(context, "signatureManagement", DccElectronicSignatureManagementService.class);
            registerMock(context, "signatures", DccControlledFileSignatureMapper.class);
            registerMock(context, "routes", DccControlledFileRouteSnapshotMapper.class);
            registerMock(context, "assignments", DccControlledFileTaskAssigneeSnapshotMapper.class);
            registerMock(context, "attachments", DccControlledFileAttachmentMapper.class);
            registerMock(context, "lifecycle", DccControlledFileLifecycleService.class);
            registerMock(context, "datePolicy", DccWorkflowDatePolicy.class);
            registerMock(context, "audit", DccWorkflowFileStateAudit.class);
            registerMock(context, "accounts", AdminUserApi.class);
            registerMock(context, "tasks", BpmTaskService.class);
            registerMock(context, "engineTasks", TaskService.class);
            registerMock(context, "history", HistoryService.class);
            context.registerBean(DccDelayedEffectiveConfirmationService.class);

            context.refresh();

            DccDelayedEffectiveConfirmationService service =
                    context.getBean(DccDelayedEffectiveConfirmationService.class);
            assertTrue(context.isActive());
            assertTrue(context.getBeansOfType(CommonAnnotationBeanPostProcessor.class).size() >= 1);
            assertSame(context.getBean("dccControlledFileMapper"),
                    ReflectionTestUtils.getField(service, "fileMapper"));
        }
    }

    private static <T> void registerMock(AnnotationConfigApplicationContext context,
                                          String beanName, Class<T> beanType) {
        context.getBeanFactory().registerSingleton(beanName, mock(beanType));
    }
}
