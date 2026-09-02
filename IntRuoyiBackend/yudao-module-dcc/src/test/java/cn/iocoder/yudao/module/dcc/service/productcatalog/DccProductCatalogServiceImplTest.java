package cn.iocoder.yudao.module.dcc.service.productcatalog;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogTreeNodeRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogTreeReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogUpdateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.productcatalog.DccProductCatalogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog.DccProductCatalogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccProductCatalogServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DccProductCatalogServiceImpl service;

    @Mock
    private DccProductCatalogMapper productCatalogMapper;

    @Mock
    private DccProjectCodeMapper projectCodeMapper;

    @Test
    void getProductCatalogPageShouldReadDatabasePage() {
        DccProductCatalogPageReqVO reqVO = new DccProductCatalogPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setKeyword("翰凌");
        reqVO.setCategoryLevel1("输注、护理和防护器械");
        reqVO.setCategoryLevel2("止血带");
        reqVO.setProductStatus("S");
        reqVO.setDataSource("瑛泰产品");
        when(productCatalogMapper.selectPage(reqVO)).thenReturn(
                new PageResult<>(List.of(row(10L, "瑛泰产品", 4, "无源止血器")), 1L));

        PageResult<DccProductCatalogRespVO> result = service.getProductCatalogPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals("无源止血器", result.getList().get(0).getProduct());
        assertEquals("无源止血器项目", result.getList().get(0).getProjectName());
        assertEquals("WZY", result.getList().get(0).getProjectCode());
        assertEquals(4, result.getList().get(0).getOriginalRowNo());
        verify(productCatalogMapper).selectPage(reqVO);
    }

    @Test
    void getProductCatalogPageShouldExposeBatchRecordRecognitionJsonFromProjectCode() {
        DccProductCatalogPageReqVO reqVO = new DccProductCatalogPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        when(productCatalogMapper.selectPage(reqVO)).thenReturn(
                new PageResult<>(List.of(row(10L, "瑛泰产品", 4, "按压式球囊扩充压力泵")), 1L));
        when(projectCodeMapper.selectEnabledList()).thenReturn(List.of(DccProjectCodeDO.builder()
                .projectCode("WZY")
                .batchRecordTotalRecognitionJson("{\"sourceFileName\":\"RE-PP-IDI-01.docx\"}")
                .build()));

        PageResult<DccProductCatalogRespVO> result = service.getProductCatalogPage(reqVO);

        assertEquals("{\"sourceFileName\":\"RE-PP-IDI-01.docx\"}",
                result.getList().get(0).getBatchRecordTotalRecognitionJson());
        verify(projectCodeMapper).selectEnabledList();
    }

    @Test
    void getProductCatalogTreeShouldGroupRowsByThreeExcelCategoryColumns() {
        DccProductCatalogTreeReqVO reqVO = new DccProductCatalogTreeReqVO();
        reqVO.setDataSource("瑛泰产品");
        DccProductCatalogDO first = row(10L, "瑛泰产品", 2, "血管鞘（长鞘）");
        first.setCategoryLevel1("神经和心血管手术器械");
        first.setCategoryLevel2("鞘");
        first.setProductCode("0313140101");
        first.setRegistrationCertificateName("一次性使用血管鞘");
        DccProductCatalogDO second = row(11L, "瑛泰产品", 3, "血管鞘（长鞘）");
        second.setCategoryLevel1("神经和心血管手术器械");
        second.setCategoryLevel2("鞘");
        second.setProductCode("0313140102");
        second.setRegistrationCertificateName("Guiding Sheath 血管鞘");
        when(productCatalogMapper.selectTreeRows(reqVO)).thenReturn(List.of(first, second));

        List<DccProductCatalogTreeNodeRespVO> tree = service.getProductCatalogTree(reqVO);

        assertEquals(1, tree.size());
        DccProductCatalogTreeNodeRespVO level1 = tree.get(0);
        assertEquals("categoryLevel1", level1.getNodeType());
        assertEquals("神经和心血管手术器械", level1.getCategoryLevel1());
        assertEquals(1, level1.getChildren().size());
        DccProductCatalogTreeNodeRespVO level2 = level1.getChildren().get(0);
        assertEquals("categoryLevel2", level2.getNodeType());
        assertEquals("鞘", level2.getCategoryLevel2());
        DccProductCatalogTreeNodeRespVO product = level2.getChildren().get(0);
        assertEquals("product", product.getNodeType());
        assertEquals("血管鞘（长鞘）", product.getProduct());
        assertEquals(2, product.getChildren().size());
        assertEquals("detail", product.getChildren().get(0).getNodeType());
        assertEquals(2, product.getChildren().get(0).getOriginalRowNo());
        assertEquals("0313140101", product.getChildren().get(0).getProductCode());
        assertEquals(3, product.getChildren().get(1).getOriginalRowNo());
        verify(productCatalogMapper).selectTreeRows(reqVO);
    }

    @Test
    void createProductCatalogShouldInsertDatabaseRowWithNextSourceRowNumber() {
        DccProductCatalogSaveReqVO reqVO = request("瑛泰产品", "新增产品");
        when(productCatalogMapper.selectMaxOriginalRowNo("瑛泰产品")).thenReturn(181);
        doAnswer(invocation -> {
            invocation.<DccProductCatalogDO>getArgument(0).setId(100L);
            return 1;
        }).when(productCatalogMapper).insert(any(DccProductCatalogDO.class));

        DccProductCatalogRespVO result = service.createProductCatalog(reqVO);

        assertEquals(182, result.getOriginalRowNo());
        assertEquals("新增产品", result.getProduct());
        assertEquals("新增项目", result.getProjectName());
        assertEquals("NEW", result.getProjectCode());
        ArgumentCaptor<DccProductCatalogDO> captor = ArgumentCaptor.forClass(DccProductCatalogDO.class);
        verify(productCatalogMapper).insert(captor.capture());
        assertEquals(182, captor.getValue().getOriginalRowNo());
        assertEquals("瑛泰产品", captor.getValue().getDataSource());
        assertEquals("新增项目", captor.getValue().getProjectName());
        assertEquals("NEW", captor.getValue().getProjectCode());
    }

    @Test
    void updateProductCatalogShouldUpdateDatabaseRowByStableRowKey() {
        DccProductCatalogUpdateReqVO reqVO = new DccProductCatalogUpdateReqVO();
        copy(request("瑛泰产品", "更新产品"), reqVO);
        reqVO.setOriginalRowNo(2);
        when(productCatalogMapper.selectByRowKey("瑛泰产品", 2))
                .thenReturn(row(20L, "瑛泰产品", 2, "旧产品"));

        service.updateProductCatalog(reqVO);

        ArgumentCaptor<DccProductCatalogDO> captor = ArgumentCaptor.forClass(DccProductCatalogDO.class);
        verify(productCatalogMapper).updateById(captor.capture());
        assertEquals(20L, captor.getValue().getId());
        assertEquals("更新产品", captor.getValue().getProduct());
        assertEquals("新增项目", captor.getValue().getProjectName());
        assertEquals("NEW", captor.getValue().getProjectCode());
        assertEquals(2, captor.getValue().getOriginalRowNo());
    }

    @Test
    void deleteProductCatalogShouldDeleteDatabaseRowByStableRowKey() {
        when(productCatalogMapper.selectByRowKey("瑛泰产品", 8))
                .thenReturn(row(30L, "瑛泰产品", 8, "待删除产品"));

        service.deleteProductCatalog("瑛泰产品", 8);

        verify(productCatalogMapper).deleteById(30L);
    }

    private DccProductCatalogDO row(Long id, String dataSource, Integer originalRowNo, String product) {
        return DccProductCatalogDO.builder()
                .id(id)
                .dataSource(dataSource)
                .originalRowNo(originalRowNo)
                .categoryLevel1("分类 I")
                .categoryLevel2("分类 II")
                .productSequence("1")
                .product(product)
                .productCode("P-001")
                .projectName(product + "项目")
                .projectCode("WZY")
                .productStatus("S")
                .build();
    }

    private DccProductCatalogSaveReqVO request(String dataSource, String product) {
        DccProductCatalogSaveReqVO reqVO = new DccProductCatalogSaveReqVO();
        reqVO.setDataSource(dataSource);
        reqVO.setCategoryLevel1("分类 I");
        reqVO.setCategoryLevel2("分类 II");
        reqVO.setProductSequence("1");
        reqVO.setProduct(product);
        reqVO.setProductCode("P-001");
        reqVO.setProjectName("新增项目");
        reqVO.setProjectCode("NEW");
        reqVO.setProductStatus("S");
        return reqVO;
    }

    private void copy(DccProductCatalogSaveReqVO source, DccProductCatalogUpdateReqVO target) {
        target.setDataSource(source.getDataSource());
        target.setCategoryLevel1(source.getCategoryLevel1());
        target.setCategoryLevel2(source.getCategoryLevel2());
        target.setProductSequence(source.getProductSequence());
        target.setProduct(source.getProduct());
        target.setProductCode(source.getProductCode());
        target.setProjectName(source.getProjectName());
        target.setProjectCode(source.getProjectCode());
        target.setRegistrationCertificateName(source.getRegistrationCertificateName());
        target.setRegistrationCertificateNumber(source.getRegistrationCertificateNumber());
        target.setCertificateHolder(source.getCertificateHolder());
        target.setRegistrationPlace(source.getRegistrationPlace());
        target.setEffectiveDate(source.getEffectiveDate());
        target.setExpiryDate(source.getExpiryDate());
        target.setClassification(source.getClassification());
        target.setRegistrationInfoLink(source.getRegistrationInfoLink());
        target.setProductStatus(source.getProductStatus());
        target.setRemark(source.getRemark());
    }
}
