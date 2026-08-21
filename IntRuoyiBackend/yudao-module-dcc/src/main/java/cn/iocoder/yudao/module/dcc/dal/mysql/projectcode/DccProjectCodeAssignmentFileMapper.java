package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentFilePageReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeAssignmentFileDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface DccProjectCodeAssignmentFileMapper extends BaseMapperX<DccProjectCodeAssignmentFileDO> {

    default DccProjectCodeAssignmentFileDO selectByAssignmentIdAndFileId(Long assignmentId, Long controlledFileId) {
        return selectOne(new LambdaQueryWrapperX<DccProjectCodeAssignmentFileDO>()
                .eq(DccProjectCodeAssignmentFileDO::getAssignmentId, assignmentId)
                .eq(DccProjectCodeAssignmentFileDO::getControlledFileId, controlledFileId));
    }

    default DccProjectCodeAssignmentFileDO selectByAssignmentIdAndMasterId(Long assignmentId, Long masterId) {
        return selectOne(new LambdaQueryWrapperX<DccProjectCodeAssignmentFileDO>()
                .eq(DccProjectCodeAssignmentFileDO::getAssignmentId, assignmentId)
                .eq(DccProjectCodeAssignmentFileDO::getMasterId, masterId));
    }

    default List<DccProjectCodeAssignmentFileDO> selectListByAssignmentId(Long assignmentId) {
        return selectList(new LambdaQueryWrapperX<DccProjectCodeAssignmentFileDO>()
                .eq(DccProjectCodeAssignmentFileDO::getAssignmentId, assignmentId)
                .orderByAsc(DccProjectCodeAssignmentFileDO::getId));
    }

    default List<DccProjectCodeAssignmentFileDO> selectListByAssignmentIds(Collection<Long> assignmentIds) {
        return selectList(new LambdaQueryWrapperX<DccProjectCodeAssignmentFileDO>()
                .inIfPresent(DccProjectCodeAssignmentFileDO::getAssignmentId, assignmentIds));
    }

    @Select("""
            SELECT DISTINCT latest_file.id
            FROM dcc_project_code_assignment_file assignment_file
            JOIN dcc_project_code_assignment assignment
              ON assignment.id = assignment_file.assignment_id
            JOIN (
                SELECT master_id,
                       MAX(id) AS latest_file_id
                FROM dcc_controlled_file
                WHERE deleted = 0
                  AND status IN ('ACTIVE', 'APPROVED')
                  AND master_id IS NOT NULL
                GROUP BY master_id
            ) latest
              ON latest.master_id = assignment_file.master_id
            JOIN dcc_controlled_file latest_file
              ON latest_file.id = latest.latest_file_id
            WHERE assignment_file.deleted = 0
              AND assignment.deleted = 0
              AND assignment.assignee_user_id = #{assigneeUserId}
              AND assignment.status = 'ACTIVE'
              AND (assignment.expire_time IS NULL OR assignment.expire_time &gt; #{now})
              AND assignment_file.master_id IS NOT NULL
            ORDER BY latest_file.id
            """)
    List<Long> selectActiveControlledFileIdsByAssigneeUserId(@Param("assigneeUserId") Long assigneeUserId,
                                                             @Param("now") LocalDateTime now);

    @Select("""
            SELECT DISTINCT latest_file.directory_id
            FROM dcc_project_code_assignment_file assignment_file
            JOIN dcc_project_code_assignment assignment
              ON assignment.id = assignment_file.assignment_id
            JOIN (
                SELECT master_id,
                       MAX(id) AS latest_file_id
                FROM dcc_controlled_file
                WHERE deleted = 0
                  AND status IN ('ACTIVE', 'APPROVED')
                  AND master_id IS NOT NULL
                GROUP BY master_id
            ) latest
              ON latest.master_id = assignment_file.master_id
            JOIN dcc_controlled_file latest_file
              ON latest_file.id = latest.latest_file_id
            WHERE assignment_file.deleted = 0
              AND assignment.deleted = 0
              AND assignment.assignee_user_id = #{assigneeUserId}
              AND assignment.status = 'ACTIVE'
              AND (assignment.expire_time IS NULL OR assignment.expire_time &gt; #{now})
              AND latest_file.directory_id IS NOT NULL
            ORDER BY latest_file.directory_id
            """)
    List<Long> selectActiveDirectoryIdsByAssigneeUserId(@Param("assigneeUserId") Long assigneeUserId,
                                                        @Param("now") LocalDateTime now);

    default PageResult<DccProjectCodeAssignmentFileDO> selectPage(Long assignmentId,
                                                                   DccProjectCodeAssignmentFilePageReqVO reqVO) {
        LambdaQueryWrapperX<DccProjectCodeAssignmentFileDO> wrapper =
                new LambdaQueryWrapperX<DccProjectCodeAssignmentFileDO>()
                        .eq(DccProjectCodeAssignmentFileDO::getAssignmentId, assignmentId)
                        .eqIfPresent(DccProjectCodeAssignmentFileDO::getChanged, reqVO.getChanged())
                        .eqIfPresent(DccProjectCodeAssignmentFileDO::getCategoryIdSnapshot, reqVO.getCategoryId())
                        .eqIfPresent(DccProjectCodeAssignmentFileDO::getInitialFileTypeLevel2, reqVO.getFileTypeLevel2())
                        .eqIfPresent(DccProjectCodeAssignmentFileDO::getInitialFileTypeLevel3, reqVO.getFileTypeLevel3());
        wrapper.orderByAsc(DccProjectCodeAssignmentFileDO::getChanged)
                .orderByDesc(DccProjectCodeAssignmentFileDO::getLastChangedTime)
                .orderByAsc(DccProjectCodeAssignmentFileDO::getId);
        String keyword = StrUtil.trimToNull(reqVO.getKeyword());
        if (keyword != null) {
            wrapper.and(item -> item.like(DccProjectCodeAssignmentFileDO::getFileNameSnapshot, keyword)
                    .or()
                    .like(DccProjectCodeAssignmentFileDO::getFileNumberSnapshot, keyword));
        }
        return selectPage(reqVO, wrapper);
    }

}
