package cn.iocoder.yudao.module.dcc.service.productcatalog;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRegistrationExpiryCompareReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRegistrationExpiryCompareRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogUpdateReqVO;

import java.util.List;

public interface DccProductCatalogService {

    PageResult<DccProductCatalogRespVO> getProductCatalogPage(DccProductCatalogPageReqVO reqVO);

    DccProductCatalogRespVO createProductCatalog(DccProductCatalogSaveReqVO reqVO);

    void updateProductCatalog(DccProductCatalogUpdateReqVO reqVO);

    void deleteProductCatalog(String dataSource, Integer originalRowNo);

    List<DccProductCatalogRegistrationExpiryCompareRespVO> compareRegistrationExpiry(
            DccProductCatalogRegistrationExpiryCompareReqVO reqVO);
}
