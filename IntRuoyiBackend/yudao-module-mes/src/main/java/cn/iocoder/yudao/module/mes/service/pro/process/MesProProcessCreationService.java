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
    public Long create(MesProProcessDO process) {
        TenantContextHolder.getRequiredTenantId();
        if (process.getId() != null) {
            throw new IllegalArgumentException("新增工序不能指定已有工序 ID");
        }
        String code = autoCodeRecordService.generateAutoCode(
                MesMdAutoCodeRuleCodeEnum.PRO_PROCESS_GX_CODE.getCode(), null);
        if (code == null || !code.matches("GX-[0-9]{4,}") || code.matches("GX-0+")) {
            throw new IllegalStateException("工序编码规则必须生成 GX-0001 起的递增编号");
        }
        process.setCode(code);
        processMapper.insert(process);
        return process.getId();
    }
}
