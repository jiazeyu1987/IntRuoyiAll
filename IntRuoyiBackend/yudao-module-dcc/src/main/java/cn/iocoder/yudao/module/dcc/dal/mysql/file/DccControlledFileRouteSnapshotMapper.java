package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Comparator;
import java.util.List;

/**
 * DCC controlled file route snapshot mapper.
 */
@Mapper
public interface DccControlledFileRouteSnapshotMapper extends BaseMapperX<DccControlledFileRouteSnapshotDO> {

    default List<DccControlledFileRouteSnapshotDO> selectListByControlledFileId(Long controlledFileId) {
        return selectList(DccControlledFileRouteSnapshotDO::getControlledFileId, controlledFileId).stream()
                .sorted(Comparator.comparing(DccControlledFileRouteSnapshotDO::getStageNo))
                .toList();
    }
}
