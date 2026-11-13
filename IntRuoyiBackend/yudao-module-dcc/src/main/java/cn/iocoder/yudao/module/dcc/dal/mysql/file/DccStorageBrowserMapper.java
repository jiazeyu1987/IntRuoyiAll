package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DccStorageBrowserMapper {
    /* Version keys implement HistoricalVersion's segment comparator. Numeric segments are
       padded to ten places; trailing canonical zero segments compare equal to missing ones.
       Invalid versions sort last (the existing Comparator.nullsLast/max behavior). */
    String CTE="""
        WITH RECURSIVE directory_paths AS (
          SELECT d.id,d.parent_id,CAST(d.name AS CHAR(10000)) AS path FROM dcc_file_directory d
          WHERE d.tenant_id=#{tenant} AND d.deleted=0 AND d.active=1 AND NOT EXISTS
            (SELECT 1 FROM dcc_file_directory parent WHERE parent.id=d.parent_id AND parent.tenant_id=d.tenant_id AND parent.deleted=0 AND parent.active=1)
          UNION ALL
          SELECT child.id,child.parent_id,CONCAT(parent.path,'/',child.name) FROM dcc_file_directory child
          JOIN directory_paths parent ON parent.id=child.parent_id WHERE child.tenant_id=#{tenant} AND child.deleted=0 AND child.active=1
        ), history_authorized AS (
          SELECT f.* FROM dcc_controlled_file f WHERE (${historyPermission.sqlSegment})
        ), candidates AS (
          SELECT * FROM dcc_controlled_file WHERE tenant_id=#{tenant} AND deleted=0
          <if test='candidate != null and !candidate.emptyOfNormal'>AND (${candidate.expression.normal.sqlSegment})</if>
        ), authorized AS (
          SELECT f.* FROM candidates f WHERE (${permission.sqlSegment})
          <if test='!working'>AND f.controlled_time IS NOT NULL AND f.activated_time IS NOT NULL AND f.status IN ('ACTIVE','SUPERSEDED','OBSOLETE')</if>
          <if test='working'>AND (f.status != 'REJECTED' OR f.id=#{query.workingFileId})</if>
        ), version_parts AS (
          SELECT f.*,UPPER(TRIM(f.version_no)) AS normalized_version,
                 CASE WHEN LOCATE('/',f.version_no)>0 THEN 1 ELSE 0 END AS slash_version,
                 REPLACE(REGEXP_REPLACE(SUBSTRING_INDEX(REGEXP_REPLACE(UPPER(TRIM(f.version_no)),'^V([0-9])','$1'),'-',1),'([./]0)+$',''),'/','.') AS version_core
          FROM authorized f
        ), segments AS (
          SELECT f.id,1 AS ordinal_index,CAST(SUBSTRING_INDEX(f.version_core,'.',1) AS CHAR(255)) AS part,
                 CAST(CASE WHEN LOCATE('.',f.version_core)>0 THEN SUBSTRING(f.version_core,LOCATE('.',f.version_core)+1) ELSE '' END AS CHAR(255)) AS rest
          FROM version_parts f
          UNION ALL
          SELECT id,ordinal_index+1,SUBSTRING_INDEX(rest,'.',1),CASE WHEN LOCATE('.',rest)>0 THEN SUBSTRING(rest,LOCATE('.',rest)+1) ELSE '' END
          FROM segments WHERE rest!=''
        ), version_keys AS (
          SELECT f.*,CASE
            WHEN normalized_version IS NULL OR NOT (normalized_version REGEXP '^[A-Z]+(/[1-9][0-9]*)+(-[1-9][0-9]*)?$' OR normalized_version REGEXP '^V?[0-9]+([.][0-9]+)*$') THEN '2'
            WHEN EXISTS (SELECT 1 FROM segments part WHERE part.id=f.id AND part.part REGEXP '^[0-9]+$' AND CAST(part.part AS DECIMAL(65,0))>2147483647) THEN '2'
            WHEN slash_version=1 AND LOCATE('-',normalized_version)>0 AND CAST(SUBSTRING_INDEX(normalized_version,'-',-1) AS DECIMAL(65,0))>2147483647 THEN '2'
            ELSE CONCAT(slash_version,COALESCE((SELECT GROUP_CONCAT(
              CASE WHEN part.part REGEXP '^([1-9][0-9]*|0)$' THEN CONCAT('0',LPAD(part.part,10,'0'))
                   ELSE CONCAT('1',LPAD(LENGTH(part.part),5,'0'),part.part) END ORDER BY part.ordinal_index SEPARATOR '/')
              FROM segments part WHERE part.id=f.id),''),
              '!',LPAD(CASE WHEN slash_version=1 AND LOCATE('-',normalized_version)>0 THEN SUBSTRING_INDEX(normalized_version,'-',-1) ELSE '0' END,10,'0')) END AS version_sort_key
          FROM version_parts f        ), ranked AS (
          SELECT f.*,ROW_NUMBER() OVER(PARTITION BY COALESCE(f.master_id,f.id) ORDER BY f.version_sort_key COLLATE utf8mb4_bin DESC,f.id DESC) AS version_rank
          FROM version_keys f
        )
        """;
    String JOIN="""
        FROM ranked f
        LEFT JOIN dcc_controlled_file_master m ON m.id=f.master_id AND m.tenant_id=f.tenant_id AND m.deleted=0
        LEFT JOIN dcc_controlled_file current_active ON current_active.id=m.current_active_controlled_file_id AND current_active.master_id=m.id AND current_active.tenant_id=f.tenant_id AND current_active.deleted=0 AND current_active.status='ACTIVE' AND current_active.controlled_time IS NOT NULL AND current_active.activated_time IS NOT NULL
        <if test='historyPermission.paramNameValuePairs.browserBlacklistedExtensions != null'>
          <foreach collection='historyPermission.paramNameValuePairs.browserBlacklistedExtensions' item='blockedExtension'>
            AND NOT (LOCATE('.',LOWER(TRIM(CASE WHEN current_active.source_original_file_name IS NOT NULL THEN current_active.source_original_file_name WHEN NULLIF(TRIM(current_active.file_name),'') IS NOT NULL THEN current_active.file_name ELSE COALESCE(current_active.title,'') END)))>0
              AND CONCAT('.',REGEXP_REPLACE(LOWER(TRIM(CASE WHEN current_active.source_original_file_name IS NOT NULL THEN current_active.source_original_file_name WHEN NULLIF(TRIM(current_active.file_name),'') IS NOT NULL THEN current_active.file_name ELSE COALESCE(current_active.title,'') END)),'^.*[.]',''))=#{blockedExtension})
          </foreach>
        </if>
        LEFT JOIN history_authorized selected_active ON selected_active.id=current_active.id AND selected_active.status='ACTIVE'
        LEFT JOIN dcc_controlled_file sf ON sf.id=COALESCE(#{query.workingFileId},selected_active.id,f.id) AND (sf.master_id=f.master_id OR sf.id=f.id) AND sf.tenant_id=f.tenant_id AND sf.deleted=0
        LEFT JOIN directory_paths path ON path.id=f.directory_id
        LEFT JOIN dcc_file_category category ON category.id=f.category_id AND category.tenant_id=f.tenant_id AND category.deleted=0
        WHERE 1=1
        <choose><when test='latest'>AND f.id=m.current_active_controlled_file_id AND f.status='ACTIVE' AND f.controlled_time IS NOT NULL AND f.activated_time IS NOT NULL</when><otherwise>AND f.version_rank=1</otherwise></choose>
        <if test="query.keyword != null and query.keyword != ''">AND (LOCATE(LOWER(TRIM(#{query.keyword})),LOWER(f.title))>0 OR LOCATE(LOWER(TRIM(#{query.keyword})),LOWER(f.source_original_file_name))>0 OR LOCATE(LOWER(TRIM(#{query.keyword})),LOWER(f.file_name))>0 OR LOCATE(LOWER(TRIM(#{query.keyword})),LOWER(f.file_number))>0)</if>
        """;
    @Select("<script>"+CTE+"SELECT COUNT(*) FROM authorized f WHERE NOT EXISTS (SELECT 1 FROM dcc_controlled_file_master m WHERE m.id=f.master_id AND m.tenant_id=f.tenant_id AND m.deleted=0)</script>")
    long countInvalidLatestMasters(@Param("tenant") Long tenant,@Param("query") DccControlledFilePageReqVO query,
            @Param("candidate") LambdaQueryWrapper<DccControlledFileDO> candidate,@Param("permission") QueryWrapper<Object> permission,
            @Param("historyPermission") QueryWrapper<Object> historyPermission,
            @Param("working") boolean working);
    @SelectProvider(type=DccStorageBrowserSql.class,method="count")
    long count(@Param("tenant") Long tenant,@Param("query") DccControlledFilePageReqVO query,
            @Param("candidate") LambdaQueryWrapper<DccControlledFileDO> candidate,@Param("permission") QueryWrapper<Object> permission,
            @Param("historyPermission") QueryWrapper<Object> historyPermission,
            @Param(Constants.WRAPPER) QueryWrapper<Object> filters,@Param("latest") boolean latest,@Param("working") boolean working);
    @SelectProvider(type=DccStorageBrowserSql.class,method="page")
    List<DccControlledFileDO> page(@Param("tenant") Long tenant,@Param("query") DccControlledFilePageReqVO query,
            @Param("candidate") LambdaQueryWrapper<DccControlledFileDO> candidate,@Param("permission") QueryWrapper<Object> permission,
            @Param("historyPermission") QueryWrapper<Object> historyPermission,
            @Param(Constants.WRAPPER) QueryWrapper<Object> filters,@Param("latest") boolean latest,@Param("working") boolean working,
            @Param("size") int size,@Param("offset") long offset);
}
