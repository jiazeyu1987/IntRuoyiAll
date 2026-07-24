package cn.iocoder.yudao.module.showroom.dal.mysql.release;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleaseSourceSnapshotDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShowroomReleaseSourceSnapshotMapper extends BaseMapperX<ShowroomReleaseSourceSnapshotDO> {

    default ShowroomReleaseSourceSnapshotDO selectByReleaseId(String releaseId) {
        return selectOne(new LambdaQueryWrapperX<ShowroomReleaseSourceSnapshotDO>()
                .eq(ShowroomReleaseSourceSnapshotDO::getReleaseId, releaseId)
                .last("LIMIT 1"));
    }
}
