package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordChangeEventDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrRecordChangeEventMapper extends BaseMapperX<MesProEdhrRecordChangeEventDO> {

    default PageResult<MesProEdhrRecordChangeEventDO> selectPage(EdhrRecordChangePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrRecordChangeEventDO>()
                .eqIfPresent(MesProEdhrRecordChangeEventDO::getChangeType, reqVO.getChangeType())
                .eqIfPresent(MesProEdhrRecordChangeEventDO::getTargetScope, reqVO.getTargetScope())
                .eqIfPresent(MesProEdhrRecordChangeEventDO::getBatchExecutionId, reqVO.getBatchExecutionId())
                .eqIfPresent(MesProEdhrRecordChangeEventDO::getExecutionId, reqVO.getExecutionId())
                .eqIfPresent(MesProEdhrRecordChangeEventDO::getChangeStatus, reqVO.getChangeStatus())
                .orderByDesc(MesProEdhrRecordChangeEventDO::getId));
    }

}
