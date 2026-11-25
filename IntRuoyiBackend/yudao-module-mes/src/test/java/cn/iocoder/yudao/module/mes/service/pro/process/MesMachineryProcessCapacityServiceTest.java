package cn.iocoder.yudao.module.mes.service.pro.process;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.process.MesMachineryProcessCapacitySaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesMachineryProcessCapacityServiceTest {
    @BeforeAll
    static void initializeMyBatisMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "capacity"), MesDvMachineryProcessDO.class);
    }
    @Mock private MesDvMachineryMapper machineryMapper;
    @Mock private MesDvMachineryProcessMapper machineryProcessMapper;
    @Mock private MesProProcessService processService;
    @InjectMocks private MesMachineryProcessCapacityService service;

    private MesMachineryProcessCapacitySaveReqVO request() {
        var request = new MesMachineryProcessCapacitySaveReqVO();
        request.setMachineryId(11L);
        request.setProcessId(101L);
        request.setStandardHourlyCapacity(new BigDecimal("100"));
        request.setRemark("模拟测试");
        return request;
    }

    private void givenValidResources(List<MesDvMachineryProcessDO> rows) {
        when(machineryMapper.selectOne(any(Wrapper.class))).thenReturn(
                MesDvMachineryDO.builder().id(11L).code("M11").name("设备11").build());
        when(processService.getProcess(101L)).thenReturn(
                MesProProcessDO.builder().id(101L).code("P101").name("工序101").build());
        when(machineryProcessMapper.selectList(any(Wrapper.class))).thenReturn(rows);
    }

    @Test
    void createsMissingPairWithServerSideIdentityAndRealCapacity() {
        givenValidResources(List.of());
        when(machineryProcessMapper.insert(any(MesDvMachineryProcessDO.class))).thenReturn(1);
        service.saveCapacity(request());
        var captor = ArgumentCaptor.forClass(MesDvMachineryProcessDO.class);
        verify(machineryProcessMapper).insert(captor.capture());
        var row = captor.getValue();
        assertEquals(11L, row.getMachineryId());
        assertEquals(101L, row.getProcessId());
        assertEquals("P101", row.getProcessCode());
        assertEquals("M11", row.getMachineryCode());
        assertEquals(0, new BigDecimal("1050").compareTo(row.getTenHalfHourDailyCapacity()));
        assertEquals("模拟测试", row.getRemark());
        verify(processService).validateProcessExistsAndEnable(101L);
    }

    @Test
    void updatesAllExactPairSourceRowsAndPreservesTheirQuantitiesAndSource() {
        var first = MesDvMachineryProcessDO.builder().id(1L).machineryId(11L).processId(101L)
                .deviceQuantity(new BigDecimal("2")).sourceRowNo(20).build();
        var second = MesDvMachineryProcessDO.builder().id(2L).machineryId(11L).processId(101L)
                .deviceQuantity(new BigDecimal("3")).sourceRowNo(30).build();
        givenValidResources(List.of(first, second));
        when(machineryProcessMapper.update(any(MesDvMachineryProcessDO.class), any(Wrapper.class))).thenReturn(1);
        service.saveCapacity(request());
        verify(machineryProcessMapper, never()).insert(any(MesDvMachineryProcessDO.class));
        assertEquals(0, new BigDecimal("2100").compareTo(first.getTenHalfHourDailyCapacity()));
        assertEquals(0, new BigDecimal("3150").compareTo(second.getTenHalfHourDailyCapacity()));
        assertEquals(first.getStandardHourlyCapacity(), second.getStandardHourlyCapacity());
        assertEquals(20, first.getSourceRowNo());
        assertEquals(30, second.getSourceRowNo());
        var query = ArgumentCaptor.forClass(Wrapper.class);
        verify(machineryProcessMapper).selectList(query.capture());
        assertTrue(query.getValue().getSqlSegment().contains("machinery_id"));
        assertTrue(query.getValue().getSqlSegment().contains("process_id"));
    }

    @Test
    void missingDeviceFailsBeforeAnyCapacityWrite() {
        assertThrows(ServiceException.class, () -> service.saveCapacity(request()));
        verifyNoInteractions(machineryProcessMapper, processService);
    }

    @Test
    void disabledProcessFailsBeforeAnyCapacityWrite() {
        when(machineryMapper.selectOne(any(Wrapper.class))).thenReturn(MesDvMachineryDO.builder().id(11L).build());
        doThrow(new ServiceException(1, "工序已禁用")).when(processService).validateProcessExistsAndEnable(101L);
        assertThrows(ServiceException.class, () -> service.saveCapacity(request()));
        verifyNoInteractions(machineryProcessMapper);
    }

    @Test
    void unsuccessfulWriteCannotReturnSuccess() {
        givenValidResources(List.of());
        assertThrows(IllegalStateException.class, () -> service.saveCapacity(request()));
    }

    @Test
    void rejectsUnpersistableConvertedDailyCapacity() {
        givenValidResources(List.of(MesDvMachineryProcessDO.builder()
                .id(1L).deviceQuantity(new BigDecimal("999999999999")).build()));
        assertThrows(ServiceException.class, () -> service.saveCapacity(request()));
        verify(machineryProcessMapper, never()).update(any(MesDvMachineryProcessDO.class), any(Wrapper.class));
    }

    @Test
    void validatesPositiveCapacityPrecisionAndMaintenanceReason() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var req = request();
            assertTrue(validator.validate(req).isEmpty());
            for (String invalid : List.of("0", "-1", "1.0000001", "1000000000000")) {
                req.setStandardHourlyCapacity(new BigDecimal(invalid));
                assertFalse(validator.validate(req).isEmpty(), invalid);
            }
            req.setStandardHourlyCapacity(BigDecimal.ONE);
            req.setRemark("  ");
            assertFalse(validator.validate(req).isEmpty());
        }
    }
}
