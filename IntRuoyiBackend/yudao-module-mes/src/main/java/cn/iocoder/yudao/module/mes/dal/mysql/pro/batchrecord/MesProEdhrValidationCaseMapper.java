package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqCasePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrValidationCaseDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrValidationCaseMapper extends BaseMapperX<MesProEdhrValidationCaseDO> {

    default PageResult<MesProEdhrValidationCaseDO> selectPage(MesProEdhrOqPqCasePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrValidationCaseDO>()
                .eqIfPresent(MesProEdhrValidationCaseDO::getPackageId, reqVO.getPackageId())
                .eqIfPresent(MesProEdhrValidationCaseDO::getCaseType, reqVO.getCaseType())
                .eqIfPresent(MesProEdhrValidationCaseDO::getCaseStatus, reqVO.getCaseStatus())
                .likeIfPresent(MesProEdhrValidationCaseDO::getCaseCode, reqVO.getCaseCode())
                .orderByAsc(MesProEdhrValidationCaseDO::getSort)
                .orderByDesc(MesProEdhrValidationCaseDO::getId));
    }
}
