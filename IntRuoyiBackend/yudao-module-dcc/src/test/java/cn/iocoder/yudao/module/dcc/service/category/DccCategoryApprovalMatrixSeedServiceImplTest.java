package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryApprovalMatrixSaveReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DccCategoryApprovalMatrixSeedServiceImplTest {
    @Test
    void seedKeepsEveryParticipantAndExactLabel() {
        List<DccCategoryApprovalMatrixSaveReqVO.Rule> rules = ReflectionTestUtils.invokeMethod(
                new DccCategoryApprovalMatrixSeedServiceImpl(), "buildSeedRules", "SIGNOFF",
                List.of(11L, 12L), List.of("QA", "QMS"));
        assertNotNull(rules);
        assertEquals(List.of(11L, 12L), rules.stream().map(DccCategoryApprovalMatrixSaveReqVO.Rule::getSubjectId).toList());
        assertEquals(List.of("QA", "QMS"), rules.stream().map(DccCategoryApprovalMatrixSaveReqVO.Rule::getSubjectLabel).toList());
        assertThrows(IllegalArgumentException.class, () -> ReflectionTestUtils.invokeMethod(
                new DccCategoryApprovalMatrixSeedServiceImpl(), "buildSeedRules", "SIGNOFF", List.of(11L), List.of("QA", "QMS")));
    }
}
