package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.CatalogItem;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Category;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Condition;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.EvidenceItem;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Test-only fixture. It is never registered as a production adapter. */
final class MesProEdhrReverseTraceTestFixtureAdapter implements MesProEdhrReverseTraceSourceAdapter {

    private final Category category;
    private final boolean qualifiedCatalog;

    MesProEdhrReverseTraceTestFixtureAdapter() {
        this(Category.FIELD);
    }

    MesProEdhrReverseTraceTestFixtureAdapter(Category category) {
        this(category, false);
    }

    MesProEdhrReverseTraceTestFixtureAdapter(Category category, boolean qualifiedCatalog) {
        this.category = category;
        this.qualifiedCatalog = qualifiedCatalog;
    }

    @Override
    public Category category() {
        return category;
    }

    @Override
    public boolean supports(String evidenceKey) {
        return fixtureKey().equals(evidenceKey);
    }

    @Override
    public CatalogResult readCatalog(MesProEdhrBatchExecutionDO batch) {
        CatalogItem first = item(Map.of("sampleId", "A"));
        CatalogItem second = item(Map.of("sampleId", "B"));
        List<CatalogItem> items = qualifiedCatalog ? List.of(first, second) : List.of(item(Map.of()));
        return new CatalogResult(sourceVersion(batch), "fixture-source-" + batch.getId(), "AVAILABLE", null, null, items);
    }

    @Override
    public EvaluationResult evaluate(MesProEdhrBatchExecutionDO batch, List<Condition> conditions) {
        return new EvaluationResult(sourceVersion(batch), "fixture-source-" + batch.getId(), "COMPLETE", null, null,
                conditions.stream().map(condition -> new ConditionMatch(condition.getConditionId(), true,
                        "fixture", "fixture-ref-" + condition.getConditionId())).toList());
    }

    @Override
    public EvidenceResult readEvidence(MesProEdhrBatchExecutionDO batch, List<Condition> conditions) {
        return new EvidenceResult(sourceVersion(batch), "fixture-source-" + batch.getId(), String.valueOf(batch.getId()),
                conditions.stream().map(condition -> new EvidenceItem().setConditionId(condition.getConditionId())
                        .setTargetBatchExecutionId(String.valueOf(batch.getId()))
                        .setSourceIdentity("fixture-source-" + batch.getId())
                        .setSourceStage("FLOW-04").setSourceAction("fixture")
                        .setSourceRef("fixture-ref-" + condition.getConditionId()).setRecordStatus("RECORDED")
                        .setActualValue("fixture")).toList());
    }

    @Override
    public ValidationResult validateCondition(CatalogItem item, Condition condition) {
        if (item == null || !item.getAllowedOperators().contains(condition.getOperator())
                || !"RECORDED".equals(condition.getSourceView()) || !qualifiersMatch(item, condition)) {
            return new ValidationResult(false, "CONDITION_INVALID", "fixture condition contract mismatch");
        }
        return new ValidationResult(true, null, null);
    }

    private CatalogItem item(Map<String, String> qualifiers) {
        return new CatalogItem().setEvidenceKey(fixtureKey()).setCategory(category)
                .setSourceView("RECORDED").setValueType("string").setUnit("text")
                .setAllowedOperators(List.of("EQ")).setQualifiers(formalQualifiers(qualifiers));
    }

    private Map<String, String> formalQualifiers(Map<String, String> qualifiers) {
        if (category != Category.PARAMETER) return qualifiers;
        Map<String, String> result = new LinkedHashMap<>(Map.of(
                "routeVersionId", "3001",
                "routeProcessId", "3002",
                "processId", "3003",
                "unit", "C"));
        result.putAll(qualifiers);
        return result;
    }

    private boolean qualifiersMatch(CatalogItem item, Condition condition) {
        return condition.getQualifiers() == null || condition.getQualifiers().isEmpty()
                || item.getQualifiers() != null && condition.getQualifiers().entrySet().stream()
                .allMatch(entry -> entry.getValue().equals(item.getQualifiers().get(entry.getKey())));
    }

    private String sourceVersion(MesProEdhrBatchExecutionDO batch) {
        return "fixture-v1-" + batch.getId();
    }

    private String fixtureKey() {
        return category == Category.FIELD ? "fixture-field" : "fixture-" + category.name().toLowerCase();
    }
}
