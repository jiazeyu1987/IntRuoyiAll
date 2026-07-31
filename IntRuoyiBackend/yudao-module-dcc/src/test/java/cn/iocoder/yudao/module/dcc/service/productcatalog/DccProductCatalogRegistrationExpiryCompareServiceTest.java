package cn.iocoder.yudao.module.dcc.service.productcatalog;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRegistrationExpiryCompareReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRegistrationExpiryCompareRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.productcatalog.DccProductCatalogDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog.DccProductCatalogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class DccProductCatalogRegistrationExpiryCompareServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DccProductCatalogServiceImpl service;

    @Mock
    private DccProductCatalogMapper productCatalogMapper;

    @Mock
    private DccRegistrationExpiryExternalPageClient externalPageClient;

    @Test
    void compareRegistrationExpiryShouldReturnMatchWhenNormalizedDatesEqual() {
        when(productCatalogMapper.selectAllInDisplayOrder())
                .thenReturn(List.of(row("瑛泰产品", 2, "2029.8.19", "https://example.com/nmpa-match")));
        when(externalPageClient.fetch("https://example.com/nmpa-match"))
                .thenReturn("批准日期 2024-08-20 有效期至 2029年8月19日 结构及组成");

        List<DccProductCatalogRegistrationExpiryCompareRespVO> result =
                service.compareRegistrationExpiry(req("瑛泰产品", 2));

        assertEquals(1, result.size());
        assertEquals("MATCH", result.get(0).getStatus());
        assertEquals("2029-08-19", result.get(0).getLocalExpiryDate());
        assertEquals("2029-08-19", result.get(0).getRemoteExpiryDate());
    }

    @Test
    void compareRegistrationExpiryShouldReturnMatchForNmpaHyphenDateWhenLocalUsesDots() {
        String nmpaUrl = "https://www.nmpa.gov.cn/datasearch/search-info.html"
                + "?nmpa=aWQ9MTdlYjFiMGM5OTg3OGQ2YjUyZTU4NTc1Y2JhNTlkNmYmaXRlbUlkPWZmODA4MDgxODNjYWQ3NTAwMTgzY2I2NmZlNjkwMjg1";
        when(productCatalogMapper.selectAllInDisplayOrder())
                .thenReturn(List.of(row("瑛泰产品", 88, "2027.9.8", nmpaUrl)));
        when(externalPageClient.fetch(nmpaUrl))
                .thenReturn("""
                        <html>
                          <body>
                            <table>
                              <tr><td>产品名称</td><td>心脏瓣膜球囊扩张导管</td></tr>
                              <tr><td>有效期</td><td>2027-09-08</td></tr>
                            </table>
                          </body>
                        </html>
                        """);

        List<DccProductCatalogRegistrationExpiryCompareRespVO> result =
                service.compareRegistrationExpiry(req("瑛泰产品", 88));

        assertEquals(1, result.size());
        assertEquals("MATCH", result.get(0).getStatus());
        assertEquals("2027-09-08", result.get(0).getLocalExpiryDate());
        assertEquals("2027-09-08", result.get(0).getRemoteExpiryDate());
    }

    @Test
    void compareRegistrationExpiryShouldReturnMismatchWhenDatesDiffer() {
        when(productCatalogMapper.selectAllInDisplayOrder())
                .thenReturn(List.of(row("瑛泰产品", 2, "2029/8/19", "https://example.com/nmpa-mismatch")));
        when(externalPageClient.fetch("https://example.com/nmpa-mismatch"))
                .thenReturn("有效期至：2030-08-19");

        List<DccProductCatalogRegistrationExpiryCompareRespVO> result =
                service.compareRegistrationExpiry(req("瑛泰产品", 2));

        assertEquals("MISMATCH", result.get(0).getStatus());
        assertEquals("2029-08-19", result.get(0).getLocalExpiryDate());
        assertEquals("2030-08-19", result.get(0).getRemoteExpiryDate());
    }

    @Test
    void compareRegistrationExpiryShouldReturnFetchFailedWhenExternalRequestFails() {
        when(productCatalogMapper.selectAllInDisplayOrder())
                .thenReturn(List.of(row("瑛泰产品", 2, "2029-08-19", "https://example.com/nmpa-412")));
        when(externalPageClient.fetch("https://example.com/nmpa-412"))
                .thenThrow(new DccRegistrationExpiryExternalPageFetchException("HTTP 412"));

        List<DccProductCatalogRegistrationExpiryCompareRespVO> result =
                service.compareRegistrationExpiry(req("瑛泰产品", 2));

        assertEquals("FETCH_FAILED", result.get(0).getStatus());
        assertEquals("HTTP 412", result.get(0).getMessage());
    }

    @Test
    void compareRegistrationExpiryShouldReturnNoLinkWithoutColoring() {
        when(productCatalogMapper.selectAllInDisplayOrder())
                .thenReturn(List.of(row("瑛泰产品", 2, "2029-08-19", null)));

        List<DccProductCatalogRegistrationExpiryCompareRespVO> result =
                service.compareRegistrationExpiry(req("瑛泰产品", 2));

        assertEquals("NO_LINK", result.get(0).getStatus());
    }

    @Test
    void compareRegistrationExpiryShouldReturnUnsupportedWhenPageHasNoExpiryDate() {
        when(productCatalogMapper.selectAllInDisplayOrder())
                .thenReturn(List.of(row("瑛泰产品", 2, "2029-08-19", "https://example.com/fda")));
        when(externalPageClient.fetch("https://example.com/fda"))
                .thenReturn("Decision Date 09/05/2025");

        List<DccProductCatalogRegistrationExpiryCompareRespVO> result =
                service.compareRegistrationExpiry(req("瑛泰产品", 2));

        assertEquals("UNSUPPORTED", result.get(0).getStatus());
    }

    @Test
    void compareRegistrationExpiryShouldFailFastWhenRowKeyIsMissing() {
        when(productCatalogMapper.selectAllInDisplayOrder())
                .thenReturn(List.of(row("瑛泰产品", 2, "2029-08-19", "https://example.com/nmpa")));

        assertThrows(ServiceException.class, () -> service.compareRegistrationExpiry(req("瑛泰产品", 999)));
    }

    @Test
    void compareRegistrationExpiryShouldFailFastWhenRowKeyIsDuplicated() {
        when(productCatalogMapper.selectAllInDisplayOrder())
                .thenReturn(List.of(
                        row("瑛泰产品", 2, "2029-08-19", "https://example.com/one"),
                        row("瑛泰产品", 2, "2029-08-19", "https://example.com/two")));

        assertThrows(ServiceException.class, () -> service.compareRegistrationExpiry(req("瑛泰产品", 2)));
    }

    private DccProductCatalogRegistrationExpiryCompareReqVO req(String dataSource, Integer originalRowNo) {
        DccProductCatalogRegistrationExpiryCompareReqVO reqVO =
                new DccProductCatalogRegistrationExpiryCompareReqVO();
        reqVO.setRows(List.of(new DccProductCatalogRegistrationExpiryCompareReqVO.RowKey(
                dataSource, originalRowNo)));
        return reqVO;
    }

    private DccProductCatalogDO row(String dataSource, Integer originalRowNo, String expiryDate,
                                    String registrationInfoLink) {
        return DccProductCatalogDO.builder()
                .dataSource(dataSource)
                .originalRowNo(originalRowNo)
                .product("测试产品")
                .expiryDate(expiryDate)
                .registrationInfoLink(registrationInfoLink)
                .build();
    }
}
