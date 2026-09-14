package cn.iocoder.yudao.module.erp.kingdeeautosync;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpProductionPickListFilterContractTest {

    private static final Path ERP_MAIN = Path.of("src/main/java/cn/iocoder/yudao/module/erp");

    @Test
    void pageRequestAndMapper_mustExposeProductionOrganizationFilters() throws IOException {
        String pageReq = read(ERP_MAIN.resolve(
                "controller/admin/production/vo/ErpProductionPickListPageReqVO.java"));
        String mapper = read(ERP_MAIN.resolve(
                "dal/mysql/production/kingdee/ErpKingdeeProductionPickListMapper.java"));
        String itemMapper = read(ERP_MAIN.resolve(
                "dal/mysql/production/kingdee/ErpKingdeeProductionPickListItemMapper.java"));
        String service = read(ERP_MAIN.resolve(
                "service/production/kingdee/ErpKingdeeProductionPickListServiceImpl.java"));

        for (String field : new String[]{
                "private String productionOrderNo;",
                "private String stockOrgName;",
                "private String productionOrgName;"
        }) {
            assertContains(pageReq, field);
        }
        assertContains(mapper, "ErpKingdeeProductionPickListDO::getStockOrgName");
        assertContains(mapper, "ErpKingdeeProductionPickListDO::getProductionOrgName");
        assertContains(mapper, "reqVO.getStockOrgName()");
        assertContains(mapper, "reqVO.getProductionOrgName()");
        assertContains(itemMapper, "selectPickListIdsByProductionOrderNo");
        assertContains(itemMapper, "getProductionOrderNo");
        assertContains(service, "selectPickListIdsByProductionOrderNo");
        assertContains(service, "productionPickListMapper.selectPageByProductionPickListIds");
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path);
    }

    private static void assertContains(String source, String token) {
        assertTrue(source.contains(token), "Missing token: " + token);
    }
}
