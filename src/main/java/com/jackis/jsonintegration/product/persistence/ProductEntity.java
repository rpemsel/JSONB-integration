package com.jackis.jsonintegration.product.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "product")
public class ProductEntity implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column
  private long id;

  @Column(unique = true, nullable = false)
  private String sku;

  @Column()
  @Type(type = "JsonNodeType")
  private JsonNode attributes;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
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

  @Override
  public boolean equals(Object o) {
      if (this == o) {
          return true;
      }
      if (o == null || getClass() != o.getClass()) {
          return false;
      }
    ProductEntity that = (ProductEntity) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}