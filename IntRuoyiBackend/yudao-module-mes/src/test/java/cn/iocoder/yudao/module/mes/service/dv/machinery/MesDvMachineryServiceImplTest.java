package cn.iocoder.yudao.module.mes.service.dv.machinery;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachinerySaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryTypeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.service.dv.checkplan.MesDvCheckPlanMachineryService;
import cn.iocoder.yudao.module.mes.service.dv.checkrecord.MesDvCheckRecordService;
import cn.iocoder.yudao.module.mes.service.dv.maintenrecord.MesDvMaintenRecordService;
import cn.iocoder.yudao.module.mes.service.dv.repair.MesDvRepairService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkshopService;
import cn.iocoder.yudao.module.mes.service.wm.barcode.MesWmBarcodeService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.module.mes.enums.wm.BarcodeBizTypeEnum.MACHINERY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(MesDvMachineryServiceImpl.class)
class MesDvMachineryServiceImplTest extends BaseDbUnitTest {

    @Resource
    private MesDvMachineryServiceImpl machineryService;

    @Resource
    private MesDvMachineryMapper machineryMapper;

    @MockitoBean
    private MesDvMachineryTypeService machineryTypeService;
    @MockitoBean
    private MesMdWorkshopService workshopService;
    @MockitoBean
    private MesWmBarcodeService barcodeService;
    @MockitoBean
    private MesDvCheckPlanMachineryService checkPlanMachineryService;
    @MockitoBean
    private MesDvCheckRecordService checkRecordService;
    @MockitoBean
    private MesDvMaintenRecordService maintenRecordService;
    @MockitoBean
    private MesDvRepairService repairService;

    @Test
    void testCreateMachinery_persistProcessNameAndStandardHourlyCapacity() {
        MesDvMachinerySaveReqVO reqVO = new MesDvMachinerySaveReqVO();
        reqVO.setCode("B09528");
        reqVO.setName("自动上料磨削");
        reqVO.setMachineryTypeId(2001L);
        reqVO.setWorkshopId(3001L);
        reqVO.setStatus(2);
        reqVO.setProcessName("造影导管磨削");
        reqVO.setStandardHourlyCapacity(new BigDecimal("180"));

        when(machineryTypeService.getMachineryType(reqVO.getMachineryTypeId()))
                .thenReturn(MesDvMachineryTypeDO.builder().id(reqVO.getMachineryTypeId()).build());
        when(workshopService.getWorkshop(reqVO.getWorkshopId()))
                .thenReturn(MesMdWorkshopDO.builder().id(reqVO.getWorkshopId()).build());

        Long id = machineryService.createMachinery(reqVO);

        MesDvMachineryDO machinery = machineryMapper.selectById(id);
        assertNotNull(machinery);
        assertEquals("造影导管磨削", machinery.getProcessName());
        assertEquals(0, machinery.getStandardHourlyCapacity().compareTo(new BigDecimal("180")));
        verify(barcodeService).autoGenerateBarcode(MACHINERY.getValue(), id, "B09528", "自动上料磨削");
    }

    @Test
    void testImportMachineryList_persistProcessNameAndStandardHourlyCapacity() {
        MesDvMachineryTypeDO machineryType = MesDvMachineryTypeDO.builder()
                .id(2001L).code("DEFAULT-MACHINERY-TYPE").name("默认设备类型")
                .status(CommonStatusEnum.ENABLE.getStatus()).sort(1).parentId(0L).build();
        MesMdWorkshopDO workshop = MesMdWorkshopDO.builder()
                .id(3001L).code("AUTO-WSHOP").name("AutoScheduleWorkshop").build();
        when(machineryTypeService.getMachineryTypeList(any(cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.type.MesDvMachineryTypeListReqVO.class)))
                .thenReturn(List.of(machineryType));
        when(workshopService.getWorkshopListByStatus(anyInt()))
                .thenReturn(List.of(workshop));

        MesDvMachineryImportExcelVO importExcelVO = MesDvMachineryImportExcelVO.builder()
                .code("A05192")
                .name("熔接机")
                .machineryTypeCode("DEFAULT-MACHINERY-TYPE")
                .workshopCode("AUTO-WSHOP")
                .status(2)
                .processName("造影导管融飞边")
                .standardHourlyCapacity(new BigDecimal("190"))
                .build();

        MesDvMachineryImportRespVO respVO = machineryService.importMachineryList(List.of(importExcelVO), false);

        assertEquals(List.of("A05192"), respVO.getCreateCodes());
        MesDvMachineryDO machinery = machineryMapper.selectByCode("A05192");
        assertNotNull(machinery);
        assertEquals("造影导管融飞边", machinery.getProcessName());
        assertEquals(0, machinery.getStandardHourlyCapacity().compareTo(new BigDecimal("190")));
        verify(barcodeService).autoGenerateBarcode(MACHINERY.getValue(), machinery.getId(), "A05192", "熔接机");
    }
}
