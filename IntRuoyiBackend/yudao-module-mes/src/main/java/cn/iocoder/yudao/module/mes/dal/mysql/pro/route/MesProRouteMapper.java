package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRoutePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * MES 工艺路线 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesProRouteMapper extends BaseMapperX<MesProRouteDO> {
    java.util.Map<String, ListFilterQueryUtils.Field> LIST_FILTER_FIELDS = java.util.Map.ofEntries(
            java.util.Map.entry("code", ListFilterQueryUtils.text("code")),
            java.util.Map.entry("name", ListFilterQueryUtils.text("name")),
            java.util.Map.entry("description", ListFilterQueryUtils.text("description")),
            java.util.Map.entry("remark", ListFilterQueryUtils.text("remark")),
            java.util.Map.entry("status", ListFilterQueryUtils.number("status")),
            java.util.Map.entry("createTime", ListFilterQueryUtils.dateTime("create_time")),
            java.util.Map.entry("ownerName", ListFilterQueryUtils.text("COALESCE((CASE WHEN LOCATE('[owner]',mes_pro_route.remark)>0 AND LOCATE('[/owner]',mes_pro_route.remark)>LOCATE('[owner]',mes_pro_route.remark) THEN NULLIF(TRIM(SUBSTRING(mes_pro_route.remark,LOCATE('[owner]',mes_pro_route.remark)+7,LOCATE('[/owner]',mes_pro_route.remark)-LOCATE('[owner]',mes_pro_route.remark)-7)),'') END),(SELECT (COALESCE((CASE WHEN LOCATE('[owner]',x.remark)>0 AND LOCATE('[/owner]',x.remark)>LOCATE('[owner]',x.remark) THEN NULLIF(TRIM(SUBSTRING(x.remark,LOCATE('[owner]',x.remark)+7,LOCATE('[/owner]',x.remark)-LOCATE('[owner]',x.remark)-7)),'') END),CASE WHEN LOCATE('owner ',(CASE WHEN LOCATE('[owner]',x.remark)>0 AND LOCATE('[/owner]',x.remark)>LOCATE('[owner]',x.remark) THEN TRIM(CONCAT(SUBSTRING(x.remark,1,LOCATE('[owner]',x.remark)-1),SUBSTRING(x.remark,LOCATE('[/owner]',x.remark)+8))) ELSE TRIM(x.remark) END))>0 THEN NULLIF(TRIM(SUBSTRING_INDEX((CASE WHEN LOCATE('[owner]',x.remark)>0 AND LOCATE('[/owner]',x.remark)>LOCATE('[owner]',x.remark) THEN TRIM(CONCAT(SUBSTRING(x.remark,1,LOCATE('[owner]',x.remark)-1),SUBSTRING(x.remark,LOCATE('[/owner]',x.remark)+8))) ELSE TRIM(x.remark) END),'owner ',-1)),'') WHEN LOCATE('负责人',(CASE WHEN LOCATE('[owner]',x.remark)>0 AND LOCATE('[/owner]',x.remark)>LOCATE('[owner]',x.remark) THEN TRIM(CONCAT(SUBSTRING(x.remark,1,LOCATE('[owner]',x.remark)-1),SUBSTRING(x.remark,LOCATE('[/owner]',x.remark)+8))) ELSE TRIM(x.remark) END))>0 THEN NULLIF(TRIM(SUBSTRING_INDEX((CASE WHEN LOCATE('[owner]',x.remark)>0 AND LOCATE('[/owner]',x.remark)>LOCATE('[owner]',x.remark) THEN TRIM(CONCAT(SUBSTRING(x.remark,1,LOCATE('[owner]',x.remark)-1),SUBSTRING(x.remark,LOCATE('[/owner]',x.remark)+8))) ELSE TRIM(x.remark) END),'负责人',-1)),'') END)) FROM mes_pro_route_product x WHERE x.route_id=mes_pro_route.id AND x.tenant_id=mes_pro_route.tenant_id AND x.deleted=0 AND (COALESCE((CASE WHEN LOCATE('[owner]',x.remark)>0 AND LOCATE('[/owner]',x.remark)>LOCATE('[owner]',x.remark) THEN NULLIF(TRIM(SUBSTRING(x.remark,LOCATE('[owner]',x.remark)+7,LOCATE('[/owner]',x.remark)-LOCATE('[owner]',x.remark)-7)),'') END),CASE WHEN LOCATE('owner ',(CASE WHEN LOCATE('[owner]',x.remark)>0 AND LOCATE('[/owner]',x.remark)>LOCATE('[owner]',x.remark) THEN TRIM(CONCAT(SUBSTRING(x.remark,1,LOCATE('[owner]',x.remark)-1),SUBSTRING(x.remark,LOCATE('[/owner]',x.remark)+8))) ELSE TRIM(x.remark) END))>0 THEN NULLIF(TRIM(SUBSTRING_INDEX((CASE WHEN LOCATE('[owner]',x.remark)>0 AND LOCATE('[/owner]',x.remark)>LOCATE('[owner]',x.remark) THEN TRIM(CONCAT(SUBSTRING(x.remark,1,LOCATE('[owner]',x.remark)-1),SUBSTRING(x.remark,LOCATE('[/owner]',x.remark)+8))) ELSE TRIM(x.remark) END),'owner ',-1)),'') WHEN LOCATE('负责人',(CASE WHEN LOCATE('[owner]',x.remark)>0 AND LOCATE('[/owner]',x.remark)>LOCATE('[owner]',x.remark) THEN TRIM(CONCAT(SUBSTRING(x.remark,1,LOCATE('[owner]',x.remark)-1),SUBSTRING(x.remark,LOCATE('[/owner]',x.remark)+8))) ELSE TRIM(x.remark) END))>0 THEN NULLIF(TRIM(SUBSTRING_INDEX((CASE WHEN LOCATE('[owner]',x.remark)>0 AND LOCATE('[/owner]',x.remark)>LOCATE('[owner]',x.remark) THEN TRIM(CONCAT(SUBSTRING(x.remark,1,LOCATE('[owner]',x.remark)-1),SUBSTRING(x.remark,LOCATE('[/owner]',x.remark)+8))) ELSE TRIM(x.remark) END),'负责人',-1)),'') END)) IS NOT NULL ORDER BY x.id LIMIT 1))")),
            java.util.Map.entry("keyProcessName", ListFilterQueryUtils.text("JSON_UNQUOTE(CONCAT(CHAR(34),REPLACE(SUBSTRING(CAST((SELECT JSON_ARRAYAGG(p.name) OVER(ORDER BY x.sort IS NULL,x.sort,x.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM mes_pro_route_process x JOIN mes_pro_process p ON p.id=x.process_id AND p.tenant_id=x.tenant_id AND p.deleted=0 WHERE x.route_id=mes_pro_route.id AND x.tenant_id=mes_pro_route.tenant_id AND x.deleted=0 AND x.key_flag=1 AND p.name IS NOT NULL LIMIT 1) AS CHAR),3,CHAR_LENGTH(CAST((SELECT JSON_ARRAYAGG(p.name) OVER(ORDER BY x.sort IS NULL,x.sort,x.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM mes_pro_route_process x JOIN mes_pro_process p ON p.id=x.process_id AND p.tenant_id=x.tenant_id AND p.deleted=0 WHERE x.route_id=mes_pro_route.id AND x.tenant_id=mes_pro_route.tenant_id AND x.deleted=0 AND x.key_flag=1 AND p.name IS NOT NULL LIMIT 1) AS CHAR))-4),CONCAT(CHAR(34),', ',CHAR(34)),'、'),CHAR(34)))")),
            java.util.Map.entry("productCount", ListFilterQueryUtils.number("(SELECT COUNT(DISTINCT x.item_id) FROM mes_pro_route_product x WHERE x.route_id=mes_pro_route.id AND x.tenant_id=mes_pro_route.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("dccProjectCode", ListFilterQueryUtils.text("(SELECT p.project_code FROM mes_pro_route_dcc_project_binding x JOIN dcc_project_code p ON p.id=x.dcc_project_code_id AND p.tenant_id=x.tenant_id AND p.deleted=0 WHERE x.route_id=mes_pro_route.id AND x.tenant_id=mes_pro_route.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("flowGraphConfigured", ListFilterQueryUtils.text("CASE WHEN EXISTS(SELECT 1 FROM mes_pro_route_process_flow_edge x WHERE x.route_id=mes_pro_route.id AND x.tenant_id=mes_pro_route.tenant_id AND x.deleted=0) OR EXISTS(SELECT 1 FROM mes_pro_route_process_flow_boundary_edge x WHERE x.route_id=mes_pro_route.id AND x.tenant_id=mes_pro_route.tenant_id AND x.deleted=0) OR EXISTS(SELECT 1 FROM mes_pro_route_process x WHERE x.route_id=mes_pro_route.id AND x.tenant_id=mes_pro_route.tenant_id AND x.deleted=0 AND x.next_process_id IS NOT NULL) THEN '已设' ELSE '未设' END")),
            java.util.Map.entry("activeRouteVersionNo", ListFilterQueryUtils.text("(SELECT x.version_no FROM mes_pro_route_version x WHERE x.route_id=mes_pro_route.id AND x.tenant_id=mes_pro_route.tenant_id AND x.deleted=0 AND x.active=1 ORDER BY x.id DESC LIMIT 1)")),
            java.util.Map.entry("pendingRouteVersionNo", ListFilterQueryUtils.text("COALESCE((SELECT CONCAT(x.version_no,' ',CASE x.lifecycle_status WHEN 'DRAFT' THEN '草稿' WHEN 'PENDING_APPROVAL' THEN '审批中' ELSE '待发布' END,CASE WHEN (SELECT COUNT(*) FROM mes_pro_route_version y WHERE y.route_id=mes_pro_route.id AND y.tenant_id=mes_pro_route.tenant_id AND y.deleted=0 AND COALESCE(y.active,0)=0 AND y.lifecycle_status IN ('READY_TO_PUBLISH','PENDING_APPROVAL','DRAFT'))>1 THEN CONCAT(' +',(SELECT COUNT(*)-1 FROM mes_pro_route_version y WHERE y.route_id=mes_pro_route.id AND y.tenant_id=mes_pro_route.tenant_id AND y.deleted=0 AND COALESCE(y.active,0)=0 AND y.lifecycle_status IN ('READY_TO_PUBLISH','PENDING_APPROVAL','DRAFT'))) ELSE '' END) FROM mes_pro_route_version x WHERE x.route_id=mes_pro_route.id AND x.tenant_id=mes_pro_route.tenant_id AND x.deleted=0 AND COALESCE(x.active,0)=0 AND x.lifecycle_status IN ('READY_TO_PUBLISH','PENDING_APPROVAL','DRAFT') ORDER BY FIELD(x.lifecycle_status,'READY_TO_PUBLISH','PENDING_APPROVAL','DRAFT'),x.id DESC LIMIT 1),'无')"))
    );


    /** Historical terminal records remain readable; unfinished business prevents route deletion. */
    @Select("""
            SELECT EXISTS (
                SELECT 1 FROM mes_pro_process_pool_active_order
                WHERE route_id = #{routeId} AND deleted = 0 AND active_status = 'ACTIVE'
            ) OR EXISTS (
                SELECT 1 FROM mes_pro_schedule_order
                WHERE route_id = #{routeId} AND deleted = 0
                  AND (status IS NULL OR status NOT IN (3, 4))
            ) OR EXISTS (
                SELECT 1 FROM mes_pro_edhr_batch_execution
                WHERE route_id = #{routeId} AND deleted = 0
                  AND (status IS NULL OR status NOT IN (30, 40, 50, 60))
            )
            """)
    boolean existsUnfinishedBusinessByRouteId(@Param("routeId") Long routeId);

    default MesProRouteDO selectByIdForUpdate(Long id) {
        if (id == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProRouteDO>()
                .eq(MesProRouteDO::getId, id)
                .last("FOR UPDATE"));
    }

    default PageResult<MesProRouteDO> selectPage(MesProRoutePageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<MesProRouteDO>()
                .likeIfPresent(MesProRouteDO::getCode, reqVO.getCode())
                .likeIfPresent(MesProRouteDO::getName, reqVO.getName())
                .eqIfPresent(MesProRouteDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesProRouteDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesProRouteDO::getId), LIST_FILTER_FIELDS));
    }

    default MesProRouteDO selectByCode(String code) {
        return selectOne(MesProRouteDO::getCode, code);
    }

    default MesProRouteDO selectByName(String name) {
        return selectOne(new LambdaQueryWrapperX<MesProRouteDO>().apply("TRIM(name) = {0}", name));
    }

    default List<MesProRouteDO> selectListByName(String name) {
        return selectList(new LambdaQueryWrapperX<MesProRouteDO>()
                .eq(MesProRouteDO::getName, name)
                .orderByAsc(MesProRouteDO::getId));
    }

    default List<MesProRouteDO> selectListByCodePrefix(String codePrefix) {
        return selectList(new LambdaQueryWrapperX<MesProRouteDO>()
                .likeRight(MesProRouteDO::getCode, codePrefix));
    }

    default List<MesProRouteDO> selectListByStatus(Integer status) {
        return selectList(MesProRouteDO::getStatus, status);
    }

    @Select({
            "<script>",
            "SELECT id, code, name, description, status, remark, create_time, update_time, creator, updater, deleted, tenant_id ",
            "FROM mes_pro_route ",
            "WHERE id IN ",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<MesProRouteDO> selectListByIdsIgnoreDeleted(@Param("ids") Collection<Long> ids);

    @Select("SELECT id, code, name, description, status, remark, create_time, update_time, creator, updater, deleted, tenant_id " +
            "FROM mes_pro_route WHERE id = #{id} LIMIT 1")
    MesProRouteDO selectByIdIgnoreDeleted(@Param("id") Long id);

}
