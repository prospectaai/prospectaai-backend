package br.com.prospectaai.shared.billing;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PlanType {
    MONTHLY,
    ANNUAL;

    @JsonCreator
    public static PlanType fromString(String value) {
        if (value == null) return null;
        switch (value.toLowerCase()) {
            case "monthly": return MONTHLY;
            case "annual": return ANNUAL;
            default: return PlanType.valueOf(value);
        }
    }
}