package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccNasControlAuditSkippedDirectoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccNasControlAuditSkippedDirectoryMapper
        extends BaseMapperX<DccNasControlAuditSkippedDirectoryDO> {

    default List<DccNasControlAuditSkippedDirectoryDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<DccNasControlAuditSkippedDirectoryDO>()
                .eq(DccNasControlAuditSkippedDirectoryDO::getTaskId, taskId)
                .orderByAsc(DccNasControlAuditSkippedDirectoryDO::getId));
    }
}
