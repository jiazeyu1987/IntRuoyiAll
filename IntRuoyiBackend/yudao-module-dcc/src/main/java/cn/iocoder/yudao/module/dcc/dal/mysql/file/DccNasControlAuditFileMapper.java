package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasControlAuditFilePageReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccNasControlAuditFileDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DccNasControlAuditFileMapper extends BaseMapperX<DccNasControlAuditFileDO> {

    default PageResult<DccNasControlAuditFileDO> selectPage(Long taskId, DccNasControlAuditFilePageReqVO reqVO) {
        QueryWrapperX<DccNasControlAuditFileDO> wrapper = new QueryWrapperX<>();
        wrapper.eq("task_id", taskId)
                .eqIfPresent("classification_status", reqVO.getClassificationStatus())
                .eqIfPresent("download_status", reqVO.getDownloadStatus())
                .eqIfPresent("archive_status", reqVO.getArchiveStatus());
        String keyword = StrUtil.trimToNull(reqVO.getKeyword());
        if (keyword != null) {
            wrapper.and(item -> item.like("normalized_relative_path", keyword)
                    .or().like("file_name", keyword));
        }
        wrapper.orderByAsc("id");
        return selectPage(reqVO, wrapper);
    }

    default List<DccNasControlAuditFileDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<DccNasControlAuditFileDO>()
                .eq(DccNasControlAuditFileDO::getTaskId, taskId)
                .orderByAsc(DccNasControlAuditFileDO::getId));
    }

    default List<DccNasControlAuditFileDO> selectPendingRecognitionList(Long taskId) {
        return selectList(new LambdaQueryWrapperX<DccNasControlAuditFileDO>()
                .eq(DccNasControlAuditFileDO::getTaskId, taskId)
                .eq(DccNasControlAuditFileDO::getClassificationStatus, "PENDING_RECOGNITION")
                .orderByAsc(DccNasControlAuditFileDO::getId));
    }

    @Update("""
            UPDATE dcc_nas_control_audit_file
            SET original_path_sync_status = 'ORIGINAL_PATH_DELETED',
                original_path_sync_file_id = NULL,
                original_path_sync_error_code = NULL,
                original_path_sync_error = NULL,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{auditFileId}
              AND original_path_sync_file_id = #{syncFileId}
              AND deleted = b'0'
            """)
    int markOriginalPathSyncDeleted(@Param("auditFileId") Long auditFileId,
                                    @Param("syncFileId") Long syncFileId);
}
