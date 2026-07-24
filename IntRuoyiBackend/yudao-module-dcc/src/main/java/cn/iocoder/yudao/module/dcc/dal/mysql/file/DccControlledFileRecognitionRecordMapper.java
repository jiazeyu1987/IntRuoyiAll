package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRecognitionFailureSummaryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRecognitionRecordDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DccControlledFileRecognitionRecordMapper extends BaseMapperX<DccControlledFileRecognitionRecordDO> {

    @Insert("""
            INSERT INTO dcc_controlled_file_recognition_record (
                tenant_id, controlled_file_id, recognition_scope, recognition_method, recognition_version,
                status, batch_task_id, matched_project_code_id, matched_project_alias_id,
                matched_project_alias_text, matched_project_alias_source, recognized_product_code, recognized_product_name,
                match_type, match_text, failure_stage, failure_code, failure_message,
                file_type_taxonomy_id,
                file_type_level1, file_type_level2, file_type_level3,
                file_type_level4, file_type_level5, recognized_by, recognized_time, source_file_id,
                creator, updater, deleted
            ) VALUES (
                #{tenantId}, #{controlledFileId}, #{recognitionScope}, #{recognitionMethod}, #{recognitionVersion},
                #{status}, #{batchTaskId}, #{matchedProjectCodeId}, #{matchedProjectAliasId},
                #{matchedProjectAliasText}, #{matchedProjectAliasSource}, #{recognizedProductCode}, #{recognizedProductName},
                #{matchType}, #{matchText}, #{failureStage}, #{failureCode}, #{failureMessage},
                #{fileTypeTaxonomyId},
                #{fileTypeLevel1}, #{fileTypeLevel2}, #{fileTypeLevel3},
                #{fileTypeLevel4}, #{fileTypeLevel5}, #{recognizedBy}, #{recognizedTime}, #{sourceFileId},
                '', '', 0
            )
            ON DUPLICATE KEY UPDATE
                status = VALUES(status),
                batch_task_id = VALUES(batch_task_id),
                matched_project_code_id = VALUES(matched_project_code_id),
                matched_project_alias_id = VALUES(matched_project_alias_id),
                matched_project_alias_text = VALUES(matched_project_alias_text),
                matched_project_alias_source = VALUES(matched_project_alias_source),
                recognized_product_code = VALUES(recognized_product_code),
                recognized_product_name = VALUES(recognized_product_name),
                match_type = VALUES(match_type),
                match_text = VALUES(match_text),
                failure_stage = VALUES(failure_stage),
                failure_code = VALUES(failure_code),
                failure_message = VALUES(failure_message),
                file_type_taxonomy_id = VALUES(file_type_taxonomy_id),
                file_type_level1 = VALUES(file_type_level1),
                file_type_level2 = VALUES(file_type_level2),
                file_type_level3 = VALUES(file_type_level3),
                file_type_level4 = VALUES(file_type_level4),
                file_type_level5 = VALUES(file_type_level5),
                recognized_by = VALUES(recognized_by),
                recognized_time = VALUES(recognized_time),
                source_file_id = VALUES(source_file_id),
                updater = '',
                update_time = CURRENT_TIMESTAMP
            """)
    int upsert(DccControlledFileRecognitionRecordDO record);

    @Select("""
            SELECT *
            FROM dcc_controlled_file_recognition_record
            WHERE controlled_file_id = #{controlledFileId}
              AND recognition_scope = #{recognitionScope}
              AND recognition_method = #{recognitionMethod}
              AND recognition_version = #{recognitionVersion}
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
            """)
    DccControlledFileRecognitionRecordDO selectLatestByBizKey(@Param("controlledFileId") Long controlledFileId,
                                                              @Param("recognitionScope") String recognitionScope,
                                                              @Param("recognitionMethod") String recognitionMethod,
                                                              @Param("recognitionVersion") String recognitionVersion);

    @Select("""
            SELECT *
            FROM dcc_controlled_file_recognition_record
            WHERE controlled_file_id = #{controlledFileId}
              AND recognition_scope = #{recognitionScope}
              AND recognition_version = #{recognitionVersion}
              AND deleted = 0
            ORDER BY recognized_time DESC, id DESC
            LIMIT 1
            """)
    DccControlledFileRecognitionRecordDO selectLatestByFileAndVersion(
            @Param("controlledFileId") Long controlledFileId,
            @Param("recognitionScope") String recognitionScope,
            @Param("recognitionVersion") String recognitionVersion);

    @Select("""
            <script>
            SELECT *
            FROM dcc_controlled_file_recognition_record
            WHERE controlled_file_id = #{controlledFileId}
              AND recognition_scope = #{recognitionScope}
              AND recognition_version = #{recognitionVersion}
              AND status = 'SUCCESS'
              AND deleted = 0
              AND recognition_method IN
              <foreach collection="recognitionMethods" item="method" open="(" separator="," close=")">
                #{method}
              </foreach>
            ORDER BY recognized_time DESC, id DESC
            LIMIT 1
            </script>
            """)
    DccControlledFileRecognitionRecordDO selectLatestSuccessfulByFileAndVersion(
            @Param("controlledFileId") Long controlledFileId,
            @Param("recognitionScope") String recognitionScope,
            @Param("recognitionVersion") String recognitionVersion,
            @Param("recognitionMethods") List<String> recognitionMethods);

    @Select("""
            <script>
            SELECT *
            FROM dcc_controlled_file_recognition_record
            WHERE deleted = 0
              AND controlled_file_id IN
              <foreach collection="controlledFileIds" item="controlledFileId" open="(" separator="," close=")">
                #{controlledFileId}
              </foreach>
              <if test="recognitionStatus != null and recognitionStatus != ''">
              AND status = #{recognitionStatus}
              </if>
              <if test="batchTaskId != null">
              AND batch_task_id = #{batchTaskId}
              </if>
            ORDER BY controlled_file_id ASC, recognized_time DESC, id DESC
            </script>
            """)
    List<DccControlledFileRecognitionRecordDO> selectListByFileIds(
            @Param("controlledFileIds") List<Long> controlledFileIds,
            @Param("recognitionStatus") String recognitionStatus,
            @Param("batchTaskId") Long batchTaskId);

    @Select("""
            <script>
            SELECT *
            FROM dcc_controlled_file_recognition_record
            WHERE deleted = 0
              AND batch_task_id = #{batchTaskId}
              <if test="recognitionStatus != null and recognitionStatus != ''">
              AND status = #{recognitionStatus}
              </if>
            ORDER BY controlled_file_id ASC, recognized_time DESC, id DESC
            </script>
            """)
    List<DccControlledFileRecognitionRecordDO> selectListByBatchTaskId(
            @Param("batchTaskId") Long batchTaskId,
            @Param("recognitionStatus") String recognitionStatus);

    @Select("""
            SELECT COALESCE(failure_stage, 'UNCLASSIFIED') AS failureStage,
                   COALESCE(failure_code, 'MISSING_FAILURE_METADATA') AS failureCode,
                   MIN(failure_message) AS failureMessage,
                   COUNT(DISTINCT controlled_file_id) AS failureCount
            FROM dcc_controlled_file_recognition_record
            WHERE deleted = 0
              AND batch_task_id = #{batchTaskId}
              AND status = 'FAILED'
              AND failure_message IS NOT NULL
              AND TRIM(failure_message) <> ''
            GROUP BY COALESCE(failure_stage, 'UNCLASSIFIED'),
                     COALESCE(failure_code, 'MISSING_FAILURE_METADATA')
            ORDER BY failureCount DESC, failureStage ASC, failureCode ASC
            LIMIT #{limit}
            """)
    List<DccControlledFileRecognitionFailureSummaryDO> selectFailureSummariesByBatchTaskId(
            @Param("batchTaskId") Long batchTaskId,
            @Param("limit") Integer limit);

    @Select("""
            <script>
            SELECT COUNT(DISTINCT controlled_file_id)
            FROM dcc_controlled_file_recognition_record
            WHERE deleted = 0
              AND recognition_scope = #{recognitionScope}
              AND recognition_version = #{recognitionVersion}
              AND controlled_file_id IN
              <foreach collection="controlledFileIds" item="controlledFileId" open="(" separator="," close=")">
                #{controlledFileId}
              </foreach>
            </script>
            """)
    Long countRecordedFilesByFileIdsAndVersion(
            @Param("controlledFileIds") List<Long> controlledFileIds,
            @Param("recognitionScope") String recognitionScope,
            @Param("recognitionVersion") String recognitionVersion);
}
