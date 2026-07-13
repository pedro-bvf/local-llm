package com.demo.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Embeddable value type. Hibernate Assistant reports this to the LLM as an
 * {@code embeddable} in the metamodel, so questions like "which suppliers are
 * in Italy?" resolve against {@code supplier.address.country} without anyone
 * describing the schema by hand.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {

  private String street;
  private String city;
  private String country;

}
