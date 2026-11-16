package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import java.util.List;

@Mapper
public interface MesProEdhrBatchVisibilityMapper {
    /** At most one scalar leaves SQL, even if millions of batches are examined by the database. */
    @Select("SELECT EXISTS (SELECT 1 FROM mes_pro_edhr_batch_execution ${ew.customSqlSegment})")
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    boolean exists(@Param("ew") Wrapper<MesProEdhrBatchExecutionDO> query);

    /** r is correlated by taskForRule in the wrapper; each applicable rule is returned once. */
    @Select("SELECT r.* FROM mes_pro_edhr_process_form_permission_rule r WHERE EXISTS "
            + "(SELECT 1 FROM mes_pro_edhr_batch_execution ${ew.customSqlSegment})")
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    List<MesProEdhrProcessFormPermissionRuleDO> selectEffectiveRules(@Param("ew") Wrapper<MesProEdhrBatchExecutionDO> query);
}
