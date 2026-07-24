package cn.iocoder.yudao.module.mdm.service.product;

import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductImportExcelVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductSaveReqVO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.product.MdmProductDO;
import cn.iocoder.yudao.module.mdm.dal.mysql.product.MdmProductImportBatchMapper;
import cn.iocoder.yudao.module.mdm.dal.mysql.product.MdmProductImportRowMapper;
import cn.iocoder.yudao.module.mdm.dal.mysql.product.MdmProductMapper;
import cn.iocoder.yudao.module.mdm.dal.mysql.product.MdmProductReferenceMapper;
import cn.iocoder.yudao.module.mdm.enums.MdmProductStatusConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MdmProductServiceImplTest {

    @Mock
    private MdmProductMapper productMapper;
    @Mock
    private MdmProductImportBatchMapper importBatchMapper;
    @Mock
    private MdmProductImportRowMapper importRowMapper;
    @Mock
    private MdmProductReferenceMapper referenceMapper;

    private MdmProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new MdmProductServiceImpl();
        ReflectionTestUtils.setField(productService, "productMapper", productMapper);
        ReflectionTestUtils.setField(productService, "importBatchMapper", importBatchMapper);
        ReflectionTestUtils.setField(productService, "importRowMapper", importRowMapper);
        ReflectionTestUtils.setField(productService, "referenceMapper", referenceMapper);
    }

    @Test
    void updateProductShouldAllowProductCodeChangeForSameMasterId() {
        MdmProductDO existing = MdmProductDO.builder()
                .id(100L)
                .productCode("OLD-CODE")
                .dccProductCode("A1234567890123")
                .nameCn("旧名称")
                .status(MdmProductStatusConstants.ENABLE)
                .build();
        when(productMapper.selectById(100L)).thenReturn(existing);
        when(productMapper.selectByProductCode("NEW-CODE")).thenReturn(null);
        when(productMapper.selectByDccProductCode("A1234567890123")).thenReturn(existing);

        MdmProductSaveReqVO reqVO = new MdmProductSaveReqVO();
        reqVO.setId(100L);
        reqVO.setProductCode("NEW-CODE");
        reqVO.setDccProductCode("A1234567890123");
        reqVO.setNameCn("新名称");
        reqVO.setNameEn("New Name");
        reqVO.setModelSpecification("M1");
        reqVO.setCategory("分类");
        reqVO.setStatus(MdmProductStatusConstants.ENABLE);

        productService.updateProduct(reqVO);

        ArgumentCaptor<MdmProductDO> updateCaptor = ArgumentCaptor.forClass(MdmProductDO.class);
        verify(productMapper).updateById(updateCaptor.capture());
        assertEquals(100L, updateCaptor.getValue().getId());
        assertEquals("NEW-CODE", updateCaptor.getValue().getProductCode());
        assertEquals("A1234567890123", updateCaptor.getValue().getDccProductCode());
        assertEquals("新名称", updateCaptor.getValue().getNameCn());
    }

    @Test
    void importFromShowroomWorkbookShouldUpsertOnlyWorkbookRows() {
        MdmProductDO existingInWorkbook = MdmProductDO.builder()
                .id(100L)
                .productCode("KEEP-001")
                .dccProductCode("A1234567890123")
                .nameCn("旧名称")
                .status(MdmProductStatusConstants.ENABLE)
                .build();
        MdmProductDO existingOutsideWorkbook = MdmProductDO.builder()
                .id(200L)
                .productCode("OUTSIDE-001")
                .dccProductCode("B1234567890123")
                .nameCn("不在展厅包内")
                .status(MdmProductStatusConstants.ENABLE)
                .build();
        when(productMapper.selectList()).thenReturn(List.of(existingInWorkbook, existingOutsideWorkbook));

        Map<String, Long> idsByCode = productService.importFromShowroomWorkbook(List.of(
                MdmProductImportExcelVO.builder()
                        .productCode("KEEP-001")
                        .dccProductCode("A1234567890123")
                        .nameCn("新名称")
                        .nameEn("Updated Name")
                        .modelSpecification("M1")
                        .category("分类")
                        .build()));

        assertEquals(Map.of("KEEP-001", 100L), idsByCode);
        ArgumentCaptor<MdmProductDO> updateCaptor = ArgumentCaptor.forClass(MdmProductDO.class);
        verify(productMapper).updateById(updateCaptor.capture());
        assertEquals(100L, updateCaptor.getValue().getId());
        assertEquals("新名称", updateCaptor.getValue().getNameCn());
        assertEquals(MdmProductStatusConstants.ENABLE, updateCaptor.getValue().getStatus());
        verify(productMapper).selectList();
        verifyNoInteractions(importBatchMapper, importRowMapper);
    }

    @Test
    void importFromShowroomWorkbookShouldCreateNewProductAndReturnId() {
        when(productMapper.selectList()).thenReturn(List.of());
        doAnswer(invocation -> {
            MdmProductDO product = invocation.getArgument(0);
            product.setId(300L);
            return 1;
        }).when(productMapper).insert(any(MdmProductDO.class));

        Map<String, Long> idsByCode = productService.importFromShowroomWorkbook(List.of(
                MdmProductImportExcelVO.builder()
                        .productCode("NEW-001")
                        .dccProductCode("C1234567890123")
                        .nameCn("新增产品")
                        .nameEn("New Product")
                        .modelSpecification("M2")
                        .category("分类")
                        .build()));

        ArgumentCaptor<MdmProductDO> insertCaptor = ArgumentCaptor.forClass(MdmProductDO.class);
        verify(productMapper).insert(insertCaptor.capture());
        assertEquals("NEW-001", insertCaptor.getValue().getProductCode());
        assertEquals("新增产品", insertCaptor.getValue().getNameCn());
        assertEquals(MdmProductStatusConstants.ENABLE, insertCaptor.getValue().getStatus());
        assertEquals(Map.of("NEW-001", insertCaptor.getValue().getId()), idsByCode);
        verifyNoInteractions(importBatchMapper, importRowMapper);
    }

}
