package com.jackis.jsonintegration.product.rest;

import com.fasterxml.jackson.databind.JsonNode;

public class Product {

  private String sku;
  private JsonNode attributes;

  public Product(String sku, JsonNode attributes) {
    this.sku = sku;
    this.attributes = attributes;
  }

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public JsonNode getAttributes() {
    return attributes;
  }

  public void setAttributes(JsonNode attributes) {
    this.attributes = attributes;
  }
}
