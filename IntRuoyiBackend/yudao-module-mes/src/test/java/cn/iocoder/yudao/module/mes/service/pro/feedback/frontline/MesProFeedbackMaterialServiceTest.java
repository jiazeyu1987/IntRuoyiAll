package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackMaterialDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMaterialMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProFeedbackMaterialServiceTest {

    @Mock
    private MesProFeedbackMaterialMapper materialMapper;

    private MesProFeedbackMaterialService service;

    @BeforeEach
    void setUp() {
        service = new MesProFeedbackMaterialServiceImpl(materialMapper);
    }

    @Test
    void createMaterials_persistsEveryMaterialAsOnePendingErpFact() {
        when(materialMapper.insertBatch(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(true);
        MesProFeedbackMaterialCreateCommand command = command(List.of(
                entry(501L, "A001", "弹簧", "5", "0"),
                entry(502L, "A002", "杠杆", "3", "1")));

        service.createMaterials(command);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<MesProFeedbackMaterialDO>> captor =
                ArgumentCaptor.forClass(java.util.Collection.class);
        verify(materialMapper).insertBatch(captor.capture());
        List<MesProFeedbackMaterialDO> rows = List.copyOf(captor.getValue());
        assertEquals(2, rows.size());
        assertEquals(9001L, rows.get(0).getFeedbackId());
        assertEquals(627L, rows.get(0).getRouteVersionId());
        assertEquals(501L, rows.get(0).getMaterialId());
        assertEquals(BigDecimal.ZERO, rows.get(0).getBomQuantity());
        assertEquals(new BigDecimal("5"), rows.get(0).getOutputQuantity());
        assertEquals("[]", rows.get(0).getLossDetailsJson());
        assertEquals(502L, rows.get(1).getMaterialId());
        assertEquals(new BigDecimal("1"), rows.get(1).getLossQuantity());
    }

    @Test
    void createMaterials_rejectsDuplicateMaterialIdentityBeforeInsert() {
        MesProFeedbackMaterialCreateCommand command = command(List.of(
                entry(501L, "A001", "弹簧", "5", "0"),
                entry(501L, "A001", "弹簧", "3", "0")));

        ServiceException error = assertThrows(ServiceException.class, () -> service.createMaterials(command));

        assertTrue(error.getMessage().contains("物料重复"));
        verify(materialMapper, never()).insertBatch(org.mockito.ArgumentMatchers.anyCollection());
    }

    @Test
    void createMaterials_rejectsLossGreaterThanCompletionBeforeInsert() {
        MesProFeedbackMaterialCreateCommand command = command(List.of(
                entry(501L, "A001", "弹簧", "2", "3")));

        ServiceException error = assertThrows(ServiceException.class, () -> service.createMaterials(command));

        assertTrue(error.getMessage().contains("损耗数量不能大于完成数量"));
        verify(materialMapper, never()).insertBatch(org.mockito.ArgumentMatchers.anyCollection());
    }

    private static MesProFeedbackMaterialCreateCommand command(
            List<MesProFeedbackMaterialCreateCommand.Entry> entries) {
        return new MesProFeedbackMaterialCreateCommand(9001L, 8101L, 4101L, 101L, 627L,
                1001L, 201L, entries);
    }

    private static MesProFeedbackMaterialCreateCommand.Entry entry(
            Long materialId, String code, String name, String output, String loss) {
        return new MesProFeedbackMaterialCreateCommand.Entry(materialId, code, name, null, null,
                new BigDecimal(output), new BigDecimal(loss), "[]", null, "[]");
    }
}
