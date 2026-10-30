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
    void deleteAll_deletesChildTypesBeforeParents() {
        when(workshopService.getWorkshopPage(any())).thenReturn(
                new cn.iocoder.yudao.framework.common.pojo.PageResult<>(List.of(), 0L));
        when(machineryTypeService.getMachineryTypeList(any(
                cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.type.MesDvMachineryTypeListReqVO.class)))
                .thenReturn(List.of(MesDvMachineryTypeDO.builder().id(1L).parentId(0L).build(),
                        MesDvMachineryTypeDO.builder().id(2L).parentId(1L).build()));
        machineryService.deleteAllMachineryAndWorkshops();
        var order = org.mockito.Mockito.inOrder(machineryTypeService);
        order.verify(machineryTypeService).deleteMachineryType(2L);
        order.verify(machineryTypeService).deleteMachineryType(1L);
    }

    @Test
    void deleteAll_typeFailureRollsBackEquipmentDeletion() {
        MesDvMachineryDO equipment = MesDvMachineryDO.builder().code("CLEAR-TYPE").name("clear").build();
        machineryMapper.insert(equipment);
        when(workshopService.getWorkshopPage(any())).thenReturn(
                new cn.iocoder.yudao.framework.common.pojo.PageResult<>(List.of(), 0L));
        when(machineryTypeService.getMachineryTypeList(any(
                cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.type.MesDvMachineryTypeListReqVO.class)))
                .thenReturn(List.of(MesDvMachineryTypeDO.builder().id(1L).parentId(0L).build()));
        org.mockito.Mockito.doThrow(new IllegalStateException("type has machinery"))
                .when(machineryTypeService).deleteMachineryType(1L);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> machineryService.deleteAllMachineryAndWorkshops());
        assertNotNull(machineryMapper.selectById(equipment.getId()));
    }

    @Test
    void deleteAll_equipmentRestrictionCancelsDeletion() {
        MesDvMachineryDO equipment = MesDvMachineryDO.builder().code("CLEAR-3").name("clear").build();
        machineryMapper.insert(equipment);
        when(workshopService.getWorkshopPage(any())).thenReturn(
                new cn.iocoder.yudao.framework.common.pojo.PageResult<>(List.of(), 0L));
        when(checkRecordService.getCheckRecordCountByMachineryId(equipment.getId())).thenReturn(1L);
        org.junit.jupiter.api.Assertions.assertThrows(
                cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> machineryService.deleteAllMachineryAndWorkshops());
        assertNotNull(machineryMapper.selectById(equipment.getId()));
        org.mockito.Mockito.verify(workshopService, org.mockito.Mockito.never()).deleteWorkshop(any());
    }

    @Test
    void deleteAll_removesEquipmentAndCallsWorkshopDeletion() {
        MesDvMachineryDO equipment = MesDvMachineryDO.builder().code("CLEAR-1").name("clear").build();
        machineryMapper.insert(equipment);
        when(workshopService.getWorkshopPage(any())).thenReturn(
                new cn.iocoder.yudao.framework.common.pojo.PageResult<>(
                        List.of(MesMdWorkshopDO.builder().id(3001L).build()), 1L));
        machineryService.deleteAllMachineryAndWorkshops();
        org.junit.jupiter.api.Assertions.assertNull(machineryMapper.selectById(equipment.getId()));
        verify(workshopService).deleteWorkshop(3001L);
    }

    @Test
    void deleteAll_workshopRestrictionRollsBackEquipmentDeletion() {
        MesDvMachineryDO equipment = MesDvMachineryDO.builder().code("CLEAR-2").name("clear").build();
        machineryMapper.insert(equipment);
        when(workshopService.getWorkshopPage(any())).thenReturn(
                new cn.iocoder.yudao.framework.common.pojo.PageResult<>(
                        List.of(MesMdWorkshopDO.builder().id(3001L).build()), 1L));
        org.mockito.Mockito.doThrow(new IllegalStateException("workshop has workstation"))
                .when(workshopService).deleteWorkshop(3001L);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> machineryService.deleteAllMachineryAndWorkshops());
        assertNotNull(machineryMapper.selectById(equipment.getId()));
    }

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
    @Test
    void testImportWorkshopNameAndRejectAmbiguity() {
        when(machineryTypeService.getMachineryTypeList(any(cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.type.MesDvMachineryTypeListReqVO.class)))
                .thenReturn(List.of(MesDvMachineryTypeDO.builder().id(2001L).code("TYPE").build()));
        MesMdWorkshopDO workshop = MesMdWorkshopDO.builder().id(3001L).code("WS").name("一车间").build();
        when(workshopService.getWorkshopListByStatus(anyInt())).thenReturn(List.of(workshop));
        MesDvMachineryImportExcelVO row = MesDvMachineryImportExcelVO.builder()
                .code("NAME-IMPORT").name("设备").machineryTypeCode("TYPE")
                .workshopName("一车间").status(2).build();
        assertEquals(List.of("NAME-IMPORT"), machineryService.importMachineryList(List.of(row), false).getCreateCodes());
        assertEquals(3001L, machineryMapper.selectByCode("NAME-IMPORT").getWorkshopId());
        assertEquals(List.of("NAME-IMPORT"), machineryService.importMachineryList(List.of(row), true).getUpdateCodes());
        row.setWorkshopCode("WS");
        row.setWorkshopName("其他车间");
        assertEquals("所属车间与车间编码不一致", machineryService.importMachineryList(List.of(row), true).getFailureCodes().get(row.getCode()));
        row.setWorkshopCode(null);
        assertEquals("所属车间[其他车间]不存在或未启用", machineryService.importMachineryList(List.of(row), true).getFailureCodes().get(row.getCode()));
        row.setWorkshopName("一车间");
        when(workshopService.getWorkshopListByStatus(anyInt())).thenReturn(List.of(workshop,
                MesMdWorkshopDO.builder().id(3002L).code("WS2").name("一车间").build()));
        assertEquals("所属车间[一车间]存在重名，请填写所属车间编码", machineryService.importMachineryList(List.of(row), true).getFailureCodes().get(row.getCode()));
    }
}
