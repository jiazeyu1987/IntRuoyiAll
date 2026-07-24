package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileLocalFolderUploadChunkDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccControlledFileLocalFolderUploadChunkMapper
        extends BaseMapperX<DccControlledFileLocalFolderUploadChunkDO> {

    default DccControlledFileLocalFolderUploadChunkDO selectByTaskIdAndRelativePathAndChunkIndex(
            Long taskId, String relativePath, Integer chunkIndex) {
        return selectOne(new LambdaQueryWrapperX<DccControlledFileLocalFolderUploadChunkDO>()
                .eq(DccControlledFileLocalFolderUploadChunkDO::getTaskId, taskId)
                .eq(DccControlledFileLocalFolderUploadChunkDO::getRelativePath, relativePath)
                .eq(DccControlledFileLocalFolderUploadChunkDO::getChunkIndex, chunkIndex));
    }

    default List<DccControlledFileLocalFolderUploadChunkDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<DccControlledFileLocalFolderUploadChunkDO>()
                .eq(DccControlledFileLocalFolderUploadChunkDO::getTaskId, taskId)
                .orderByAsc(DccControlledFileLocalFolderUploadChunkDO::getRelativePath)
                .orderByAsc(DccControlledFileLocalFolderUploadChunkDO::getChunkIndex));
    }

    default List<DccControlledFileLocalFolderUploadChunkDO> selectListByTaskIdAndRelativePath(
            Long taskId, String relativePath) {
        return selectList(new LambdaQueryWrapperX<DccControlledFileLocalFolderUploadChunkDO>()
                .eq(DccControlledFileLocalFolderUploadChunkDO::getTaskId, taskId)
                .eq(DccControlledFileLocalFolderUploadChunkDO::getRelativePath, relativePath)
                .orderByAsc(DccControlledFileLocalFolderUploadChunkDO::getChunkIndex));
    }
}
