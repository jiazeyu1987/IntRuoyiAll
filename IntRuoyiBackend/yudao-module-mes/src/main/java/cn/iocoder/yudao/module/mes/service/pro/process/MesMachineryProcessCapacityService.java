package cn.iocoder.yudao.module.mes.service.pro.process;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.process.MesMachineryProcessCapacitySaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.DV_MACHINERY_NOT_EXISTS;

/** 维护排产资源产能；不修改设备主档、工序身份或已生成的排产快照。 */
@Service
@Validated
public class MesMachineryProcessCapacityService {
    @Resource private MesDvMachineryMapper machineryMapper;
    @Resource private MesDvMachineryProcessMapper machineryProcessMapper;
    @Resource private MesProProcessService processService;

    @Transactional(rollbackFor = Exception.class)
    public void saveCapacity(@Valid MesMachineryProcessCapacitySaveReqVO reqVO) {
        // 同一设备的维护串行执行，避免两次首次保存创建重复的设备工序对。
        MesDvMachineryDO machinery = machineryMapper.selectOne(new LambdaQueryWrapperX<MesDvMachineryDO>()
                .eq(MesDvMachineryDO::getId, reqVO.getMachineryId()).last("FOR UPDATE"));
        if (machinery == null) throw exception(DV_MACHINERY_NOT_EXISTS);
        processService.validateProcessExistsAndEnable(reqVO.getProcessId());
        MesProProcessDO process = processService.getProcess(reqVO.getProcessId());
        List<MesDvMachineryProcessDO> rows = machineryProcessMapper.selectList(
                new LambdaQueryWrapperX<MesDvMachineryProcessDO>()
                        .eq(MesDvMachineryProcessDO::getMachineryId, reqVO.getMachineryId())
                        .eq(MesDvMachineryProcessDO::getProcessId, reqVO.getProcessId()));
        if (rows.isEmpty()) {
            MesDvMachineryProcessDO row = MesDvMachineryProcessDO.builder()
                    .machineryId(machinery.getId()).machineryCode(machinery.getCode())
                    .processId(process.getId()).processCode(process.getCode()).processName(process.getName())
                    .deviceName(machinery.getName()).deviceQuantity(BigDecimal.ONE).build();
            fillCapacity(row, reqVO);
            if (machineryProcessMapper.insert(row) != 1) throw new IllegalStateException("设备工序产能新增失败");
        } else {
            // 导入可保留同一设备工序对的多条来源行，统一维护其单台小时产能，保留各行来源和数量。
            for (MesDvMachineryProcessDO row : rows) {
                fillCapacity(row, reqVO);
                MesDvMachineryProcessDO update = MesDvMachineryProcessDO.builder()
                        .standardHourlyCapacity(row.getStandardHourlyCapacity()).remark(row.getRemark()).build();
                if (machineryProcessMapper.update(update, new LambdaUpdateWrapper<MesDvMachineryProcessDO>()
                        .eq(MesDvMachineryProcessDO::getId, row.getId())
                        .set(MesDvMachineryProcessDO::getTenHalfHourDailyCapacity, row.getTenHalfHourDailyCapacity())) != 1) {
                    throw new IllegalStateException("设备工序产能更新失败");
                }
            }
        }
    }

    private void fillCapacity(MesDvMachineryProcessDO row, MesMachineryProcessCapacitySaveReqVO reqVO) {
        row.setStandardHourlyCapacity(reqVO.getStandardHourlyCapacity());
        // 导入字段表示来源行设备总量的10.5小时产能，不是当前工作站的班次小时。
        row.setTenHalfHourDailyCapacity(null);
        if (row.getDeviceQuantity() != null) {
            BigDecimal daily = reqVO.getStandardHourlyCapacity().multiply(row.getDeviceQuantity())
                    .multiply(new BigDecimal("10.5")).setScale(6, RoundingMode.HALF_UP);
            if (daily.precision() > 18) throw invalidParamException("换算后的10.5小时产能超出可保存范围，请核对小时产能及设备数量");
            row.setTenHalfHourDailyCapacity(daily);
        }
        row.setRemark(reqVO.getRemark());
    }
}
