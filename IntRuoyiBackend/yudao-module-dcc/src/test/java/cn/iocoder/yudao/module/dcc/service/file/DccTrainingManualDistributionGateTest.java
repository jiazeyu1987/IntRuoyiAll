package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DccTrainingManualDistributionGateTest {
    @AfterEach void cleanup() { TenantContextHolder.clear(); }
    @Test void physicalIdAloneCannotReleaseWithoutThisRoundsTrainingAttachment() {
        TenantContextHolder.setTenantId(1L);
        var service=new DccControlledFileFinalizationServiceImpl();
        var files=mock(DccControlledFileMapper.class);
        var file=DccControlledFileDO.builder().id(20L).tenantId(1L).masterId(10L).categoryId(2L)
                .processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD).processInstanceId("round-1")
                .status("ACTIVE").needTraining(true).trainingRecordFileId(99L)
                .controlledTime(LocalDateTime.now()).activatedTime(LocalDateTime.now()).build();
        when(files.selectById(20L)).thenReturn(file);
        when(files.selectByIdAndTenantForUpdate(1L,20L)).thenReturn(file);
        var permissions=mock(DccControlledFileCategoryPermissionSupport.class);
        when(permissions.hasFilePermission(file,88L,DccFileCategoryPermissionActionEnum.DISTRIBUTE)).thenReturn(true);
        var transactions=mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        var attachments=mock(DccControlledFileAttachmentMapper.class);
        ReflectionTestUtils.setField(service,"controlledFileMapper",files);
        ReflectionTestUtils.setField(service,"permissionSupport",permissions);
        ReflectionTestUtils.setField(service,"transactionTemplate",new TransactionTemplate(transactions));
        ReflectionTestUtils.setField(service,"boundContracts",mock(DccBoundWorkflowContractResolver.class));
        ReflectionTestUtils.setField(service,"trainingAttachments",attachments);
        var rejected=assertThrows(IllegalStateException.class,()->service.releaseManualDistribution(88L,20L));
        assertTrue(rejected.getMessage().contains("培训完成缺少"));
        verify(attachments).selectTrainingRecordForUpdate(1L,20L,"round-1");
        verify(files,never()).updateById(any(DccControlledFileDO.class));
        verify(transactions).rollback(any());
    }
}
