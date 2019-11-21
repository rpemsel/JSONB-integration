package com.jackis.jsonintegration.product;

import java.util.Map;

public class Product {

    private String sku;
    private Map<String, Object> attributes;

    public Product(String sku, Map<String, Object> attributes) {
        this.sku = sku;
        this.attributes = attributes;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
