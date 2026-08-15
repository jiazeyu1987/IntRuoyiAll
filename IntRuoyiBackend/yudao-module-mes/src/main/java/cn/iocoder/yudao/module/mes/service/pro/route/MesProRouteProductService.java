package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductCopyReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * MES 工艺路线产品 Service 接口
 *
 * @author 瑛泰源码
 */
public interface MesProRouteProductService {

    /**
     * 创建工艺路线产品
     */
    Long createRouteProduct(@Valid MesProRouteProductSaveReqVO createReqVO);

    /**
     * 复制工艺路线产品
     */
    Long copyRouteProduct(@Valid MesProRouteProductCopyReqVO copyReqVO);

    /**
     * 按候选快照中的正式产品身份复制产品
     */
    Long copyCandidateRouteProduct(Long routeId, Long routeVersionId, Long sourceItemId, Long targetItemId);

    /**
     * 更新工艺路线产品
     */
    void updateRouteProduct(@Valid MesProRouteProductSaveReqVO updateReqVO);

    /**
     * 删除工艺路线产品
     */
    void deleteRouteProduct(Long id, Long routeVersionId);

    /**
     * 按候选快照中的正式产品身份删除产品
     */
    void deleteCandidateRouteProduct(Long routeId, Long itemId, Long routeVersionId);

    /**
     * 获得工艺路线产品
     */
    MesProRouteProductDO getRouteProduct(Long id);

    /**
     * 按产品（物料）获得工艺路线产品
     *
     * @param itemId 产品编号
     * @return 工艺路线产品，如果未配置则返回 null
     */
    MesProRouteProductDO getRouteProductByItemId(Long itemId);

    /**
     * 按产品（物料）保存工艺路线绑定
     *
     * @param itemId 产品编号
     * @param routeId 工艺路线编号；为空时解除绑定
     * @return 工艺路线产品编号；解除绑定时返回 null
     */
    Long saveRouteProductByItem(Long itemId, Long routeId);

    /**
     * QA 规程按产品（物料）绑定已发布工艺路线
     *
     * @param itemId 产品编号
     * @param routeId 工艺路线编号
     * @return 工艺路线产品编号
     */
    Long saveQaRegulationRouteProductByItem(Long itemId, Long routeId);

    /**
     * 按工艺路线获得产品列表
     */
    List<MesProRouteProductDO> getRouteProductListByRouteId(Long routeId);

    /**
     * 按工艺路线及指定版本获得产品列表；版本为空时读取正式关系
     */
    List<MesProRouteProductDO> getRouteProductListByRouteId(Long routeId, Long routeVersionId);

    /**
     * 按工艺路线删除产品（级联删除使用）
     *
     * @param routeId 工艺路线编号
     */
    void deleteRouteProductByRouteId(Long routeId);

}
