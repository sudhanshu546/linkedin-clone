package com.org.linkedin.utility.service;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdvanceSearchCriteria {
  private int pageNumber;
  private int pageSize;
  private List<Filter> filters;
  private AdvancedFilterRelation relation;
  private List<String> headers;

  @Getter
  @Setter
  public static class Filter {
    private String columnName;
    private AdvancedFilterOperator operator;
    private List<String> values;
    private AdvancedFilterRelation relation = AdvancedFilterRelation.OR;
    private AdvancedFilterSortDirection sortDirection = AdvancedFilterSortDirection.ASC;
  }
}
