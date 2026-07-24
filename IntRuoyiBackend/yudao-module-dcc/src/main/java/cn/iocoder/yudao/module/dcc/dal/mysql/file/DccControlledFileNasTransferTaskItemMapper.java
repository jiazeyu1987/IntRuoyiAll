package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasTransferTaskItemDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DccControlledFileNasTransferTaskItemMapper extends BaseMapperX<DccControlledFileNasTransferTaskItemDO> {

    default List<DccControlledFileNasTransferTaskItemDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<DccControlledFileNasTransferTaskItemDO>()
                .eq(DccControlledFileNasTransferTaskItemDO::getTaskId, taskId)
                .orderByAsc(DccControlledFileNasTransferTaskItemDO::getId));
    }

    default DccControlledFileNasTransferTaskItemDO selectFirstWaitingItemByTaskId(Long taskId) {
        return selectOne(new LambdaQueryWrapperX<DccControlledFileNasTransferTaskItemDO>()
                .eq(DccControlledFileNasTransferTaskItemDO::getTaskId, taskId)
                .eq(DccControlledFileNasTransferTaskItemDO::getStatus, "WAITING")
                .orderByAsc(DccControlledFileNasTransferTaskItemDO::getId)
                .last("LIMIT 1"));
    }

    default List<DccControlledFileNasTransferTaskItemDO> selectRunningItemsByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<DccControlledFileNasTransferTaskItemDO>()
                .eq(DccControlledFileNasTransferTaskItemDO::getTaskId, taskId)
                .eq(DccControlledFileNasTransferTaskItemDO::getStatus, "RUNNING")
                .orderByAsc(DccControlledFileNasTransferTaskItemDO::getId));
    }

    default List<DccControlledFileNasTransferTaskItemDO> selectFailedItemsByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<DccControlledFileNasTransferTaskItemDO>()
                .eq(DccControlledFileNasTransferTaskItemDO::getTaskId, taskId)
                .eq(DccControlledFileNasTransferTaskItemDO::getStatus, "FAILED")
                .orderByAsc(DccControlledFileNasTransferTaskItemDO::getId));
    }

    @Select("""
            SELECT COUNT(1)
            FROM dcc_controlled_file_nas_transfer_task_item
            WHERE task_id = #{taskId}
              AND item_type = #{itemType}
              AND directory_outcome = #{directoryOutcome}
              AND deleted = b'0'
            """)
    long selectCountByTaskIdAndItemTypeAndDirectoryOutcome(@Param("taskId") Long taskId,
                                                           @Param("itemType") String itemType,
                                                           @Param("directoryOutcome") String directoryOutcome);

    @Select("""
            SELECT COUNT(1)
            FROM dcc_controlled_file_nas_transfer_task_item
            WHERE task_id = #{taskId}
              AND item_type = #{itemType}
              AND category_outcome = #{categoryOutcome}
              AND deleted = b'0'
            """)
    long selectCountByTaskIdAndItemTypeAndCategoryOutcome(@Param("taskId") Long taskId,
                                                          @Param("itemType") String itemType,
                                                          @Param("categoryOutcome") String categoryOutcome);

    @Select("""
            SELECT COUNT(1)
            FROM dcc_controlled_file_nas_transfer_task_item
            WHERE task_id = #{taskId}
              AND item_type = 'FILE'
              AND status = 'COMPLETED'
              AND deleted = b'0'
            """)
    long selectCompletedFileCountByTaskId(@Param("taskId") Long taskId);

    @Select("""
            SELECT COUNT(1)
            FROM dcc_controlled_file_nas_transfer_task_item
            WHERE task_id = #{taskId}
              AND item_type = 'FILE'
              AND status = 'COMPLETED'
              AND preview_download_only = b'1'
              AND deleted = b'0'
            """)
    long selectPreviewDownloadOnlyCompletedFileCountByTaskId(@Param("taskId") Long taskId);

    default long selectPendingItemCountByTaskId(Long taskId) {
        return selectCount(new LambdaQueryWrapperX<DccControlledFileNasTransferTaskItemDO>()
                .eq(DccControlledFileNasTransferTaskItemDO::getTaskId, taskId)
                .in(DccControlledFileNasTransferTaskItemDO::getStatus, "WAITING", "RUNNING"));
    }

    default int claimWaitingItem(Long itemId) {
        return update(null, new LambdaUpdateWrapper<DccControlledFileNasTransferTaskItemDO>()
                .eq(DccControlledFileNasTransferTaskItemDO::getId, itemId)
                .eq(DccControlledFileNasTransferTaskItemDO::getStatus, "WAITING")
                .set(DccControlledFileNasTransferTaskItemDO::getStatus, "RUNNING"));
    }

    default int recoverRunningItemsToWaiting(Long taskId) {
        return update(null, new LambdaUpdateWrapper<DccControlledFileNasTransferTaskItemDO>()
                .eq(DccControlledFileNasTransferTaskItemDO::getTaskId, taskId)
                .eq(DccControlledFileNasTransferTaskItemDO::getStatus, "RUNNING")
                .set(DccControlledFileNasTransferTaskItemDO::getStatus, "WAITING")
                .set(DccControlledFileNasTransferTaskItemDO::getCompletedAt, null));
    }

    default int cancelWaitingItemsByTaskId(Long taskId, java.time.LocalDateTime completedAt) {
        return update(null, new LambdaUpdateWrapper<DccControlledFileNasTransferTaskItemDO>()
                .eq(DccControlledFileNasTransferTaskItemDO::getTaskId, taskId)
                .eq(DccControlledFileNasTransferTaskItemDO::getStatus, "WAITING")
                .set(DccControlledFileNasTransferTaskItemDO::getStatus, "CANCELLED")
                .set(DccControlledFileNasTransferTaskItemDO::getCompletedAt, completedAt));
    }

    default int cancelActiveItemsByTaskId(Long taskId, java.time.LocalDateTime completedAt) {
        return update(null, new LambdaUpdateWrapper<DccControlledFileNasTransferTaskItemDO>()
                .eq(DccControlledFileNasTransferTaskItemDO::getTaskId, taskId)
                .in(DccControlledFileNasTransferTaskItemDO::getStatus, "WAITING", "RUNNING")
                .set(DccControlledFileNasTransferTaskItemDO::getStatus, "CANCELLED")
                .set(DccControlledFileNasTransferTaskItemDO::getCompletedAt, completedAt));
    }
}
