package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QuickFilterUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentCandidatePageReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * DCC controlled file mapper.
 */
@Mapper
public interface DccControlledFileMapper extends BaseMapperX<DccControlledFileDO> {

    @Update("""
            UPDATE dcc_controlled_file
            SET checked_out_by = #{actorId},
                checked_out_time = CURRENT_TIMESTAMP,
                updater = #{actorId},
                update_time = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND id = #{controlledFileId}
              AND deleted = 0
              AND checked_out_by IS NULL
            """)
    int checkoutByIdAndTenantWhenAvailable(@Param("tenantId") Long tenantId,
                                           @Param("controlledFileId") Long controlledFileId,
                                           @Param("actorId") Long actorId);

    @Update("""
            UPDATE dcc_controlled_file
            SET checked_out_by = NULL,
                checked_out_time = NULL,
                updater = #{actorId},
                update_time = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND id = #{controlledFileId}
              AND deleted = 0
              AND checked_out_by = #{actorId}
            """)
    int checkinByIdAndTenantWhenOwner(@Param("tenantId") Long tenantId,
                                      @Param("controlledFileId") Long controlledFileId,
                                      @Param("actorId") Long actorId);

    @Select("""
            SELECT id,
                   tenant_id,
                   title,
                   file_number,
                   status,
                   process_instance_id,
                   process_definition_key,
                   requester_id,
                   submitted_time,
                   approved_time,
                   rejected_time,
                   stamped_time,
                   create_time,
                   update_time,
                   creator,
                   updater,
                   deleted
            FROM dcc_controlled_file
            WHERE id = #{id}
            LIMIT 1
            """)
    DccControlledFileDO selectByIdIncludingDeleted(@Param("id") Long id);

    default PageResult<DccControlledFileDO> selectWorkflowPage(DccControlledFilePageReqVO reqVO) {
        return selectPage(reqVO, buildWorkflowQuery(reqVO));
    }

    default PageResult<DccControlledFileDO> selectWorkflowPage(DccControlledFilePageReqVO reqVO,
                                                               Collection<Long> visibleDirectoryIds) {
        return selectPage(reqVO, buildWorkflowQuery(reqVO).inIfPresent(DccControlledFileDO::getDirectoryId, visibleDirectoryIds));
    }

    default List<DccControlledFileDO> selectWorkflowList(DccControlledFilePageReqVO reqVO) {
        return selectList(buildWorkflowQuery(reqVO));
    }

    default List<DccControlledFileDO> selectWorkflowList(DccControlledFilePageReqVO reqVO,
                                                          Collection<Long> visibleDirectoryIds) {
        return selectWorkflowList(reqVO, visibleDirectoryIds, null);
    }

    default List<DccControlledFileDO> selectWorkflowList(DccControlledFilePageReqVO reqVO,
                                                          Collection<Long> visibleDirectoryIds,
                                                          Collection<Long> visibleFileIds) {
        return selectList(buildWorkflowQuery(reqVO)
                .inIfPresent(DccControlledFileDO::getDirectoryId, visibleDirectoryIds)
                .inIfPresent(DccControlledFileDO::getId, visibleFileIds));
    }

    default PageResult<DccControlledFileDO> selectBrowserSummaryPage(DccControlledFilePageReqVO reqVO) {
        return selectPage(reqVO, buildBrowserSummaryQuery(reqVO));
    }

    default PageResult<DccControlledFileDO> selectBrowserSummaryPage(DccControlledFilePageReqVO reqVO,
                                                                     Collection<Long> visibleDirectoryIds) {
        return selectPage(reqVO, buildBrowserSummaryQuery(reqVO)
                .inIfPresent(DccControlledFileDO::getDirectoryId, visibleDirectoryIds));
    }

    default List<DccControlledFileDO> selectBrowserSummaryList(DccControlledFilePageReqVO reqVO) {
        return selectList(buildBrowserSummaryQuery(reqVO));
    }

    default List<DccControlledFileDO> selectBrowserSummaryList(DccControlledFilePageReqVO reqVO,
                                                               Collection<Long> visibleDirectoryIds) {
        return selectBrowserSummaryList(reqVO, visibleDirectoryIds, null);
    }

