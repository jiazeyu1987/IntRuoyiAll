package cn.iocoder.yudao.module.mes.service.md.item;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
import cn.iocoder.yudao.module.mes.controller.admin.md.item.vo.MesMdItemImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.item.vo.MesMdItemSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemTypeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.iocoder.yudao.module.mes.service.wm.barcode.MesWmBarcodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesMdItemProductMasterLifecycleTest {

    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesMdItemTypeService itemTypeService;
    @Mock
    private MesMdUnitMeasureService unitMeasureService;
    @Mock
    private MesWmBarcodeService barcodeService;
    @Mock
    private MesMdAutoCodeRecordService autoCodeRecordService;
    @Mock
    private MdmProductApi mdmProductApi;

    private MesMdItemServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MesMdItemServiceImpl();
        ReflectionTestUtils.setField(service, "itemMapper", itemMapper);
        ReflectionTestUtils.setField(service, "itemTypeService", itemTypeService);
        ReflectionTestUtils.setField(service, "unitMeasureService", unitMeasureService);
        ReflectionTestUtils.setField(service, "barcodeService", barcodeService);
        ReflectionTestUtils.setField(service, "autoCodeRecordService", autoCodeRecordService);
        assertDoesNotThrow(() -> ReflectionTestUtils.setField(service, "mdmProductApi", mdmProductApi),
                "MES item service must own an explicit MDM product validation boundary");
    }

    @Test
    void createItem_shouldPersistValidatedProductMasterId() {
        stubReferenceData();
        when(mdmProductApi.getProduct(11L)).thenReturn(MdmProductRespDTO.builder().id(11L).build());
        when(itemMapper.insert(any(MesMdItemDO.class))).thenAnswer(invocation -> {
            invocation.<MesMdItemDO>getArgument(0).setId(101L);
            return 1;
        });
        MesMdItemSaveReqVO request = saveRequest(null, "ITEM-101", "Item 101", 11L);

        assertEquals(101L, service.createItem(request));

        ArgumentCaptor<MesMdItemDO> captor = ArgumentCaptor.forClass(MesMdItemDO.class);
        verify(itemMapper).insert(captor.capture());
        assertEquals(11L, captor.getValue().getProductMasterId());
        verify(mdmProductApi).getProduct(11L);
    }

    @Test
    void updateItem_shouldPersistValidatedProductMasterId() {
        stubReferenceData();
        when(itemMapper.selectById(101L)).thenReturn(MesMdItemDO.builder().id(101L).build());
        when(mdmProductApi.getProduct(12L)).thenReturn(MdmProductRespDTO.builder().id(12L).build());
        MesMdItemSaveReqVO request = saveRequest(101L, "ITEM-101", "Item 101", 12L);

        service.updateItem(request);

        ArgumentCaptor<MesMdItemDO> captor = ArgumentCaptor.forClass(MesMdItemDO.class);
        verify(itemMapper).updateById(captor.capture());
        assertEquals(12L, captor.getValue().getProductMasterId());
    }

    @Test
    void importItem_shouldPersistValidatedProductMasterId() {
        stubReferenceData();
        when(unitMeasureService.getUnitMeasureByCode("PCS"))
                .thenReturn(MesMdUnitMeasureDO.builder().id(31L).code("PCS").build());
        when(mdmProductApi.listSimpleProducts(null, null, "MDM-P-13")).thenReturn(List.of(
                MdmProductRespDTO.builder().id(13L).productCode("MDM-P-13").build()));
        MesMdItemImportExcelVO row = MesMdItemImportExcelVO.builder()
                .code("ITEM-103").name("Item 103").unitMeasureCode("PCS").itemTypeId(21L).build();
        setProductMasterCode(row, "MDM-P-13");

        var result = service.importItemList(List.of(row), false);

        assertEquals(List.of("ITEM-103"), result.getCreateCodes());
        ArgumentCaptor<MesMdItemDO> captor = ArgumentCaptor.forClass(MesMdItemDO.class);
        verify(itemMapper).insert(captor.capture());
        assertEquals(13L, captor.getValue().getProductMasterId());
    }

    @Test
    void importItem_shouldRejectUnknownProductMasterCodeBeforeWrite() {
        stubReferenceData();
        when(unitMeasureService.getUnitMeasureByCode("PCS"))
                .thenReturn(MesMdUnitMeasureDO.builder().id(31L).code("PCS").build());
        when(mdmProductApi.listSimpleProducts(null, null, "UNKNOWN-MDM")).thenReturn(List.of());
        MesMdItemImportExcelVO row = MesMdItemImportExcelVO.builder()
                .code("ITEM-199").name("Item 199").unitMeasureCode("PCS").itemTypeId(21L).build();
        setProductMasterCode(row, "UNKNOWN-MDM");

        var result = service.importItemList(List.of(row), false);

        assertEquals(1, result.getFailureCodes().size());
        assertTrue(result.getFailureCodes().get("ITEM-199").contains("UNKNOWN-MDM"));
        verify(itemMapper, never()).insert(any(MesMdItemDO.class));
    }

    @Test
    void createItem_shouldRejectUnknownProductMasterBeforeWrite() {
        stubReferenceData();
        when(mdmProductApi.getProduct(99L)).thenReturn(null);
        MesMdItemSaveReqVO request = saveRequest(null, "ITEM-199", "Item 199", 99L);

        assertThrows(ServiceException.class, () -> service.createItem(request));

        verify(itemMapper, never()).insert(any(MesMdItemDO.class));
    }

    private void stubReferenceData() {
        when(itemTypeService.getItemType(21L)).thenReturn(MesMdItemTypeDO.builder().id(21L).build());
        when(itemTypeService.getItemTypeChildrenList(21L)).thenReturn(List.of());
        lenient().when(unitMeasureService.getUnitMeasure(31L))
                .thenReturn(MesMdUnitMeasureDO.builder().id(31L).build());
    }

    private static MesMdItemSaveReqVO saveRequest(Long id, String code, String name, Long productMasterId) {
        MesMdItemSaveReqVO request = new MesMdItemSaveReqVO();
        request.setId(id);
        request.setCode(code);
        request.setName(name);
        request.setItemTypeId(21L);
        request.setUnitMeasureId(31L);
        setProductMasterId(request, productMasterId);
        return request;
    }

    private static void setProductMasterId(Object target, Long productMasterId) {
        assertDoesNotThrow(() -> {
            Field field = target.getClass().getDeclaredField("productMasterId");
            field.setAccessible(true);
            field.set(target, productMasterId);
        }, target.getClass().getSimpleName() + " must expose productMasterId");
    }

    private static void setProductMasterCode(Object target, String productMasterCode) {
        assertDoesNotThrow(() -> {
            Field field = target.getClass().getDeclaredField("productMasterCode");
            field.setAccessible(true);
            field.set(target, productMasterCode);
        }, target.getClass().getSimpleName() + " must expose productMasterCode");
    }
}
