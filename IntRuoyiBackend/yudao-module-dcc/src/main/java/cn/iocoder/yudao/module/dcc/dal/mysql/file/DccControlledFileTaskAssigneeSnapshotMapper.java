package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTaskAssigneeSnapshotDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Comparator;
import java.util.List;

/**
 * DCC controlled file task assignee snapshot mapper.
 */
@Mapper
public interface DccControlledFileTaskAssigneeSnapshotMapper
        extends BaseMapperX<DccControlledFileTaskAssigneeSnapshotDO> {

    default List<DccControlledFileTaskAssigneeSnapshotDO> selectListByControlledFileId(Long controlledFileId) {
        return selectList(DccControlledFileTaskAssigneeSnapshotDO::getControlledFileId, controlledFileId).stream()
                .sorted(Comparator.comparing(DccControlledFileTaskAssigneeSnapshotDO::getStageNo,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccControlledFileTaskAssigneeSnapshotDO::getDepartmentId,
                                Comparator.nullsLast(Long::compareTo))
                        .thenComparing(DccControlledFileTaskAssigneeSnapshotDO::getId,
                                Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    default int bindBpmTaskByObligationId(Long controlledFileId, String stageCode, String obligationId,
                                          String bpmTaskId, String nodeInstanceId) {
        return update(null, new LambdaUpdateWrapper<DccControlledFileTaskAssigneeSnapshotDO>()
                .eq(DccControlledFileTaskAssigneeSnapshotDO::getControlledFileId, controlledFileId)
                .eq(DccControlledFileTaskAssigneeSnapshotDO::getStageCode, stageCode)
                .eq(DccControlledFileTaskAssigneeSnapshotDO::getObligationId, obligationId)
                .and(wrapper -> wrapper.isNull(DccControlledFileTaskAssigneeSnapshotDO::getBpmTaskId)
                        .or().eq(DccControlledFileTaskAssigneeSnapshotDO::getBpmTaskId, bpmTaskId))
                .set(DccControlledFileTaskAssigneeSnapshotDO::getBpmTaskId, bpmTaskId)
                .set(DccControlledFileTaskAssigneeSnapshotDO::getNodeInstanceId, nodeInstanceId));
    }
}