    default List<DccControlledFileDO> selectBrowserSummaryList(DccControlledFilePageReqVO reqVO,
                                                               Collection<Long> visibleDirectoryIds,
                                                               Collection<Long> visibleFileIds) {
        return selectList(buildBrowserSummaryQuery(reqVO)
                .inIfPresent(DccControlledFileDO::getDirectoryId, visibleDirectoryIds)
                .inIfPresent(DccControlledFileDO::getId, visibleFileIds));
    }

    default List<DccControlledFileDO> selectListByMasterId(Long masterId) {
        return selectList(DccControlledFileDO::getMasterId, masterId);
    }

    default DccControlledFileDO selectLatestApprovedByMasterId(Long masterId) {
        if (masterId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<DccControlledFileDO>()
                .eq(DccControlledFileDO::getMasterId, masterId)
                .in(DccControlledFileDO::getStatus, List.of(
                        DccControlledFileStatusEnum.ACTIVE.getStatus(),
                        DccControlledFileStatusEnum.APPROVED.getStatus()))
                .orderByDesc(DccControlledFileDO::getId)
                .last("LIMIT 1"));
    }

    default List<DccControlledFileDO> selectCurrentApprovedFilesByIds(Collection<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        return selectCurrentApprovedFilesByIds0(fileIds);
    }

    @Select("""
            <script>
            SELECT controlled_file.*
            FROM dcc_controlled_file controlled_file
            WHERE controlled_file.deleted = 0
              AND controlled_file.master_id IS NOT NULL
              AND controlled_file.status IN ('ACTIVE', 'APPROVED')
              AND controlled_file.id IN
              <foreach collection="fileIds" item="fileId" open="(" separator="," close=")">
                  #{fileId}
              </foreach>
              AND NOT EXISTS (
                  SELECT 1
                  FROM dcc_controlled_file newer_file
                  WHERE newer_file.deleted = 0
                    AND newer_file.master_id = controlled_file.master_id
                    AND newer_file.status IN ('ACTIVE', 'APPROVED')
                    AND newer_file.id &gt; controlled_file.id
              )
            ORDER BY controlled_file.id
            </script>
            """)
    List<DccControlledFileDO> selectCurrentApprovedFilesByIds0(@Param("fileIds") Collection<Long> fileIds);

    default PageResult<DccControlledFileDO> selectAssignmentCandidatePage(
            DccProjectCodeAssignmentCandidatePageReqVO reqVO) {
        String keyword = StrUtil.trimToNull(reqVO.getKeyword());
        LambdaQueryWrapperX<DccControlledFileDO> query = new LambdaQueryWrapperX<DccControlledFileDO>()
                .in(DccControlledFileDO::getStatus, List.of(
                        DccControlledFileStatusEnum.ACTIVE.getStatus(),
                        DccControlledFileStatusEnum.APPROVED.getStatus(),
                        DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus()));
        query.isNotNull(DccControlledFileDO::getMasterId);
        if (keyword != null) {
            query.and(wrapper -> wrapper.like(DccControlledFileDO::getFileName, keyword)
                    .or().like(DccControlledFileDO::getTitle, keyword)
                    .or().like(DccControlledFileDO::getFileNumber, keyword));
        }
        query.apply("""
                NOT EXISTS (
                    SELECT 1
                    FROM dcc_controlled_file newer_file
                    WHERE newer_file.deleted = 0
                      AND newer_file.master_id = dcc_controlled_file.master_id
                      AND newer_file.status IN ('ACTIVE', 'APPROVED', 'PENDING_DOC_CONTROL_REVIEW')
                      AND newer_file.id > dcc_controlled_file.id
                )
                """);
        query.orderByDesc(DccControlledFileDO::getCreateTime)
                .orderByDesc(DccControlledFileDO::getId);
        return selectPage(reqVO, query);
    }

    default long selectCountByReferencedFileId(Long fileId) {
        if (fileId == null) {
            return 0L;
        }
        return selectCount(new LambdaQueryWrapperX<DccControlledFileDO>()
                .and(wrapper -> wrapper.eq(DccControlledFileDO::getSourceFileId, fileId)
                        .or()
                        .eq(DccControlledFileDO::getOriginalFileId, fileId)
                        .or()
                        .eq(DccControlledFileDO::getDrawingPdfFileId, fileId)
                        .or()
                        .eq(DccControlledFileDO::getTrainingRecordFileId, fileId)
                        .or()
                        .eq(DccControlledFileDO::getPublishedFileId, fileId)
                        .or()
                        .eq(DccControlledFileDO::getStampedFileId, fileId)));
    }

