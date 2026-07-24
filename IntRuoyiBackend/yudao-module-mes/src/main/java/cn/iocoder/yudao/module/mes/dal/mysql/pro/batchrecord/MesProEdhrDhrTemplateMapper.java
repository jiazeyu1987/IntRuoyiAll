package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDhrTemplateDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrDhrTemplateMapper extends BaseMapperX<MesProEdhrDhrTemplateDO> {

    default PageResult<MesProEdhrDhrTemplateDO> selectPage(MesProEdhrDhrTemplatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrDhrTemplateDO>()
                .eqIfPresent(MesProEdhrDhrTemplateDO::getCatalogId, reqVO.getCatalogId())
                .likeIfPresent(MesProEdhrDhrTemplateDO::getTemplateCode, reqVO.getTemplateCode())
                .likeIfPresent(MesProEdhrDhrTemplateDO::getTemplateName, reqVO.getTemplateName())
                .eqIfPresent(MesProEdhrDhrTemplateDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MesProEdhrDhrTemplateDO::getReviewStatus, reqVO.getReviewStatus())
                .eqIfPresent(MesProEdhrDhrTemplateDO::getSignoffStatus, reqVO.getSignoffStatus())
                .betweenIfPresent(MesProEdhrDhrTemplateDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesProEdhrDhrTemplateDO::getId));
    }

    default MesProEdhrDhrTemplateDO selectByTemplateCode(String templateCode) {
        return selectOne(MesProEdhrDhrTemplateDO::getTemplateCode, templateCode);
    }
}
