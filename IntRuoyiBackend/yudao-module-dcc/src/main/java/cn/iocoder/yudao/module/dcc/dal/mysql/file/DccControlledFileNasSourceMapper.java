package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasSourceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface DccControlledFileNasSourceMapper extends BaseMapperX<DccControlledFileNasSourceDO> {

    default DccControlledFileNasSourceDO selectByControlledFileIdAndSourceType(Long controlledFileId,
                                                                              String sourceType) {
        return selectOne(new LambdaQueryWrapperX<DccControlledFileNasSourceDO>()
                .eq(DccControlledFileNasSourceDO::getControlledFileId, controlledFileId)
                .eq(DccControlledFileNasSourceDO::getSourceType, sourceType)
                .last("LIMIT 1"));
    }

    @Select("""
            SELECT source.controlled_file_id AS controlledFileId,
                   file.file_name AS fileName,
                   file.version_no AS versionNo,
                   source.nas_share_name AS nasShareName,
                   source.normalized_relative_path AS normalizedRelativePath,
                   source.path_hash AS pathHash,
                   source.source_type AS sourceType,
                   source.source_confidence AS sourceConfidence
            FROM dcc_controlled_file_nas_source source
            JOIN dcc_controlled_file file
              ON file.id = source.controlled_file_id
             AND file.deleted = b'0'
             AND file.tenant_id = #{tenantId}
             AND file.status = 'ACTIVE'
            JOIN dcc_controlled_file_master master
              ON master.id = file.master_id
             AND master.deleted = b'0'
             AND master.tenant_id = #{tenantId}
             AND master.current_active_controlled_file_id = file.id
            WHERE source.deleted = b'0'
              AND source.tenant_id = #{tenantId}
              AND source.nas_share_name = #{nasShareName}
            ORDER BY source.path_hash, source.controlled_file_id
            """)
    List<ActiveNasSourceRow> selectCurrentActiveSources(@Param("tenantId") Long tenantId,
                                                        @Param("nasShareName") String nasShareName);

    @Select("""
            SELECT file.id AS controlledFileId,
                   file.file_name AS fileName,
                   file.version_no AS versionNo,
                   file.remark AS remark
            FROM dcc_controlled_file file
            JOIN dcc_controlled_file_master master
              ON master.id = file.master_id
             AND master.deleted = b'0'
             AND master.tenant_id = #{tenantId}
             AND master.current_active_controlled_file_id = file.id
            LEFT JOIN dcc_controlled_file_nas_source source
              ON source.controlled_file_id = file.id
             AND source.deleted = b'0'
             AND source.tenant_id = #{tenantId}
             AND source.source_type IN ('NAS_TRANSFER', 'LEGACY_NAS_TRANSFER')
            WHERE file.deleted = b'0'
              AND file.tenant_id = #{tenantId}
              AND file.status = 'ACTIVE'
              AND file.remark LIKE 'NAS transfer source: %'
              AND source.id IS NULL
            ORDER BY file.id
            """)
    List<LegacyNasTransferSourceCandidate> selectLegacyNasTransferCandidates(@Param("tenantId") Long tenantId);

    default List<DccControlledFileNasSourceDO> selectListByControlledFileIds(Collection<Long> controlledFileIds) {
        return selectList(new LambdaQueryWrapperX<DccControlledFileNasSourceDO>()
                .inIfPresent(DccControlledFileNasSourceDO::getControlledFileId, controlledFileIds)
                .orderByAsc(DccControlledFileNasSourceDO::getControlledFileId));
    }

    class ActiveNasSourceRow {
        private Long controlledFileId;
        private String fileName;
        private String versionNo;
        private String nasShareName;
        private String normalizedRelativePath;
        private String pathHash;
        private String sourceType;
        private String sourceConfidence;

        public Long getControlledFileId() {
            return controlledFileId;
        }

        public void setControlledFileId(Long controlledFileId) {
            this.controlledFileId = controlledFileId;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getVersionNo() {
            return versionNo;
        }

        public void setVersionNo(String versionNo) {
            this.versionNo = versionNo;
        }

        public String getNasShareName() {
            return nasShareName;
        }

        public void setNasShareName(String nasShareName) {
            this.nasShareName = nasShareName;
        }

        public String getNormalizedRelativePath() {
            return normalizedRelativePath;
        }

        public void setNormalizedRelativePath(String normalizedRelativePath) {
            this.normalizedRelativePath = normalizedRelativePath;
        }

        public String getPathHash() {
            return pathHash;
        }

        public void setPathHash(String pathHash) {
            this.pathHash = pathHash;
        }

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public String getSourceConfidence() {
            return sourceConfidence;
        }

        public void setSourceConfidence(String sourceConfidence) {
            this.sourceConfidence = sourceConfidence;
        }
    }

    class LegacyNasTransferSourceCandidate {
        private Long controlledFileId;
        private String fileName;
        private String versionNo;
        private String remark;

        public Long getControlledFileId() {
            return controlledFileId;
        }

        public void setControlledFileId(Long controlledFileId) {
            this.controlledFileId = controlledFileId;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getVersionNo() {
            return versionNo;
        }

        public void setVersionNo(String versionNo) {
            this.versionNo = versionNo;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}
