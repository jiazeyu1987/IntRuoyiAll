package cn.iocoder.yudao.module.bpm.formcenter.model;

import java.util.List;

public class FormTemplateImpactCheckResult {

    private final boolean blocked;
    private final List<FormTemplateImpact> impacts;

    private FormTemplateImpactCheckResult(boolean blocked, List<FormTemplateImpact> impacts) {
        this.blocked = blocked;
        this.impacts = List.copyOf(impacts);
    }

    public static FormTemplateImpactCheckResult of(List<FormTemplateImpact> impacts) {
        return new FormTemplateImpactCheckResult(!impacts.isEmpty(), impacts);
    }

    public boolean isBlocked() {
        return blocked;
    }

    public List<FormTemplateImpact> getImpacts() {
        return impacts;
    }

}
