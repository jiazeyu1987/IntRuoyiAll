package cn.iocoder.yudao.module.dcc.service.productcatalog;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogUpdateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.productcatalog.DccProductCatalogDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog.DccProductCatalogMapper;
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

    @Test
    void getProductCatalogPageShouldReadDatabasePage() {
        DccProductCatalogPageReqVO reqVO = new DccProductCatalogPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setKeyword("翰凌");
        reqVO.setCategoryLevel1("输注、护理和防护器械");
        reqVO.setCategoryLevel2("止血带");
        reqVO.setProductStatus("S");
        reqVO.setDataSource("子公司产品");
        when(productCatalogMapper.selectPage(reqVO)).thenReturn(
                new PageResult<>(List.of(row(10L, "子公司产品", 4, "无源止血器")), 1L));

        PageResult<DccProductCatalogRespVO> result = service.getProductCatalogPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals("无源止血器", result.getList().get(0).getProduct());
        assertEquals(4, result.getList().get(0).getOriginalRowNo());
        verify(productCatalogMapper).selectPage(reqVO);
    }

    @Test
    void createProductCatalogShouldInsertDatabaseRowWithNextSourceRowNumber() {
        DccProductCatalogSaveReqVO reqVO = request("子公司产品", "新增产品");
        when(productCatalogMapper.selectMaxOriginalRowNo("子公司产品")).thenReturn(33);
        doAnswer(invocation -> {
            invocation.<DccProductCatalogDO>getArgument(0).setId(100L);
            return 1;
        }).when(productCatalogMapper).insert(any(DccProductCatalogDO.class));

        DccProductCatalogRespVO result = service.createProductCatalog(reqVO);

        assertEquals(34, result.getOriginalRowNo());
        assertEquals("新增产品", result.getProduct());
        ArgumentCaptor<DccProductCatalogDO> captor = ArgumentCaptor.forClass(DccProductCatalogDO.class);
        verify(productCatalogMapper).insert(captor.capture());
        assertEquals(34, captor.getValue().getOriginalRowNo());
        assertEquals("子公司产品", captor.getValue().getDataSource());
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
        assertEquals(2, captor.getValue().getOriginalRowNo());
    }

    @Test
    void deleteProductCatalogShouldDeleteDatabaseRowByStableRowKey() {
        when(productCatalogMapper.selectByRowKey("子公司产品", 8))
                .thenReturn(row(30L, "子公司产品", 8, "待删除产品"));

        service.deleteProductCatalog("子公司产品", 8);

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