    @Select("""
            SELECT COUNT(1)
            FROM dcc_controlled_file
            WHERE tenant_id = #{tenantId}
              AND source_file_id = #{sourceFileId}
            """)
    long countAllBySourceFileId(@Param("tenantId") Long tenantId,
                                @Param("sourceFileId") Long sourceFileId);

    @Select("""
            SELECT controlled_file.*
            FROM dcc_controlled_file controlled_file
            LEFT JOIN dcc_controlled_file_source_ownership source_owner
              ON source_owner.tenant_id = controlled_file.tenant_id
             AND source_owner.controlled_file_id = controlled_file.id
             AND source_owner.deleted = 0
            LEFT JOIN dcc_controlled_file_source_migration source_migration
              ON source_migration.tenant_id = controlled_file.tenant_id
             AND source_migration.controlled_file_id = controlled_file.id
             AND source_migration.deleted = 0
            WHERE controlled_file.tenant_id = #{tenantId}
              AND controlled_file.source_file_id IS NOT NULL
              AND source_owner.id IS NULL
            ORDER BY CASE WHEN source_migration.migration_status = 'FAILED' THEN 1 ELSE 0 END,
                     controlled_file.source_file_id,
                     controlled_file.id
            LIMIT #{limit}
            """)
    List<DccControlledFileDO> selectUnownedSourceReferences(@Param("tenantId") Long tenantId,
                                                            @Param("limit") int limit);

    @Select("""
            SELECT controlled_file.*
            FROM dcc_controlled_file controlled_file
            LEFT JOIN dcc_controlled_file_source_ownership source_owner
              ON source_owner.tenant_id = controlled_file.tenant_id
             AND source_owner.controlled_file_id = controlled_file.id
             AND source_owner.deleted = 0
            LEFT JOIN dcc_controlled_file_source_migration source_migration
              ON source_migration.tenant_id = controlled_file.tenant_id
             AND source_migration.controlled_file_id = controlled_file.id
             AND source_migration.deleted = 0
            WHERE controlled_file.tenant_id = #{tenantId}
              AND controlled_file.id <= #{snapshotMaxControlledFileId}
              AND controlled_file.deleted = 0
              AND controlled_file.source_file_id IS NOT NULL
              AND source_owner.id IS NULL
            ORDER BY CASE WHEN source_migration.migration_status = 'FAILED' THEN 1 ELSE 0 END,
                     controlled_file.source_file_id,
                     controlled_file.id
            LIMIT #{limit}
            """)
    List<DccControlledFileDO> selectEffectiveUnownedSourceReferences(
            @Param("tenantId") Long tenantId,
            @Param("snapshotMaxControlledFileId") Long snapshotMaxControlledFileId,
            @Param("limit") int limit);

    @TenantIgnore
    @Select("""
            SELECT id AS controlled_file_id,
                   tenant_id,
                   source_file_id
            FROM dcc_controlled_file
            WHERE source_file_id = #{sourceFileId}
              AND id <= #{snapshotMaxControlledFileId}
              AND deleted = 0
            ORDER BY tenant_id, id
            """)
    List<GlobalSourceReference> selectGlobalEffectiveSourceReferences(
            @Param("sourceFileId") Long sourceFileId,
            @Param("snapshotMaxControlledFileId") Long snapshotMaxControlledFileId);

    @Select("""
            SELECT *
            FROM dcc_controlled_file
            WHERE tenant_id = #{tenantId}
              AND id = #{controlledFileId}
            LIMIT 1
            """)
    DccControlledFileDO selectByIdAndTenantIncludingDeleted(@Param("tenantId") Long tenantId,
                                                             @Param("controlledFileId") Long controlledFileId);

    @Update("""
            UPDATE dcc_controlled_file
            SET source_file_id = #{isolatedSourceFileId},
                updater = #{actorId},
                update_time = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND id = #{controlledFileId}
              AND source_file_id = #{legacySourceFileId}
            """)
    int updateSourceFileIdIncludingDeleted(@Param("tenantId") Long tenantId,
                                            @Param("controlledFileId") Long controlledFileId,
                                            @Param("legacySourceFileId") Long legacySourceFileId,
                                            @Param("isolatedSourceFileId") Long isolatedSourceFileId,
                                            @Param("actorId") Long actorId);

