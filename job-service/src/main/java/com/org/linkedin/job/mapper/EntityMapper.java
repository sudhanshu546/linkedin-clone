package com.org.linkedin.job.mapper;

import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

public interface EntityMapper<D, E> {
  E toEntity(D dto);

  D toDto(E entity);

  List<E> toEntity(List<D> dtoList);

  List<D> toDto(List<E> entityList);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void partialUpdate(@MappingTarget E entity, D dto);
}
