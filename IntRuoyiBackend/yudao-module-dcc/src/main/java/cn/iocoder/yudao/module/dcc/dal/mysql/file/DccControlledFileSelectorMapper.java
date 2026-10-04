package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileSelectorQuery;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileSelectorRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** All three queries use the same candidate identity/filter; count/page are after server authorization. */
@Mapper
public interface DccControlledFileSelectorMapper {
    String VERIFIED_LEGACY_KEYWORD="<![CDATA["+"""
         EXISTS (SELECT 1 FROM dcc_legacy_source_name_evidence e
           JOIN dcc_controlled_file_name_claim c ON c.id=e.legacy_claim_id AND c.tenant_id=e.tenant_id
           WHERE e.tenant_id=f.tenant_id AND e.legacy_master_id=f.master_id AND e.controlled_file_id=f.id
             AND e.released_time IS NULL AND e.source_original_file_name LIKE CONCAT('%',#{query.keyword},'%')
             AND EXISTS(SELECT 1 FROM dcc_legacy_source_name_scope s WHERE s.id=e.verification_scope_id AND
         """ + DccLegacySourceNameSql.VALID_SCOPE + "))]]>";
    String FROM = """
         FROM dcc_controlled_file_master m
         JOIN dcc_controlled_file f ON f.id=m.latest_controlled_file_id
              AND f.master_id=m.id AND f.tenant_id=m.tenant_id AND f.deleted=0
         JOIN dcc_project_code p ON p.id=f.dcc_project_code_id AND p.tenant_id=f.tenant_id AND p.deleted=0
         LEFT JOIN dcc_file_directory d ON d.id=f.directory_id AND d.tenant_id=f.tenant_id AND d.deleted=0
         LEFT JOIN dcc_project_file_placement placement ON placement.tenant_id=f.tenant_id
              AND placement.controlled_file_id=f.id AND placement.project_code_id=f.dcc_project_code_id
              AND placement.storage_directory_id=f.directory_id AND placement.deleted=0
         LEFT JOIN dcc_project_folder folder ON folder.id=placement.project_folder_id
              AND folder.project_code_id=placement.project_code_id AND folder.tenant_id=f.tenant_id
              AND folder.deleted=0 AND folder.active=1
         WHERE m.tenant_id=#{tenant} AND m.deleted=0
         <if test="query.projectId != null">AND f.dcc_project_code_id=#{query.projectId}</if>
         <if test="query.sourceDirectoryId != null">AND f.directory_id=#{query.sourceDirectoryId}</if>
         <if test="query.projectFolderId != null">AND folder.id=#{query.projectFolderId}</if>
         <if test="query.keyword != null and query.keyword != ''">
           AND (f.source_original_file_name LIKE CONCAT('%',#{query.keyword},'%')
                OR f.file_number LIKE CONCAT('%',#{query.keyword},'%') OR p.project_name LIKE CONCAT('%',#{query.keyword},'%')
                OR
         """+VERIFIED_LEGACY_KEYWORD+"""
             )
         </if>
         """;
    String AUTH = """
         AND f.id IN
         <foreach collection="authorizedIds" item="id" open="(" separator="," close=")">#{id}</foreach>
         """;
    String COLUMNS = "SELECT f.*,m.latest_controlled_file_id,p.project_name,d.name AS source_directory_name,folder.id AS project_folder_id,folder.name AS project_folder_name ";

    @Select("<script>" + COLUMNS + FROM + " ORDER BY m.id </script>")
    List<DccControlledFileSelectorRecord> selectCandidates(@Param("tenant") Long tenant,
                                                         @Param("query") DccControlledFileSelectorQuery query);
    @Select("<script>SELECT COUNT(*) " + FROM + AUTH + "</script>")
    long countAuthorized(@Param("tenant") Long tenant, @Param("query") DccControlledFileSelectorQuery query,
                         @Param("authorizedIds") List<Long> authorizedIds);
    @Select("<script>" + COLUMNS + FROM + AUTH + " ORDER BY m.id LIMIT #{size} OFFSET #{offset}</script>")
    List<DccControlledFileSelectorRecord> selectAuthorizedPage(@Param("tenant") Long tenant,
            @Param("query") DccControlledFileSelectorQuery query, @Param("authorizedIds") List<Long> authorizedIds,
            @Param("size") int size, @Param("offset") long offset);
}
