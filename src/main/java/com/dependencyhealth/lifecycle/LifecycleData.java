package com.dependencyhealth.lifecycle;

public class LifecycleData {
    private String product;
    private String version;
    private boolean isEol;
    private String eolDate;
    private String latestVersion;

    public LifecycleData() {
    }

    public LifecycleData(String product, String version, boolean isEol, String eolDate, String latestVersion) {
        this.product = product;
        this.version = version;
        this.isEol = isEol;
        this.eolDate = eolDate;
        this.latestVersion = latestVersion;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isEol() {
        return isEol;
    }

    public void setEol(boolean eol) {
        isEol = eol;
    }

    public String getEolDate() {
        return eolDate;
    }

    public void setEolDate(String eolDate) {
        this.eolDate = eolDate;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(String latestVersion) {
        this.latestVersion = latestVersion;
    }
}
