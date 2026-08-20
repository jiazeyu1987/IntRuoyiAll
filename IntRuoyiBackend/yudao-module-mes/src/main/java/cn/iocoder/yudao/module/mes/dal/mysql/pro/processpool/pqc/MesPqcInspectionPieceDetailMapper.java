package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
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

    default int deleteByTaskIds(Collection<Long> taskIds) {
        return taskIds == null || taskIds.isEmpty() ? 0 : physicalDeleteByTaskIds(taskIds);
    }

    @Delete({
            "<script>",
            "DELETE FROM mes_pqc_inspection_piece_detail WHERE task_id IN",
            "<foreach collection='taskIds' item='taskId' open='(' separator=',' close=')'>#{taskId}</foreach>",
            "</script>"
    })
    int physicalDeleteByTaskIds(@Param("taskIds") Collection<Long> taskIds);
}
