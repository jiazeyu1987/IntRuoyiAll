package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordbookTemplateDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrRecordbookTemplateMapper extends BaseMapperX<MesProEdhrRecordbookTemplateDO> {

    default PageResult<MesProEdhrRecordbookTemplateDO> selectPage(MesProEdhrRecordbookTemplatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrRecordbookTemplateDO>()
                .likeIfPresent(MesProEdhrRecordbookTemplateDO::getTemplateCode, reqVO.getTemplateCode())
                .likeIfPresent(MesProEdhrRecordbookTemplateDO::getTemplateName, reqVO.getTemplateName())
                .eqIfPresent(MesProEdhrRecordbookTemplateDO::getRecordbookType, reqVO.getRecordbookType())
                .eqIfPresent(MesProEdhrRecordbookTemplateDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesProEdhrRecordbookTemplateDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesProEdhrRecordbookTemplateDO::getId));
    }

    default MesProEdhrRecordbookTemplateDO selectByTemplateCode(String templateCode) {
        return selectOne(MesProEdhrRecordbookTemplateDO::getTemplateCode, templateCode);
    }
}
