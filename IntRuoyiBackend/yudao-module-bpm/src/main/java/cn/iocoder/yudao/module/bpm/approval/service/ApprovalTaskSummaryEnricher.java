package cn.iocoder.yudao.module.bpm.approval.service;

/** Business modules provide their formal summary while BPM retains process ownership and navigation. */
public interface ApprovalTaskSummaryEnricher {
    void enrich(ApprovalTaskSummary summary, String processDefinitionKey);

    /** SQL facts must receive the same business overrides as the rows rendered after paging. */
    default void enrichFilterFacts(java.util.Map<String, String> facts, FilterContext context) { }

    record FilterContext(String sourceType, String processId, String tenantId,
                         String definitionKey, String businessKey, String currentNodeName,
                         String applicable, java.util.Map<String, String> projections) { }

}
