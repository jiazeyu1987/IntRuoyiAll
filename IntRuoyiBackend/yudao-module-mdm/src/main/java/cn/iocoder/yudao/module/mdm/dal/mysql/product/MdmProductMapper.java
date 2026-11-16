package cn.iocoder.yudao.module.mdm.dal.mysql.product;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductPageReqVO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.product.MdmProductDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import java.util.Map;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MdmProductMapper extends BaseMapperX<MdmProductDO> {

    default PageResult<MdmProductDO> selectPage(MdmProductPageReqVO reqVO) {
        String keyword = StrUtil.trimToNull(reqVO.getKeyword());
        LambdaQueryWrapperX<MdmProductDO> wrapper = new LambdaQueryWrapperX<MdmProductDO>()
                .eqIfPresent(MdmProductDO::getId, reqVO.getProductMasterId())
                .likeIfPresent(MdmProductDO::getProductCode, reqVO.getProductCode())
                .likeIfPresent(MdmProductDO::getDccProductCode, reqVO.getDccProductCode())
                .eqIfPresent(MdmProductDO::getStatus, reqVO.getStatus())
                .orderByDesc(MdmProductDO::getId);
        if (keyword != null) {
            wrapper.and(item -> item.like(MdmProductDO::getProductCode, keyword)
                    .or().like(MdmProductDO::getDccProductCode, keyword)
                    .or().like(MdmProductDO::getNameCn, keyword)
                    .or().like(MdmProductDO::getNameEn, keyword)
                    .or().like(MdmProductDO::getModelSpecification, keyword));
        }
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, wrapper, Map.ofEntries(
                Map.entry("productCode", text("product_code")),
                Map.entry("nameCn", text("name_cn")),
                Map.entry("nameEn", text("name_en")),
                Map.entry("modelSpecification", text("model_specification")),
                Map.entry("category", text("category")),
                Map.entry("status", text("CASE WHEN status = 'ENABLE' THEN '启用' ELSE '停用' END")),
                Map.entry("updateTime", dateTime("update_time"))
        )));
    }

    default MdmProductDO selectByProductCode(String productCode) {
        return selectOne(MdmProductDO::getProductCode, productCode);
    }

    default MdmProductDO selectByDccProductCode(String dccProductCode) {
        return selectOne(MdmProductDO::getDccProductCode, dccProductCode);
    }

    default List<MdmProductDO> selectListByProductCodes(Collection<String> productCodes) {
        if (productCodes == null || productCodes.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MdmProductDO>()
                .in(MdmProductDO::getProductCode, productCodes));
    }

    default List<MdmProductDO> selectSimpleList(String status, Boolean requireDccProductCode, String keyword) {
        String normalizedKeyword = StrUtil.trimToNull(keyword);
        LambdaQueryWrapperX<MdmProductDO> wrapper = new LambdaQueryWrapperX<MdmProductDO>()
                .eqIfPresent(MdmProductDO::getStatus, status);
        if (Boolean.TRUE.equals(requireDccProductCode)) {
            wrapper.isNotNull(MdmProductDO::getDccProductCode);
        }
        if (normalizedKeyword != null) {
            wrapper.and(item -> item.like(MdmProductDO::getProductCode, normalizedKeyword)
                    .or().like(MdmProductDO::getDccProductCode, normalizedKeyword)
                    .or().like(MdmProductDO::getNameCn, normalizedKeyword)
                    .or().like(MdmProductDO::getNameEn, normalizedKeyword));
        }
        wrapper.orderByAsc(MdmProductDO::getProductCode);
        return selectList(wrapper);
    }

}
