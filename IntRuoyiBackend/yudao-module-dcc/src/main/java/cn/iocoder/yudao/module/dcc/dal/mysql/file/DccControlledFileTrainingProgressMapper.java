package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingProgressDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DccControlledFileTrainingProgressMapper extends BaseMapperX<DccControlledFileTrainingProgressDO> {

    @Select("""
        <script>
        SELECT p.* FROM dcc_controlled_file_training_progress p
        INNER JOIN dcc_controlled_file f ON f.id=p.controlled_file_id
        ${ew.customSqlSegment}
        </script>
        """)
    com.baomidou.mybatisplus.core.metadata.IPage<DccControlledFileTrainingProgressDO> selectTaskPage(
        com.baomidou.mybatisplus.core.metadata.IPage<DccControlledFileTrainingProgressDO> page,
        @Param("ew") com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DccControlledFileTrainingProgressDO> wrapper);

    default cn.iocoder.yudao.framework.common.pojo.PageResult<DccControlledFileTrainingProgressDO> selectTaskPage(
            Long userId, cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingTaskPageReqVO request) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DccControlledFileTrainingProgressDO>();
        wrapper.eq("p.user_id",userId).eq("p.deleted",false).eq("f.deleted",false)
            .eq("f.tenant_id",cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())
            .eq(request.getCategoryId()!=null,"f.category_id",request.getCategoryId())
            .eq(request.getStatus()!=null && !request.getStatus().isBlank(),taskStatus(),request.getStatus());
        String departments="COALESCE((SELECT GROUP_CONCAT(COALESCE(d.name,CONCAT('部门#',tr.department_id)) ORDER BY tr.id SEPARATOR '、')"
            +" FROM dcc_controlled_file_training tr LEFT JOIN system_dept d ON d.id=tr.department_id AND d.deleted=0 AND d.status=0"
            +" WHERE tr.controlled_file_id=p.controlled_file_id AND tr.deleted=0"
            +" AND EXISTS(SELECT 1 FROM dcc_controlled_file_training_assignment a WHERE a.training_id=tr.id AND a.user_id=p.user_id AND a.deleted=0)"
            +" AND tr.id=(SELECT MIN(t2.id) FROM dcc_controlled_file_training t2 WHERE t2.controlled_file_id=tr.controlled_file_id AND t2.department_id=tr.department_id AND t2.deleted=0"
            +" AND EXISTS(SELECT 1 FROM dcc_controlled_file_training_assignment a2 WHERE a2.training_id=t2.id AND a2.user_id=p.user_id AND a2.deleted=0))),'-')";
        cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.apply(request,wrapper,java.util.Map.of(
            "title",cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text("f.title"),
            "fileNumber",cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text("f.file_number"),
            "versionNo",cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text("f.version_no"),
            "departmentNames",cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text(departments),
            "trainingSummary",cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text(taskSummary()),
            "categoryId",cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.number("f.category_id"),
            "status",cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text(taskStatus())));
        wrapper.orderByDesc("f.published_time","p.id");
        var page=cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils.<DccControlledFileTrainingProgressDO>buildPage(request);
        page.setOptimizeCountSql(false);
        selectTaskPage(page,wrapper);
        return new cn.iocoder.yudao.framework.common.pojo.PageResult<>(page.getRecords(),page.getTotal());
    }

    static String requiredSeconds() { return "CASE WHEN COALESCE(p.required_view_seconds,0)>0 THEN p.required_view_seconds ELSE 600 END"; }
    static String accumulatedSeconds() { return "GREATEST(0,COALESCE(p.accumulated_view_seconds,0))"; }
    static String taskStatus() { return "CASE WHEN p.acknowledged_at IS NOT NULL THEN 'ACKNOWLEDGED' WHEN "+accumulatedSeconds()+">=("+requiredSeconds()+") THEN 'READY_TO_ACKNOWLEDGE' ELSE 'PENDING_VIEW' END"; }
    static String secondsText(String seconds) { return "CONCAT(FLOOR(("+seconds+")/60),'分',MOD(("+seconds+"),60),'秒')"; }
    static String taskSummary() {
        String required=requiredSeconds(),accumulated=accumulatedSeconds();
        return "CONCAT(CASE WHEN p.acknowledged_at IS NOT NULL THEN '已完成' WHEN "+accumulated+">=("+required+") THEN '已达标待确认' ELSE '待查看' END,' ',"
            +secondsText(accumulated)+",' / ',"+secondsText(required)+",' ',CASE WHEN p.acknowledged_at IS NOT NULL THEN '已确认完成' WHEN "+accumulated+">=("+required+") THEN '已达标，进入培训后可确认' ELSE CONCAT('还需 ',"
            +secondsText("GREATEST(0,("+required+")-"+accumulated+")")+") END,' ',CASE WHEN p.acknowledged_at IS NOT NULL THEN CONCAT('确认：',DATE_FORMAT(p.acknowledged_at,'%Y-%m-%d %H:%i:%s')) ELSE CONCAT('发布：',COALESCE(DATE_FORMAT(f.published_time,'%Y-%m-%d %H:%i:%s'),'-')) END)";
    }

    @Select("SELECT * FROM dcc_controlled_file_training_progress WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    DccControlledFileTrainingProgressDO selectByIdForUpdate(@Param("id") Long id);

    default DccControlledFileTrainingProgressDO selectByControlledFileIdAndUserId(Long controlledFileId, Long userId) {
        return selectOne(DccControlledFileTrainingProgressDO::getControlledFileId, controlledFileId,
                DccControlledFileTrainingProgressDO::getUserId, userId);
    }

    default List<DccControlledFileTrainingProgressDO> selectListByControlledFileId(Long controlledFileId) {
        return selectList(DccControlledFileTrainingProgressDO::getControlledFileId, controlledFileId);
    }

    default List<DccControlledFileTrainingProgressDO> selectListByUserId(Long userId) {
        return selectList(DccControlledFileTrainingProgressDO::getUserId, userId);
    }
}
