package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Condition;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.EvidenceRequest;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.QueryRequest;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.TargetScope;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrReverseTraceRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void queryAllowsOutOfLimitWithoutOperand() {
        assertTrue(validator.validate(queryRequest(outOfLimitCondition())).isEmpty());
    }

    @Test
    void evidenceAllowsOutOfLimitWithoutOperand() {
        EvidenceRequest request = new EvidenceRequest()
                .setAnchorBatchExecutionId("100")
                .setTargetBatchExecutionId("200")
                .setTargetScope(targetScope())
                .setLogic("AND")
                .setConditions(List.of(outOfLimitCondition()));

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void queryRejectsMissingOperandForOtherOperators() {
        assertFalse(validator.validate(queryRequest(missingOperandCondition("GT"))).isEmpty());
    }

    @Test
    void evidenceRejectsMissingOperandForOtherOperators() {
        EvidenceRequest request = new EvidenceRequest()
                .setAnchorBatchExecutionId("100")
                .setTargetBatchExecutionId("200")
                .setTargetScope(targetScope())
                .setLogic("AND")
                .setConditions(List.of(missingOperandCondition("GT")));

        assertFalse(validator.validate(request).isEmpty());
    }

    private QueryRequest queryRequest(Condition condition) {
        return new QueryRequest()
                .setAnchorBatchExecutionId("100")
                .setTargetScope(targetScope())
                .setLogic("AND")
                .setConditions(List.of(condition));
    }

    private Condition outOfLimitCondition() {
        return new Condition()
                .setConditionId("C1")
                .setEvidenceKey("PARAMETER:TEMP")
                .setSourceView("RECORDED")
                .setOperator("OUT_OF_LIMIT")
                .setValue(null);
    }

    private Condition missingOperandCondition(String operator) {
        return new Condition()
                .setConditionId("C1")
                .setEvidenceKey("PARAMETER:TEMP")
                .setSourceView("RECORDED")
                .setOperator(operator)
                .setValue(null);
    }

    private TargetScope targetScope() {
        return new TargetScope().setKind("RELEASED_HISTORY");
    }
}
