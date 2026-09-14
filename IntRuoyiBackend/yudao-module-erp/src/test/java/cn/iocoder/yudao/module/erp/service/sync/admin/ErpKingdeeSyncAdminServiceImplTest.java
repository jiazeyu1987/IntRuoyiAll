package cn.iocoder.yudao.module.erp.service.sync.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJobParam;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncWatermarkRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeFullSyncRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncRunDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncWatermarkDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncRunMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncWatermarkMapper;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncRunStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.enums.kingdeeautosync.ErpKingdeeTableAutoSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.sync.admin.ErpKingdeeFullSyncHandler;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobPageReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.service.job.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class ErpKingdeeSyncAdminServiceImplTest {

    @Mock
    private ErpKingdeeSyncRunMapper runMapper;
    @Mock
    private ErpKingdeeSyncWatermarkMapper watermarkMapper;
    @Mock
    private ErpKingdeeConfigService configService;
    @Mock
    private JobService jobService;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private ErpKingdeeFullSyncHandler fullSyncHandler;

    private ErpKingdeeSyncAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ErpKingdeeSyncAdminServiceImpl();
        ReflectionTestUtils.setField(service, "runMapper", runMapper);
        ReflectionTestUtils.setField(service, "watermarkMapper", watermarkMapper);
        ReflectionTestUtils.setField(service, "kingdeeConfigService", configService);
        ReflectionTestUtils.setField(service, "jobService", jobService);
        ReflectionTestUtils.setField(service, "applicationContext", applicationContext);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void getRunPage_mapsFailureMessageAndCounts() {
        ErpKingdeeSyncRunPageReqVO reqVO = new ErpKingdeeSyncRunPageReqVO();
        ErpKingdeeSyncRunDO run = ErpKingdeeSyncRunDO.builder()
                .id(100L)
                .syncType(ErpKingdeeSyncTypeEnum.BOM.getType())
                .status(ErpKingdeeSyncRunStatusEnum.FAILED.getStatus())
                .createdCount(1)
                .updatedCount(2)
                .failureMessage("ERP BOM 父项物料未映射")
                .build();
        when(runMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(run), 1L));

        PageResult<ErpKingdeeSyncRunRespVO> result = service.getRunPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(ErpKingdeeSyncTypeEnum.BOM.getType(), result.getList().get(0).getSyncType());
        assertEquals("ERP BOM 父项物料未映射", result.getList().get(0).getFailureMessage());
        assertEquals(2, result.getList().get(0).getUpdatedCount());
    }

    @Test
    void getWatermarks_returnsOrderedMapperResult() {
        LocalDateTime lastSuccessTime = LocalDateTime.of(2026, 6, 12, 10, 0);
        when(watermarkMapper.selectListOrderBySyncType()).thenReturn(List.of(
                ErpKingdeeSyncWatermarkDO.builder()
                        .syncType(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER.getType())
                        .lastSuccessTime(lastSuccessTime)
                        .build()));

        List<ErpKingdeeSyncWatermarkRespVO> result = service.getWatermarks();

        assertEquals(1, result.size());
        assertEquals(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER.getType(), result.get(0).getSyncType());
        assertEquals(lastSuccessTime, result.get(0).getLastSuccessTime());
    }

    @Test
    void runFullSync_submitsQuartzJobWithExplicitFullParameter() throws Exception {
        TenantContextHolder.setTenantId(1L);
        JobDO job = JobDO.builder().id(99L).handlerName("kingdeeProductionMaterialListSyncJob").build();
        when(applicationContext.getBean("kingdeeProductionMaterialListSyncJob")).thenReturn(fullSyncHandler);
        when(jobService.getJobPage(any(JobPageReqVO.class))).thenReturn(new PageResult<>(List.of(job), 1L));

        ErpKingdeeFullSyncRespVO response = service.runFullSync(
                ErpKingdeeSyncTypeEnum.PRODUCTION_MATERIAL_LIST.getType());

        assertEquals(99L, response.getJobId());
        verify(jobService).triggerJob(eq(99L), eq(TenantJobParam.forTenant(
                1L, ErpKingdeeFullSyncHandler.FULL_SYNC_JOB_PARAM)));
        verify(fullSyncHandler, never()).executeFullSync();
    }

    @Test
    void runIncrementalSync_submitsQuartzJobWithCurrentTenantAndConfiguredParameter() throws Exception {
        TenantContextHolder.setTenantId(1L);
        JobDO job = JobDO.builder().id(102L).handlerName("kingdeeProductItemSyncJob").build();
        job.setHandlerParam(null);
        when(applicationContext.getBean("kingdeeProductItemSyncJob")).thenReturn(new Object());
        when(jobService.getJobPage(any(JobPageReqVO.class))).thenReturn(new PageResult<>(List.of(job), 1L));

        ErpKingdeeFullSyncRespVO response = service.runIncrementalSync(
                ErpKingdeeTableAutoSyncTypeEnum.PRODUCT.getSyncType());

        assertEquals("已提交增量同步任务", response.getMessage());
        verify(jobService).triggerJob(eq(102L), eq(TenantJobParam.forTenant(1L, null)));
    }
}
