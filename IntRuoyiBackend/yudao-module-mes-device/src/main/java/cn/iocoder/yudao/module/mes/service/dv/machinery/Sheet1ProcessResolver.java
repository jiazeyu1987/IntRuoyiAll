package cn.iocoder.yudao.module.mes.service.dv.machinery;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;

/** 工序编码是导入关联依据；名称只用于核对，禁止按名称猜测关联。 */
@Component
public class Sheet1ProcessResolver {
    @Resource
    private MesProProcessMapper processMapper;

    public Map<String, MesProProcessDO> resolve(Sheet1MachineryProcessExcelParser.ParsedSheet sheet) {
        var codes = new LinkedHashSet<String>();
        sheet.deviceRows().forEach(row -> codes.add(row.processCode()));
        sheet.manualRows().forEach(row -> codes.add(row.processCode()));
        Map<String, MesProProcessDO> byCode = new LinkedHashMap<>();
        for (MesProProcessDO process : processMapper.selectListByCodes(codes)) {
            if (byCode.putIfAbsent(process.getCode(), process) != null) {
                throw ServiceExceptionUtil.invalidParamException("工序编码 [{}] 对应多条工序，请先修正工序资料后再导入", process.getCode());
            }
        }
        sheet.deviceRows().forEach(row -> validate(byCode, row.processCode(), row.processName(), row.sourceRowNo()));
        sheet.manualRows().forEach(row -> validate(byCode, row.processCode(), row.processName(), row.sourceRowNo()));
        return byCode;
    }

    private void validate(Map<String, MesProProcessDO> byCode, String code, String name, int row) {
        MesProProcessDO process = byCode.get(code);
        if (process == null) {
            throw ServiceExceptionUtil.invalidParamException("Excel 第 {} 行工序编码 [{}] 不存在，请核对工序唯一编码", row, code);
        }
        if (!Objects.equals(process.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
            throw ServiceExceptionUtil.invalidParamException("Excel 第 {} 行工序编码 [{}] 未启用，请先启用对应工序", row, code);
        }
        if (!Objects.equals(process.getName(), name)) {
            throw ServiceExceptionUtil.invalidParamException("Excel 第 {} 行工序名称与编码不匹配：编码 [{}] 的名称为 [{}]，文件填写的是 [{}]", row, code, process.getName(), name);
        }
    }
}
