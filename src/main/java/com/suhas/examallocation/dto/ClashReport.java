package com.suhas.examallocation.dto;

import java.util.ArrayList;
import java.util.List;

public class ClashReport {

    private boolean hasClashes;
    private List<String> violations;

    public ClashReport() {
        this.hasClashes = false;
        this.violations = new ArrayList<>();
    }

    public void addViolation(String violation) {
        this.violations.add(violation);
        this.hasClashes = true;
    }

    public boolean isHasClashes() {
        return hasClashes;
    }

    public void setHasClashes(boolean hasClashes) {
        this.hasClashes = hasClashes;
    }

    public List<String> getViolations() {
        return violations;
    }

    public void setViolations(List<String> violations) {
        this.violations = violations;
    }
}
