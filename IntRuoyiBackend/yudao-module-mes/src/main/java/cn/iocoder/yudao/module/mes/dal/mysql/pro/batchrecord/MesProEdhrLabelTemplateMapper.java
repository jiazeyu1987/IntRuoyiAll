package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrLabelTemplateDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrLabelTemplateMapper extends BaseMapperX<MesProEdhrLabelTemplateDO> {

    default PageResult<MesProEdhrLabelTemplateDO> selectPage(MesProEdhrLabelTemplatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrLabelTemplateDO>()
                .likeIfPresent(MesProEdhrLabelTemplateDO::getTemplateCode, reqVO.getTemplateCode())
                .likeIfPresent(MesProEdhrLabelTemplateDO::getTemplateName, reqVO.getTemplateName())
                .eqIfPresent(MesProEdhrLabelTemplateDO::getBusinessObjectType, reqVO.getBusinessObjectType())
                .eqIfPresent(MesProEdhrLabelTemplateDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesProEdhrLabelTemplateDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesProEdhrLabelTemplateDO::getId));
    }

    default MesProEdhrLabelTemplateDO selectByTemplateCode(String templateCode) {
        return selectOne(MesProEdhrLabelTemplateDO::getTemplateCode, templateCode);
    }

    default MesProEdhrLabelTemplateDO selectActiveTemplate(String businessObjectType) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrLabelTemplateDO>()
                .eq(MesProEdhrLabelTemplateDO::getBusinessObjectType, businessObjectType)
                .eq(MesProEdhrLabelTemplateDO::getStatus, "ACTIVE"));
    }
}
