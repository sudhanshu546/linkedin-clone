package com.org.linkedin.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class BasePageResponse<T> extends BaseResponse<T> {

  private static final long serialVersionUID = 1L;

  private int pageNumber;

  private int pageSize;

  private long totalRecords;
}
