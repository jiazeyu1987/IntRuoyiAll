package cn.iocoder.yudao.module.erp.service.sync.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncWatermarkRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncRunDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncWatermarkDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncRunMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncWatermarkMapper;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncRunStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpKingdeeSyncAdminServiceImplTest {

    @Mock
    private ErpKingdeeSyncRunMapper runMapper;
    @Mock
    private ErpKingdeeSyncWatermarkMapper watermarkMapper;

    private ErpKingdeeSyncAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ErpKingdeeSyncAdminServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "runMapper", runMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "watermarkMapper", watermarkMapper);
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
}
