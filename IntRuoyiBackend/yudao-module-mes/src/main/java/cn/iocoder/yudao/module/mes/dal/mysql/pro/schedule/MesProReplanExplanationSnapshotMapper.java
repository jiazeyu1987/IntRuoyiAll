package cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProReplanExplanationSnapshotDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProReplanExplanationSnapshotMapper extends BaseMapperX<MesProReplanExplanationSnapshotDO> {

    default MesProReplanExplanationSnapshotDO selectLatest() {
        return selectOne(new LambdaQueryWrapperX<MesProReplanExplanationSnapshotDO>()
                .orderByDesc(MesProReplanExplanationSnapshotDO::getAppliedAt)
                .orderByDesc(MesProReplanExplanationSnapshotDO::getId)
                .last("LIMIT 1"));
    }
}
