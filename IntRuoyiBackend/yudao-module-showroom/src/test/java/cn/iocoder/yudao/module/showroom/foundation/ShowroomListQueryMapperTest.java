package cn.iocoder.yudao.module.showroom.foundation;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomAdminController.PageQueryReqVO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductRevisionDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomListQueryMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductRevisionMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

@org.springframework.context.annotation.Import(cn.iocoder.yudao.module.showroom.support.ShowroomMysqlTextTestConfiguration.class)
class ShowroomListQueryMapperTest extends BaseDbUnitTest {
    @Resource private ShowroomListQueryMapper query;
    @Resource private ShowroomProductMapper products;
    @Resource private ShowroomProductRevisionMapper revisions;
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void filtersUsePublishedDisplayRevisionAndCountBeforePagingWithinVisibleTenantScope() {
        TenantContextHolder.setTenantId(122L);
        Long first = product("A",122L,"发布内容","草稿内容");
        Long second = product("B",122L,"发布内容二","未提交内容");
        Long third = product("C",122L,"不匹配","发布内容");
        product("X",123L,"发布内容","草稿");
        String filters = "[{\"key\":\"nameCn\",\"operator\":\"contains\",\"value\":\"发布内容\"},{\"key\":\"lifecycleText\",\"operator\":\"eq\",\"value\":\"已注册\"}]";
        var req = new PageQueryReqVO(null,1,1,null,null,null,null,null,null,filters);
        var page = query.productPage(req, Set.of(first,second,third));
        assertEquals(2L,page.getTotal());
        assertEquals(List.of(first),page.getRecords());
        var next = query.productPage(new PageQueryReqVO(null,2,1,null,null,null,null,null,null,filters),Set.of(first,second,third));
        assertEquals(2L,next.getTotal());
        assertEquals(List.of(second),next.getRecords());
        assertEquals(0L,query.productPage(req,Set.of()).getTotal());
        assertEquals(1L,query.productPage(req,Set.of(second)).getTotal());
    }
    private Long product(String code,Long tenant,String displayName,String latestName) {
        var product=ShowroomProductDO.builder().productCode(code).legacyProductCode(code).status("LIVE").incompleteFlag(false).build();
        product.setTenantId(tenant); products.insert(product);
        var display=revision(product.getId(),tenant,1,displayName);
        revision(product.getId(),tenant,2,latestName);
        product.setCurrentRevisionId(display.getId());product.setCurrentRevisionNo(1);products.updateById(product);
        return product.getId();
    }
    private ShowroomProductRevisionDO revision(Long product,Long tenant,int version,String name) {
        var revision=ShowroomProductRevisionDO.builder().productId(product).revisionNo(version).status("PUBLISHED")
                .nameCn(name).nameEn("English").ownerCompanyId(1L).productOwnerType("YINGTAI").lifecycleStage("REGISTERED").build();
        revision.setTenantId(tenant);revisions.insert(revision);return revision;
    }
}