    @Select("""
            SELECT COUNT(1)
            FROM dcc_controlled_file
            WHERE tenant_id = #{tenantId}
              AND source_file_id IS NOT NULL
            """)
    long countAllSourceReferences(@Param("tenantId") Long tenantId);

    @Select("""
            SELECT COUNT(1)
            FROM dcc_controlled_file controlled_file
            LEFT JOIN dcc_controlled_file_source_ownership source_owner
              ON source_owner.tenant_id = controlled_file.tenant_id
             AND source_owner.controlled_file_id = controlled_file.id
             AND source_owner.deleted = 0
            WHERE controlled_file.tenant_id = #{tenantId}
              AND controlled_file.source_file_id IS NOT NULL
              AND source_owner.id IS NULL
            """)
    long countUnownedSourceReferences(@Param("tenantId") Long tenantId);

    @Select("""
            SELECT COUNT(1)
            FROM (
                SELECT source_file_id
                FROM dcc_controlled_file
                WHERE tenant_id = #{tenantId}
                  AND source_file_id IS NOT NULL
                GROUP BY source_file_id
                HAVING COUNT(1) > 1
            ) shared_source
            """)
    long countSharedSourceGroups(@Param("tenantId") Long tenantId);

    @Select("""
            SELECT COALESCE(SUM(shared_source.reference_count), 0)
            FROM (
                SELECT COUNT(1) AS reference_count
                FROM dcc_controlled_file
                WHERE tenant_id = #{tenantId}
                  AND source_file_id IS NOT NULL
                GROUP BY source_file_id
                HAVING COUNT(1) > 1
            ) shared_source
            """)
    long countSharedSourceRecords(@Param("tenantId") Long tenantId);

    @Select("""
            <script>
            SELECT effective_project_code_id AS projectCodeId,
                   COUNT(*) AS fileCount
            FROM (
                SELECT controlled_file.id,
                       COALESCE(controlled_file.dcc_project_code_id, latest_recognition.matched_project_code_id)
                         AS effective_project_code_id
                FROM dcc_controlled_file controlled_file
                LEFT JOIN (
                    SELECT recognition_record.controlled_file_id,
                           recognition_record.matched_project_code_id
                    FROM dcc_controlled_file_recognition_record recognition_record
                    JOIN (
                        SELECT controlled_file_id,
                               MAX(id) AS latest_id
                        FROM dcc_controlled_file_recognition_record
                        WHERE deleted = 0
                          AND status = 'SUCCESS'
                          AND matched_project_code_id IS NOT NULL
                        GROUP BY controlled_file_id
                    ) latest
                      ON latest.latest_id = recognition_record.id
                ) latest_recognition
                  ON latest_recognition.controlled_file_id = controlled_file.id
                WHERE controlled_file.deleted = 0
            ) associated_file
            WHERE effective_project_code_id IS NOT NULL
              AND effective_project_code_id IN
              <foreach collection="projectCodeIds" item="projectCodeId" open="(" separator="," close=")">
                  #{projectCodeId}
              </foreach>
            GROUP BY effective_project_code_id
            ORDER BY effective_project_code_id
            </script>
            """)
    List<ProjectCodeFileCount> selectAssociatedFileCountsByProjectCodeIds(
            @Param("projectCodeIds") Collection<Long> projectCodeIds);

    @Select("""
            <script>
            SELECT controlled_file.*
            FROM dcc_controlled_file controlled_file
            LEFT JOIN (
                SELECT recognition_record.controlled_file_id,
                       recognition_record.matched_project_code_id
                FROM dcc_controlled_file_recognition_record recognition_record
                JOIN (
                    SELECT controlled_file_id,
                           MAX(id) AS latest_id
                    FROM dcc_controlled_file_recognition_record
                    WHERE deleted = 0
                      AND status = 'SUCCESS'
                      AND matched_project_code_id IS NOT NULL
                    GROUP BY controlled_file_id
                ) latest
                  ON latest.latest_id = recognition_record.id
            ) latest_recognition
              ON latest_recognition.controlled_file_id = controlled_file.id
            WHERE controlled_file.deleted = 0
              AND COALESCE(controlled_file.dcc_project_code_id, latest_recognition.matched_project_code_id)
                  = #{projectCodeId}
            <if test="fileIds != null and fileIds.size() > 0">
              AND controlled_file.id IN
              <foreach collection="fileIds" item="fileId" open="(" separator="," close=")">
                  #{fileId}
              </foreach>
            </if>
            ORDER BY controlled_file.id
            </script>
            """)
    List<DccControlledFileDO> selectAssociatedFilesByProjectCodeId(@Param("projectCodeId") Long projectCodeId,
                                                                    @Param("fileIds") Collection<Long> fileIds);

