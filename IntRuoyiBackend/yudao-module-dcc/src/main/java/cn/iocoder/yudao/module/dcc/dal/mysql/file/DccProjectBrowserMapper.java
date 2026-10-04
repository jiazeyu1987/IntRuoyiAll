package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileSelectorRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** Complete version rows, with the same formal placement, tenant and filter identity for count/page. */
@Mapper
public interface DccProjectBrowserMapper {
    String FROM = """
        FROM dcc_controlled_file f
        JOIN dcc_controlled_file_master m ON m.id=f.master_id AND m.tenant_id=f.tenant_id AND m.deleted=0
        JOIN dcc_project_code p ON p.id=f.dcc_project_code_id AND p.tenant_id=f.tenant_id AND p.deleted=0
        LEFT JOIN dcc_project_file_placement placement ON placement.controlled_file_id=f.id
          AND placement.project_code_id=f.dcc_project_code_id AND placement.tenant_id=f.tenant_id
          AND placement.storage_directory_id=f.directory_id AND placement.deleted=0
        LEFT JOIN dcc_project_folder folder ON folder.id=placement.project_folder_id
          AND folder.project_code_id=f.dcc_project_code_id AND folder.tenant_id=f.tenant_id
          AND folder.deleted=0 AND folder.active=1
        WHERE f.tenant_id=#{tenant} AND f.deleted=0
        <if test="query.dccProjectCodeId != null">AND f.dcc_project_code_id=#{query.dccProjectCodeId}</if>
        <if test="query.projectFolderId != null">AND folder.id=#{query.projectFolderId}</if>
        <if test="query.status != null and query.status != ''">AND f.status=#{query.status}</if>
        <if test="query.categoryId != null">AND f.category_id=#{query.categoryId}</if>
        <if test="query.requesterId != null">AND f.requester_id=#{query.requesterId}</if>
        <if test="query.processType != null and query.processType != ''">AND f.process_type=#{query.processType}</if>
        <if test="query.fileTypeTaxonomyId != null">AND f.file_type_taxonomy_id=#{query.fileTypeTaxonomyId}</if>
        <if test="query.latestVersionOnly == true">
          AND f.id=m.latest_controlled_file_id
          AND f.status IN ('ACTIVE','CONTROLLED_PENDING_EFFECTIVE') AND f.controlled_time IS NOT NULL
        </if>
        <if test="query.keyword != null and query.keyword != ''">
          AND (f.source_original_file_name LIKE CONCAT('%',#{query.keyword},'%')
            OR f.file_name LIKE CONCAT('%',#{query.keyword},'%')
            OR f.file_number LIKE CONCAT('%',#{query.keyword},'%')
            OR p.project_name LIKE CONCAT('%',#{query.keyword},'%'))
        </if>
        """;
    String COLUMNS="SELECT f.*,m.latest_controlled_file_id,p.project_name,folder.id AS project_folder_id,folder.name AS project_folder_name ";
    String AUTH=" AND f.id IN <foreach collection=\"authorizedIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">#{id}</foreach> ";
    @Select("<script>"+COLUMNS+FROM+" ORDER BY m.id,f.id </script>")
    List<DccControlledFileSelectorRecord> selectCandidates(@Param("tenant") Long tenant,@Param("query") DccControlledFilePageReqVO query);
    @Select("<script>SELECT COUNT(*) "+FROM+AUTH+"</script>")
    long countAuthorized(@Param("tenant") Long tenant,@Param("query") DccControlledFilePageReqVO query,@Param("authorizedIds") List<Long> authorizedIds);
    @Select("<script>"+COLUMNS+FROM+AUTH+" ORDER BY m.id,f.id LIMIT #{size} OFFSET #{offset}</script>")
    List<DccControlledFileSelectorRecord> selectAuthorizedPage(@Param("tenant") Long tenant,@Param("query") DccControlledFilePageReqVO query,
            @Param("authorizedIds") List<Long> authorizedIds,@Param("size") int size,@Param("offset") long offset);
}
