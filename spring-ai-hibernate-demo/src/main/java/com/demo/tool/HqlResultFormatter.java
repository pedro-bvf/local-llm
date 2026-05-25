package com.demo.tool;

import com.demo.model.Category;
import com.demo.model.Product;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HqlResultFormatter {

  public String format(List<?> results) {
    return results.stream()
      .map(this::formatResult)
      .collect(Collectors.joining("\n"));
  }

  private String formatResult(Object result) {
    if (result instanceof Product product) {
      String categoryName = product.getCategory() == null ? null : product.getCategory().getName();
      return "Product{name='%s', price=%s, stock=%d, category='%s'}"
        .formatted(product.getName(), product.getPrice(), product.getStock(), categoryName);
    }

    if (result instanceof Category category) {
      return "Category{name='%s'}".formatted(category.getName());
    }

    if (result instanceof Object[] row) {
      return Arrays.stream(row)
        .map(String::valueOf)
        .collect(Collectors.joining(" | "));
    }

    return String.valueOf(result);
  }
}