    class ProjectCodeFileCount {
        private Long projectCodeId;
        private Long fileCount;

        public Long getProjectCodeId() {
            return projectCodeId;
        }

        public void setProjectCodeId(Long projectCodeId) {
            this.projectCodeId = projectCodeId;
        }

        public Long getFileCount() {
            return fileCount;
        }

        public void setFileCount(Long fileCount) {
            this.fileCount = fileCount;
        }
    }

    class GlobalSourceReference {
        private Long controlledFileId;
        private Long tenantId;
        private Long sourceFileId;

        public Long getControlledFileId() {
            return controlledFileId;
        }

        public void setControlledFileId(Long controlledFileId) {
            this.controlledFileId = controlledFileId;
        }

        public Long getTenantId() {
            return tenantId;
        }

        public void setTenantId(Long tenantId) {
            this.tenantId = tenantId;
        }

        public Long getSourceFileId() {
            return sourceFileId;
        }

        public void setSourceFileId(Long sourceFileId) {
            this.sourceFileId = sourceFileId;
        }
    }

    private LambdaQueryWrapperX<DccControlledFileDO> buildWorkflowQuery(DccControlledFilePageReqVO reqVO) {
        LambdaQueryWrapperX<DccControlledFileDO> query = new LambdaQueryWrapperX<DccControlledFileDO>()
                .eqIfPresent(DccControlledFileDO::getCategoryId, reqVO.getCategoryId())
                .eqIfPresent(DccControlledFileDO::getDirectoryId, reqVO.getDirectoryId())
                .eqIfPresent(DccControlledFileDO::getRequesterId, reqVO.getRequesterId())
                .eqIfPresent(DccControlledFileDO::getStatus, reqVO.getStatus())
                .eqIfPresent(DccControlledFileDO::getProcessType, reqVO.getProcessType());
        applyFileTypeTaxonomyFilter(query, reqVO);
        applyEffectiveProjectCodeFilter(query, reqVO.getDccProjectCodeId());
        String keyword = normalizeKeyword(reqVO.getKeyword());
        if (keyword != null) {
            query.and(wrapper -> wrapper.like(DccControlledFileDO::getTitle, keyword)
                    .or()
                    .like(DccControlledFileDO::getFileName, keyword)
                    .or()
                    .like(DccControlledFileDO::getFileNumber, keyword));
        }
        QuickFilterUtils.filter(query, reqVO.getQuickFilter(), Map.of(
                "keyword", QuickFilterUtils.QuickFilterField.text(DccControlledFileDO::getFileName),
                "fileName", QuickFilterUtils.QuickFilterField.text(DccControlledFileDO::getFileName),
                "fileNumber", QuickFilterUtils.QuickFilterField.text(DccControlledFileDO::getFileNumber),
                "status", QuickFilterUtils.QuickFilterField.select(DccControlledFileDO::getStatus),
                "categoryId", QuickFilterUtils.QuickFilterField.longSelect(DccControlledFileDO::getCategoryId)
        ));
        return query
                .orderByDesc(DccControlledFileDO::getCreateTime)
                .orderByDesc(DccControlledFileDO::getId);
    }

    private void applyEffectiveProjectCodeFilter(LambdaQueryWrapperX<DccControlledFileDO> query, Long dccProjectCodeId) {
        if (dccProjectCodeId == null) {
            return;
        }
        String projectCodeIdLiteral = String.valueOf(dccProjectCodeId);
        query.and(wrapper -> wrapper
                .eq(DccControlledFileDO::getDccProjectCodeId, dccProjectCodeId)
                .or()
                .inSql(DccControlledFileDO::getId, """
                        SELECT recognition_record.controlled_file_id
                        FROM dcc_controlled_file_recognition_record recognition_record
                        JOIN (
                            SELECT controlled_file_id,
                                   MAX(id) AS latest_id
                            FROM dcc_controlled_file_recognition_record
                            WHERE deleted = 0
                              AND status = 'SUCCESS'
                              AND matched_project_code_id IS NOT NULL
                            GROUP BY controlled_file_id
                        ) latest
                          ON latest.latest_id = recognition_record.id
                        WHERE recognition_record.deleted = 0
                          AND recognition_record.status = 'SUCCESS'
                          AND recognition_record.matched_project_code_id = %s
                        """.formatted(projectCodeIdLiteral)));
    }

