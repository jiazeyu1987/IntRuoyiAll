package cn.iocoder.yudao.module.mes.service.pro.process;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.enums.md.autocode.MesMdAutoCodeRuleCodeEnum;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 所有新建工序（含导入）的统一落库入口；传入编码不作为正式工序编码。 */
@Service
public class MesProProcessCreationService {

    @Resource
    private MesMdAutoCodeRecordService autoCodeRecordService;
    @Resource
    private MesProProcessMapper processMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long create(MesProProcessDO process, Long projectId) {
        TenantContextHolder.getRequiredTenantId();
        if (process.getId() != null) {
            throw new IllegalArgumentException("新增工序不能指定已有工序 ID");
        }
        String code = autoCodeRecordService.generateProjectCode(
                MesMdAutoCodeRuleCodeEnum.PRO_PROCESS_GX_CODE.getCode(), projectId);
        if (code == null || code.isBlank()) {
            throw new IllegalStateException("项目工序编码生成结果为空");
        }
        process.setCode(code);
        processMapper.insert(process);
        return process.getId();
    }
}
