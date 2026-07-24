package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqRunPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrValidationRunDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrValidationRunMapper extends BaseMapperX<MesProEdhrValidationRunDO> {

    default PageResult<MesProEdhrValidationRunDO> selectPage(MesProEdhrOqPqRunPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrValidationRunDO>()
                .eqIfPresent(MesProEdhrValidationRunDO::getPackageId, reqVO.getPackageId())
                .eqIfPresent(MesProEdhrValidationRunDO::getCaseId, reqVO.getCaseId())
                .eqIfPresent(MesProEdhrValidationRunDO::getCaseType, reqVO.getCaseType())
                .eqIfPresent(MesProEdhrValidationRunDO::getRunStatus, reqVO.getRunStatus())
                .likeIfPresent(MesProEdhrValidationRunDO::getRunCode, reqVO.getRunCode())
                .orderByDesc(MesProEdhrValidationRunDO::getId));
    }
}
