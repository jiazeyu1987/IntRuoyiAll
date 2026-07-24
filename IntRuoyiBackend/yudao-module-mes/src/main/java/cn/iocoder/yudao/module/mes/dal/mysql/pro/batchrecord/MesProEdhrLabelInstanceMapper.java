package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelInstancePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrLabelInstanceDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrLabelInstanceMapper extends BaseMapperX<MesProEdhrLabelInstanceDO> {

    default PageResult<MesProEdhrLabelInstanceDO> selectPage(MesProEdhrLabelInstancePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrLabelInstanceDO>()
                .likeIfPresent(MesProEdhrLabelInstanceDO::getLabelCode, reqVO.getLabelCode())
                .eqIfPresent(MesProEdhrLabelInstanceDO::getTemplateId, reqVO.getTemplateId())
                .eqIfPresent(MesProEdhrLabelInstanceDO::getBusinessType, reqVO.getBusinessType())
                .eqIfPresent(MesProEdhrLabelInstanceDO::getBusinessObjectId, reqVO.getBusinessObjectId())
                .likeIfPresent(MesProEdhrLabelInstanceDO::getBusinessObjectCode, reqVO.getBusinessObjectCode())
                .eqIfPresent(MesProEdhrLabelInstanceDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MesProEdhrLabelInstanceDO::getPrintStatus, reqVO.getPrintStatus())
                .betweenIfPresent(MesProEdhrLabelInstanceDO::getGeneratedAt, reqVO.getGeneratedAt())
                .orderByDesc(MesProEdhrLabelInstanceDO::getId));
    }

    default MesProEdhrLabelInstanceDO selectByBusinessKeyHash(String businessKeyHash) {
        return selectOne(MesProEdhrLabelInstanceDO::getBusinessKeyHash, businessKeyHash);
    }
}
