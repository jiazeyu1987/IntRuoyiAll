package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

@Mapper
public interface MesPqcInspectionPieceDetailMapper extends BaseMapperX<MesPqcInspectionPieceDetailDO> {

    default List<MesPqcInspectionPieceDetailDO> selectListByTaskId(Long taskId) {
        if (taskId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesPqcInspectionPieceDetailDO>()
                .eq(MesPqcInspectionPieceDetailDO::getTaskId, taskId)
                .orderByAsc(MesPqcInspectionPieceDetailDO::getSampleNo)
                .orderByAsc(MesPqcInspectionPieceDetailDO::getId));
    }

    default int deleteByTaskId(Long taskId) {
        if (taskId == null) {
            return 0;
        }
        return delete(new LambdaQueryWrapperX<MesPqcInspectionPieceDetailDO>()
                .eq(MesPqcInspectionPieceDetailDO::getTaskId, taskId));
    }
}
