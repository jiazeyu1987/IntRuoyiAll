package cn.iocoder.yudao.module.bpm.formcenter.model;

public class FormDuplicateDecision {

    private final FormDuplicateDecisionType type;
    private final FormActionInstance instance;

    private FormDuplicateDecision(FormDuplicateDecisionType type, FormActionInstance instance) {
        this.type = type;
        this.instance = instance;
    }

    public static FormDuplicateDecision created(FormActionInstance instance) {
        return new FormDuplicateDecision(FormDuplicateDecisionType.CREATED, instance);
    }

    public static FormDuplicateDecision existingDraft(FormActionInstance instance) {
        return new FormDuplicateDecision(FormDuplicateDecisionType.RETURN_EXISTING_DRAFT, instance);
    }

    public FormDuplicateDecisionType getType() {
        return type;
    }

    public FormActionInstance getInstance() {
        return instance;
    }

}
