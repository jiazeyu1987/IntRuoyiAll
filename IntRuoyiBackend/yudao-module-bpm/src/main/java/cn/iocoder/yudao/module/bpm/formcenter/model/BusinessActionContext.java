package cn.iocoder.yudao.module.bpm.formcenter.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BusinessActionContext {

    private final Long tenantId;
    private final String dataDomain;
    private final String systemCode;
    private final String objectType;
    private final String objectId;
    private final String objectVersion;
    private final String actionCode;
    private final String objectState;
    private final String orgCode;
    private final String deptCode;
    private final List<String> roleCodes;
    private final String productCode;
    private final String categoryCode;
    private final String reason;

    private BusinessActionContext(Builder builder) {
        this.tenantId = builder.tenantId;
        this.dataDomain = builder.dataDomain;
        this.systemCode = builder.systemCode;
        this.objectType = builder.objectType;
        this.objectId = builder.objectId;
        this.objectVersion = builder.objectVersion;
        this.actionCode = builder.actionCode;
        this.objectState = builder.objectState;
        this.orgCode = builder.orgCode;
        this.deptCode = builder.deptCode;
        this.roleCodes = Collections.unmodifiableList(new ArrayList<>(builder.roleCodes));
        this.productCode = builder.productCode;
        this.categoryCode = builder.categoryCode;
        this.reason = builder.reason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getDataDomain() {
        return dataDomain;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public String getObjectType() {
        return objectType;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getObjectVersion() {
        return objectVersion;
    }

    public String getActionCode() {
        return actionCode;
    }

    public String getObjectState() {
        return objectState;
    }

    public String getOrgCode() {
        return orgCode;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getReason() {
        return reason;
    }

    public static final class Builder {

        private Long tenantId;
        private String dataDomain;
        private String systemCode;
        private String objectType;
        private String objectId;
        private String objectVersion;
        private String actionCode;
        private String objectState;
        private String orgCode;
        private String deptCode;
        private List<String> roleCodes = List.of();
        private String productCode;
        private String categoryCode;
        private String reason;

        public Builder tenantId(Long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder dataDomain(String dataDomain) {
            this.dataDomain = dataDomain;
            return this;
        }

        public Builder systemCode(String systemCode) {
            this.systemCode = systemCode;
            return this;
        }

        public Builder objectType(String objectType) {
            this.objectType = objectType;
            return this;
        }

        public Builder objectId(String objectId) {
            this.objectId = objectId;
            return this;
        }

        public Builder objectVersion(String objectVersion) {
            this.objectVersion = objectVersion;
            return this;
        }

        public Builder actionCode(String actionCode) {
            this.actionCode = actionCode;
            return this;
        }

        public Builder objectState(String objectState) {
            this.objectState = objectState;
            return this;
        }

        public Builder orgCode(String orgCode) {
            this.orgCode = orgCode;
            return this;
        }

        public Builder deptCode(String deptCode) {
            this.deptCode = deptCode;
            return this;
        }

        public Builder roleCodes(List<String> roleCodes) {
            this.roleCodes = roleCodes == null ? List.of() : roleCodes;
            return this;
        }

        public Builder productCode(String productCode) {
            this.productCode = productCode;
            return this;
        }

        public Builder categoryCode(String categoryCode) {
            this.categoryCode = categoryCode;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public BusinessActionContext build() {
            return new BusinessActionContext(this);
        }

    }

}
