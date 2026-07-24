package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrValidationDeviationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrValidationDeviationMapper extends BaseMapperX<MesProEdhrValidationDeviationDO> {

    default PageResult<MesProEdhrValidationDeviationDO> selectPage(MesProEdhrOqPqDeviationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrValidationDeviationDO>()
                .eqIfPresent(MesProEdhrValidationDeviationDO::getPackageId, reqVO.getPackageId())
                .eqIfPresent(MesProEdhrValidationDeviationDO::getRunId, reqVO.getRunId())
                .eqIfPresent(MesProEdhrValidationDeviationDO::getDeviationStatus, reqVO.getDeviationStatus())
                .likeIfPresent(MesProEdhrValidationDeviationDO::getDeviationCode, reqVO.getDeviationCode())
                .orderByDesc(MesProEdhrValidationDeviationDO::getId));
    }

    default List<MesProEdhrValidationDeviationDO> selectOpenListByRunId(Long runId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrValidationDeviationDO>()
                .eq(MesProEdhrValidationDeviationDO::getRunId, runId)
                .ne(MesProEdhrValidationDeviationDO::getDeviationStatus, "CLOSED")
                .orderByDesc(MesProEdhrValidationDeviationDO::getId));
    }

    default int countOpenByRunId(Long runId) {
        return Math.toIntExact(selectCount(new LambdaQueryWrapperX<MesProEdhrValidationDeviationDO>()
                .eq(MesProEdhrValidationDeviationDO::getRunId, runId)
                .ne(MesProEdhrValidationDeviationDO::getDeviationStatus, "CLOSED")));
    }
}
