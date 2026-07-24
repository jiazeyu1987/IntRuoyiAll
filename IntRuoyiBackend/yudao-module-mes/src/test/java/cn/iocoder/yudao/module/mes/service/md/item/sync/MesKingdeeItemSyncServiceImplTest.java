package cn.iocoder.yudao.module.mes.service.md.item.sync;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductUnitService;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemTypeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemTypeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.unitmeasure.MesMdUnitMeasureMapper;
import org.mybatis.spring.SqlSessionTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesKingdeeItemSyncServiceImplTest {

    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpProductUnitService productUnitService;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesMdItemTypeMapper itemTypeMapper;
    @Mock
    private MesMdUnitMeasureMapper unitMeasureMapper;
    @Mock
    private SqlSessionTemplate sqlSessionTemplate;

    private MesKingdeeItemSyncServiceImpl syncService;

    @BeforeEach
    void setUp() {
        syncService = new MesKingdeeItemSyncServiceImpl(productMapper, productUnitService, itemMapper,
                itemTypeMapper, unitMeasureMapper, sqlSessionTemplate);
    }

    @Test
    void syncItemsByProductCodes_onlyProcessesChangedProductCodesWithoutFullDisableSweep() {
        ErpProductDO newProduct = new ErpProductDO()
                .setId(1L)
                .setBarCode("MAT-NEW")
                .setName("Material New")
                .setStandard("Spec A")
                .setUnitId(10L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        ErpProductDO updateProduct = new ErpProductDO()
                .setId(2L)
                .setBarCode("MAT-UPD")
                .setName("Material Updated")
                .setStandard("Spec B")
                .setUnitId(11L)
                .setStatus(CommonStatusEnum.DISABLE.getStatus());
        when(productMapper.selectListByBarCodes(any(Collection.class))).thenReturn(List.of(newProduct, updateProduct));
        when(productUnitService.getProductUnitMap(any(Collection.class))).thenReturn(Map.of(
                10L, new ErpProductUnitDO().setId(10L).setName("BOX"),
                11L, new ErpProductUnitDO().setId(11L).setName("PCS")));
        when(itemMapper.selectListByCodes(any(Collection.class))).thenReturn(List.of(
                new MesMdItemDO().setId(200L).setCode("MAT-UPD").setName("Old").setSpecification("Old")
                        .setUnitMeasureId(31L).setItemTypeId(20L).setStatus(CommonStatusEnum.ENABLE.getStatus())));
        when(itemTypeMapper.selectByParentIdAndCode(MesMdItemTypeDO.PARENT_ID_ROOT,
                MesKingdeeItemSyncServiceImpl.DEFAULT_ITEM_TYPE_CODE))
                .thenReturn(new MesMdItemTypeDO().setId(20L));
        when(unitMeasureMapper.selectByCode("BOX")).thenReturn(null);
        when(unitMeasureMapper.selectByName("BOX")).thenReturn(null);
        when(unitMeasureMapper.selectByCode("PCS")).thenReturn(new MesMdUnitMeasureDO().setId(31L));
        doAnswer(invocation -> {
            MesMdUnitMeasureDO unit = invocation.getArgument(0);
            unit.setId(30L);
            return 1;
        }).when(unitMeasureMapper).insert(any(MesMdUnitMeasureDO.class));

        MesKingdeeItemSyncResult result =
                syncService.syncItemsByProductCodes(List.of("MAT-NEW", "MAT-UPD"));

        assertEquals(1, result.getCreatedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(0, result.getDisabledCount());
        assertEquals(0, result.getSkippedCount());
        ArgumentCaptor<Collection<String>> productCodesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(productMapper).selectListByBarCodes(productCodesCaptor.capture());
        verify(sqlSessionTemplate).clearCache();
        assertEquals(List.of("MAT-NEW", "MAT-UPD"), List.copyOf(productCodesCaptor.getValue()));
        ArgumentCaptor<Collection<String>> itemCodesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(itemMapper).selectListByCodes(itemCodesCaptor.capture());
        assertEquals(List.of("MAT-NEW", "MAT-UPD"), List.copyOf(itemCodesCaptor.getValue()));
        verify(itemMapper, never()).selectListAll();

        ArgumentCaptor<MesMdItemDO> createCaptor = ArgumentCaptor.forClass(MesMdItemDO.class);
        verify(itemMapper).insert(createCaptor.capture());
        assertEquals("MAT-NEW", createCaptor.getValue().getCode());
        assertEquals(30L, createCaptor.getValue().getUnitMeasureId());

        ArgumentCaptor<MesMdItemDO> updateCaptor = ArgumentCaptor.forClass(MesMdItemDO.class);
        verify(itemMapper).updateById(updateCaptor.capture());
        assertEquals(200L, updateCaptor.getValue().getId());
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), updateCaptor.getValue().getStatus());
    }

}
