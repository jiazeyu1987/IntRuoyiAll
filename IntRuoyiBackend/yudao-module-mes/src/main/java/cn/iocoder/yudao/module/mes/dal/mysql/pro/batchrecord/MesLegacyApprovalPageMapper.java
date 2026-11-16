package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** Legacy approval API: authorized Flowable identities joined to their persisted execution facts before paging. */
@Mapper
public interface MesLegacyApprovalPageMapper {
    String JOINS=" LEFT JOIN ACT_RE_PROCDEF d ON d.ID_=t.PROC_DEF_ID_ LEFT JOIN mes_pro_batch_record_approval_snapshot s ON s.process_instance_id=t.PROC_INST_ID_ AND s.deleted=0 AND s.tenant_id=CAST(t.TENANT_ID_ AS UNSIGNED) LEFT JOIN mes_pro_batch_record_execution x ON x.id=s.execution_id AND x.deleted=0 AND x.tenant_id=CAST(t.TENANT_ID_ AS UNSIGNED) ";
    @Select("SELECT COUNT(*) FROM ACT_RU_TASK t"+JOINS+"${ew.customSqlSegment}")
    long countTodo(@Param("ew") QueryWrapper<Object> query);
    @Select("SELECT COUNT(*) FROM ACT_HI_TASKINST t"+JOINS+"${ew.customSqlSegment}")
    long countDone(@Param("ew") QueryWrapper<Object> query);
    @Select("SELECT t.ID_ FROM ACT_RU_TASK t"+JOINS+"${ew.customSqlSegment}")
    List<String> todo(@Param("ew") QueryWrapper<Object> query);
    @Select("SELECT t.ID_ FROM ACT_HI_TASKINST t"+JOINS+"${ew.customSqlSegment}")
    List<String> done(@Param("ew") QueryWrapper<Object> query);
}
