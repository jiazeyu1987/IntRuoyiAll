package cn.iocoder.yudao.module.erp.service.product.sync;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductCategoryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductCategoryMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductUnitMapper;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeMaterial;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeMaterialClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpKingdeeProductSyncServiceImplTest {

    @Mock
    private ErpKingdeeMaterialClient materialClient;
    @Mock
    private ErpKingdeeConfigService kingdeeConfigService;
    @Mock
    private ErpProductMapper productMapper;
    @Mock
    private ErpProductCategoryMapper productCategoryMapper;
    @Mock
    private ErpProductUnitMapper productUnitMapper;

    private ErpKingdeeProperties kingdeeProperties;
    private ErpKingdeeProductSyncServiceImpl syncService;

    @BeforeEach
    void setUp() {
        kingdeeProperties = new ErpKingdeeProperties();
        kingdeeProperties.setBaseUrl("https://k3.example.com");
        kingdeeProperties.setAcctId("acct");
        kingdeeProperties.setUsername("user");
        kingdeeProperties.setPassword("password");
        kingdeeProperties.setLcid(2052);
        kingdeeProperties.getProduct().setQueryLimit(5000);
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        syncService = new ErpKingdeeProductSyncServiceImpl(
                materialClient, kingdeeConfigService, productMapper, productCategoryMapper, productUnitMapper);
    }

    @Test
    void syncProducts_createsUpdatesAndSkipsProducts() {
        ErpKingdeeMaterial createMaterial = buildMaterial("MAT-NEW", "Material New", "Spec A",
                "CHLB_NEW", "New Category", "BOX", "A");
        ErpKingdeeMaterial updateMaterial = buildMaterial("MAT-UPD", "Material Updated", "Spec B",
                "CHLB05_SYS", "Finished Goods", "PCS", "B");
        ErpKingdeeMaterial skipMaterial = buildMaterial("MAT-SAME", "Material Same", "Spec C",
                "CHLB05_SYS", "Finished Goods", "PCS", "A");
        when(materialClient.fetchMaterials(kingdeeProperties)).thenReturn(List.of(createMaterial, updateMaterial, skipMaterial));
        when(productMapper.selectListByBarCodes(any(Collection.class))).thenReturn(List.of(
                new ErpProductDO().setId(10L).setBarCode("MAT-UPD").setName("Old Name").setCategoryId(1L).setUnitId(1L)
                        .setStatus(CommonStatusEnum.ENABLE.getStatus()).setStandard("Old Spec"),
                new ErpProductDO().setId(11L).setBarCode("MAT-SAME").setName("Material Same").setCategoryId(2L).setUnitId(2L)
                        .setStatus(CommonStatusEnum.ENABLE.getStatus()).setStandard("Spec C")
        ));
        when(productCategoryMapper.selectByCode("CHLB05_SYS")).thenReturn(new ErpProductCategoryDO().setId(2L));
        when(productCategoryMapper.selectByCode("CHLB_NEW")).thenReturn(null);
        when(productUnitMapper.selectByName("PCS")).thenReturn(new ErpProductUnitDO().setId(2L));
        when(productUnitMapper.selectByName("BOX")).thenReturn(null);
        doAnswer(invocation -> {
            ErpProductCategoryDO category = invocation.getArgument(0);
            category.setId(20L);
            return 1;
        }).when(productCategoryMapper).insert(any(ErpProductCategoryDO.class));
        doAnswer(invocation -> {
            ErpProductUnitDO unit = invocation.getArgument(0);
            unit.setId(30L);
            return 1;
        }).when(productUnitMapper).insert(any(ErpProductUnitDO.class));
        when(productMapper.insertBatch(any(Collection.class), eq(1000))).thenReturn(true);
        when(productMapper.updateBatch(any(Collection.class), eq(1000))).thenReturn(true);

        ErpKingdeeProductSyncResult result = syncService.syncProducts();

        assertEquals(1, result.getCreatedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(1, result.getSkippedCount());

        ArgumentCaptor<Collection<ErpProductDO>> createCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(productMapper).insertBatch(createCaptor.capture(), eq(1000));
        ErpProductDO createdProduct = createCaptor.getValue().iterator().next();
        assertEquals("MAT-NEW", createdProduct.getBarCode());
        assertEquals(20L, createdProduct.getCategoryId());
        assertEquals(30L, createdProduct.getUnitId());

        ArgumentCaptor<Collection<ErpProductDO>> updateCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(productMapper).updateBatch(updateCaptor.capture(), eq(1000));
        ErpProductDO updatedProduct = updateCaptor.getValue().iterator().next();
        assertEquals(10L, updatedProduct.getId());
        assertEquals("Material Updated", updatedProduct.getName());
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), updatedProduct.getStatus());
        assertEquals("Spec B", updatedProduct.getStandard());
    }

    @Test
    void syncProductsModifiedBetween_usesIncrementalMaterialClientAndUpsertsProducts() {
        ErpKingdeeMaterial createMaterial = buildMaterial("MAT-NEW", "Material New", "Spec A",
                "CHLB_NEW", "New Category", "BOX", "A");
        createMaterial.setSourceModifyTime(LocalDateTime.of(2026, 6, 12, 8, 30));
        LocalDateTime windowStart = LocalDateTime.of(2026, 6, 12, 8, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 12, 9, 0);
        when(materialClient.fetchMaterialsModifiedBetween(kingdeeProperties, windowStart, windowEnd))
                .thenReturn(List.of(createMaterial));
        when(productMapper.selectListByBarCodes(any(Collection.class))).thenReturn(List.of());
        when(productCategoryMapper.selectByCode("CHLB_NEW")).thenReturn(null);
        when(productUnitMapper.selectByName("BOX")).thenReturn(null);
        doAnswer(invocation -> {
            ErpProductCategoryDO category = invocation.getArgument(0);
            category.setId(20L);
            return 1;
        }).when(productCategoryMapper).insert(any(ErpProductCategoryDO.class));
        doAnswer(invocation -> {
            ErpProductUnitDO unit = invocation.getArgument(0);
            unit.setId(30L);
            return 1;
        }).when(productUnitMapper).insert(any(ErpProductUnitDO.class));
        when(productMapper.insertBatch(any(Collection.class), eq(1000))).thenReturn(true);

        ErpKingdeeProductSyncResult result = syncService.syncProductsModifiedBetween(windowStart, windowEnd);

        assertEquals(1, result.getCreatedCount());
        assertEquals(0, result.getUpdatedCount());
        assertEquals(0, result.getSkippedCount());
        verify(materialClient).fetchMaterialsModifiedBetween(kingdeeProperties, windowStart, windowEnd);
        ArgumentCaptor<Collection<ErpProductDO>> createCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(productMapper).insertBatch(createCaptor.capture(), eq(1000));
        assertEquals("MAT-NEW", createCaptor.getValue().iterator().next().getBarCode());
    }

    @Test
    void syncProductsByNumbers_fetchesOnlyRequestedMaterialsAndCreatesProduct() {
        ErpKingdeeMaterial material = buildMaterial("A002.09.002.230396", "外标签 (INT)",
                "造影导管4F 通用", "CHLB10_SYS", "包装物", "张", "A");
        when(materialClient.fetchMaterialsByNumbers(kingdeeProperties, List.of("A002.09.002.230396")))
                .thenReturn(List.of(material));
        when(productMapper.selectListByBarCodes(any(Collection.class))).thenReturn(List.of());
        when(productCategoryMapper.selectByCode("CHLB10_SYS")).thenReturn(new ErpProductCategoryDO().setId(40L));
        when(productUnitMapper.selectByName("张")).thenReturn(new ErpProductUnitDO().setId(50L));
        when(productMapper.insertBatch(any(Collection.class), eq(1000))).thenReturn(true);

        ErpKingdeeProductSyncResult result = syncService.syncProductsByNumbers(List.of("A002.09.002.230396"));

        assertEquals(1, result.getCreatedCount());
        assertEquals(0, result.getUpdatedCount());
        assertEquals(0, result.getSkippedCount());
        verify(materialClient).fetchMaterialsByNumbers(kingdeeProperties, List.of("A002.09.002.230396"));
        ArgumentCaptor<Collection<ErpProductDO>> createCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(productMapper).insertBatch(createCaptor.capture(), eq(1000));
        ErpProductDO createdProduct = createCaptor.getValue().iterator().next();
        assertEquals("A002.09.002.230396", createdProduct.getBarCode());
        assertEquals("外标签 (INT)", createdProduct.getName());
        assertEquals(40L, createdProduct.getCategoryId());
        assertEquals(50L, createdProduct.getUnitId());
    }

    private static ErpKingdeeMaterial buildMaterial(String materialNumber,
                                                    String materialName,
                                                    String specification,
                                                    String categoryCode,
                                                    String categoryName,
                                                    String unitName,
                                                    String forbidStatus) {
        ErpKingdeeMaterial material = new ErpKingdeeMaterial();
        material.setMaterialNumber(materialNumber);
        material.setMaterialName(materialName);
        material.setSpecification(specification);
        material.setCategoryCode(categoryCode);
        material.setCategoryName(categoryName);
        material.setUnitName(unitName);
        material.setForbidStatus(forbidStatus);
        material.setDocumentStatus("C");
        return material;
    }

}