    private void applyFileTypeTaxonomyFilter(LambdaQueryWrapperX<DccControlledFileDO> query,
                                             DccControlledFilePageReqVO reqVO) {
        boolean hasTaxonomyId = reqVO.getFileTypeTaxonomyId() != null;
        boolean hasTaxonomyIds = hasItems(reqVO.getFileTypeTaxonomyIds());
        boolean hasTaxonomyPaths = hasItems(reqVO.getFileTypeTaxonomyPaths());
        if (!hasTaxonomyId && !hasTaxonomyIds && !hasTaxonomyPaths) {
            return;
        }
        query.and(wrapper -> {
            boolean appended = false;
            if (hasTaxonomyId) {
                wrapper.eq(DccControlledFileDO::getFileTypeTaxonomyId, reqVO.getFileTypeTaxonomyId());
                appended = true;
            }
            if (hasTaxonomyIds) {
                if (appended) {
                    wrapper.or(orWrapper -> orWrapper.in(DccControlledFileDO::getFileTypeTaxonomyId,
                            reqVO.getFileTypeTaxonomyIds()));
                } else {
                    wrapper.in(DccControlledFileDO::getFileTypeTaxonomyId, reqVO.getFileTypeTaxonomyIds());
                }
                appended = true;
            }
            if (hasTaxonomyPaths) {
                if (appended) {
                    wrapper.or(pathWrapper -> applyFileTypeTaxonomyPathFilters(pathWrapper,
                            reqVO.getFileTypeTaxonomyPaths()));
                } else {
                    wrapper.and(pathWrapper -> applyFileTypeTaxonomyPathFilters(pathWrapper,
                            reqVO.getFileTypeTaxonomyPaths()));
                }
            }
        });
    }

    private void applyFileTypeTaxonomyPathFilters(LambdaQueryWrapper<DccControlledFileDO> query,
                                                  List<DccControlledFilePageReqVO.FileTypeTaxonomyPathFilter> paths) {
        for (int index = 0; index < paths.size(); index++) {
            DccControlledFilePageReqVO.FileTypeTaxonomyPathFilter path = paths.get(index);
            if (index == 0) {
                applySingleFileTypeTaxonomyPathFilter(query, path);
            } else {
                query.or(wrapper -> applySingleFileTypeTaxonomyPathFilter(wrapper, path));
            }
        }
    }

    private void applySingleFileTypeTaxonomyPathFilter(LambdaQueryWrapper<DccControlledFileDO> query,
                                                       DccControlledFilePageReqVO.FileTypeTaxonomyPathFilter path) {
        query.isNull(DccControlledFileDO::getFileTypeTaxonomyId);
        applyPathLevel(query, DccControlledFileDO::getFileTypeLevel1, path.getLevel1());
        applyPathLevel(query, DccControlledFileDO::getFileTypeLevel2, path.getLevel2());
        applyPathLevel(query, DccControlledFileDO::getFileTypeLevel3, path.getLevel3());
        applyPathLevel(query, DccControlledFileDO::getFileTypeLevel4, path.getLevel4());
        applyPathLevel(query, DccControlledFileDO::getFileTypeLevel5, path.getLevel5());
    }

    private void applyPathLevel(LambdaQueryWrapper<DccControlledFileDO> query,
                                SFunction<DccControlledFileDO, ?> column,
                                String value) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized == null) {
            query.and(wrapper -> wrapper.isNull(column).or().eq(column, ""));
            return;
        }
        query.eq(column, normalized);
    }

    private boolean hasItems(Collection<?> values) {
        return values != null && !values.isEmpty();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private LambdaQueryWrapperX<DccControlledFileDO> buildBrowserSummaryQuery(DccControlledFilePageReqVO reqVO) {
        LambdaQueryWrapperX<DccControlledFileDO> query = buildWorkflowQuery(reqVO);
        if (Boolean.TRUE.equals(reqVO.getLatestVersionOnly())
                && (reqVO.getStatus() == null || reqVO.getStatus().isBlank())) {
            query.eq(DccControlledFileDO::getStatus, DccControlledFileStatusEnum.ACTIVE.getStatus());
        }
        return query;
    }
}
