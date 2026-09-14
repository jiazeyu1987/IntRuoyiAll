package cn.iocoder.yudao.module.mes.service.pro.mesprocess;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.mesprocess.vo.MesProMesProcessPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.mesprocess.vo.MesProMesProcessRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.mesprocess.MesProMesProcessCatalogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.mesprocess.MesProMesProcessCatalogMachineryDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.mesprocess.MesProMesProcessCatalogMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.mesprocess.MesProMesProcessCatalogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProMesProcessServiceImplTest {

    @Mock
    private MesProMesProcessCatalogMapper catalogMapper;
    @Mock
    private MesProMesProcessCatalogMachineryMapper catalogMachineryMapper;
    @InjectMocks
    private MesProMesProcessServiceImpl service;

    @Test
    void getMesProcessPage_returnsExcelRowsInSourceOrderWithoutDerivedBooleans() {
        MesProMesProcessPageReqVO reqVO = new MesProMesProcessPageReqVO();
        reqVO.setPageSize(50);
        MesProMesProcessCatalogDO row19 = MesProMesProcessCatalogDO.builder()
                .id(9003131018L)
                .sourceFileName("压力泵工序.xlsx")
                .sourceSheetName("二代压力泵")
                .sourceRowNo(19)
                .sortNo(18)
                .catalogCode("PUMP2-MES-0018")
                .productName("二代压力泵")
                .sourceMachineryCodes("B09032/G01160")
                .mesProcessName("二代压力泵负压检测")
                .sourceMachineryName("/")
                .sourceMachineryQuantity("2")
                .dailyCapacity10_5("4000")
                .dailyWorkerQuantity("1")
                .mesProcessCode("Z1610")
                .processPrice("0.0834")
                .feedbackFlag("是")
                .batchRecordFlag("是（两道合并）")
                .batchRecordProcessName("检测")
                .build();
        MesProMesProcessCatalogDO row20 = MesProMesProcessCatalogDO.builder()
                .id(9003131019L)
                .sourceFileName("压力泵工序.xlsx")
                .sourceSheetName("二代压力泵")
                .sourceRowNo(20)
                .sortNo(19)
                .catalogCode("PUMP2-MES-0019")
                .productName("二代压力泵")
                .sourceMachineryCodes("G01143")
                .mesProcessName("测二代压力泵全套")
                .sourceMachineryName("小气压检测")
                .sourceMachineryQuantity("1")
                .dailyCapacity10_5("2000")
                .dailyWorkerQuantity("1")
                .mesProcessCode("Z1580")
                .processPrice("0.1509")
                .feedbackFlag("是")
                .batchRecordFlag("是（两道合并）")
                .batchRecordProcessName("检测")
                .build();
        when(catalogMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(row19, row20), 2L));
        when(catalogMachineryMapper.selectListByCatalogIds(List.of(9003131018L, 9003131019L))).thenReturn(List.of(
                MesProMesProcessCatalogMachineryDO.builder().id(9003132016L).catalogId(9003131018L)
                        .machinerySortNo(1).machineryCode("B09032").machineryName("/").build(),
                MesProMesProcessCatalogMachineryDO.builder().id(9003132017L).catalogId(9003131018L)
                        .machinerySortNo(2).machineryCode("G01160").machineryName("/").build(),
                MesProMesProcessCatalogMachineryDO.builder().id(9003132018L).catalogId(9003131019L)
                        .machinerySortNo(1).machineryCode("G01143").machineryName("小气压检测").build()));

        PageResult<MesProMesProcessRespVO> page = service.getMesProcessPage(reqVO);

        assertEquals(2L, page.getTotal());
        assertEquals("二代压力泵:19", page.getList().get(0).getRowKey());
        assertEquals("B09032/G01160", page.getList().get(0).getSourceMachineryCodes());
        assertEquals("是（两道合并）", page.getList().get(0).getBatchRecordFlag());
        assertEquals("检测", page.getList().get(0).getBatchRecordProcessName());
        assertEquals("B09032", page.getList().get(0).getMachineryList().get(0).getMachineryCode());
        assertEquals("G01160", page.getList().get(0).getMachineryList().get(1).getMachineryCode());
        assertEquals("二代压力泵:20", page.getList().get(1).getRowKey());
        assertEquals("测二代压力泵全套", page.getList().get(1).getMesProcessName());
    }
}
