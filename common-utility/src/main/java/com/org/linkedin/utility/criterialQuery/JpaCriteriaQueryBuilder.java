package com.org.linkedin.utility.criterialQuery;

import com.org.linkedin.utility.exception.CommonExceptionHandler;
import com.org.linkedin.utility.service.AdvanceSearchCriteria;
import com.org.linkedin.utility.service.AdvancedFilterRelation;
import com.org.linkedin.utility.service.AdvancedFilterSortDirection;
import com.org.linkedin.utility.service.Constants;
import jakarta.persistence.Entity;
import jakarta.persistence.criteria.*;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/*
 * predicates --> expressions
 * CriteriaBuilder to created predicate
 * with predicates,select,where we get the criteriaQuery
 */
@Slf4j
@Component
public class JpaCriteriaQueryBuilder {

  static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  public CriteriaQuery<?> generateJpaCriteriaQuery(
      List<AdvanceSearchCriteria.Filter> filters,
      CriteriaBuilder criteriaBuilder,
      Class<?> targetClass) {
    /*
     * start making criteriaQuery from builder
     */
    CriteriaQuery<?> query = criteriaBuilder.createQuery();

    /*
     * selecting the entity type to query data from
     */
    Root<?> root = query.from(targetClass);

    /*
     * accumulate the and/or operation predicates and orders as well
     */
    List<Predicate> andPredicateList = new ArrayList<>();
    List<Predicate> orPredicateList = new ArrayList<>();
    List<Predicate> betweenPredicateList = new ArrayList<>();
    List<Order> ordersList = new ArrayList<>();

    Order order = null;

    for (AdvanceSearchCriteria.Filter filter : filters) {
      // Handle sorting for nested fields (e.g., tenant.name)
      if (filter.getColumnName().contains(".")) {
        String[] parts = filter.getColumnName().split("\\.");
        String parentField = parts[0]; // e.g., tenant
        String childField = parts[1]; // e.g., name

        // Perform the JOIN to the parent entity
        Join<?, ?> join = root.join(parentField, JoinType.LEFT);
        // Apply value filtering in the child field
        if (filter.getValues() != null) {
          Predicate childPredicate =
              createPredicateForChildEntity(
                  criteriaBuilder, join, childField, filter.getValues(), filter);
          if (AdvancedFilterRelation.AND.equals(filter.getRelation())) {
            andPredicateList.add(childPredicate);
          } else if (AdvancedFilterRelation.OR.equals(filter.getRelation())) {
            orPredicateList.add(childPredicate);
          }
        }
        // Apply the sorting on the child field
        order = criteriaBuilder.asc(join.get(childField)); // Ascending order
        if (filter.getSortDirection() == AdvancedFilterSortDirection.DESC) {
          order = criteriaBuilder.desc(join.get(childField)); // Descending order
        }
      } else {
        if (filter.getValues() != null) {
          Predicate predicate =
              createPredicateForFilter(targetClass, criteriaBuilder, query, root, filter);
          if (AdvancedFilterRelation.AND.equals(filter.getRelation())) {
            andPredicateList.add(predicate);
          } else if (AdvancedFilterRelation.OR.equals(filter.getRelation())) {
            orPredicateList.add(predicate);
          }
        }
        // For non-nested fields, directly use the root entity
        order = criteriaBuilder.asc(root.get(filter.getColumnName()));
        if (filter.getSortDirection() == AdvancedFilterSortDirection.DESC) {
          order = criteriaBuilder.desc(root.get(filter.getColumnName()));
        }
      }

      // Add the order to the list of orders
      if (order != null) {
        ordersList.add(order);
      }
    }

    Predicate finalPredicate = null;
    if (!andPredicateList.isEmpty()) {
      finalPredicate = criteriaBuilder.and(andPredicateList.toArray(Predicate[]::new));
    }
    if (!orPredicateList.isEmpty()) {
      Predicate orPredicate = criteriaBuilder.or(orPredicateList.toArray(Predicate[]::new));
      finalPredicate =
          finalPredicate != null ? criteriaBuilder.and(orPredicate, finalPredicate) : orPredicate;
    }

    if (finalPredicate != null) {
      query.where(finalPredicate).orderBy(ordersList);
    } else if (!CollectionUtils.isEmpty(ordersList)) {
      query.orderBy(ordersList);
    }
    return query;
  }

  public CriteriaQuery<Long> generateJpaCriteriaCountQuery(
      List<AdvanceSearchCriteria.Filter> filters,
      CriteriaBuilder criteriaBuilder,
      Class<?> targetClass) {
    /*
     * start making criteriaQuery from builder
     */
    CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);

    /*
     * selecting the entity type to query data from
     */
    Root<?> root = countQuery.from(targetClass);

    /*
     * accumulate the and/or operation predicates and orders as well
     */
    List<Predicate> andPredicateList = new ArrayList<>();
    List<Predicate> orPredicateList = new ArrayList<>();

    for (AdvanceSearchCriteria.Filter filter : filters) {
      if (filter.getColumnName().contains(".")) {
        String[] parts = filter.getColumnName().split("\\.");
        String parentField = parts[0];
        String childField = parts[1];
        Join<?, ?> join = root.join(parentField, JoinType.LEFT);
        if (filter.getValues() != null) {
          Predicate childPredicate =
              createPredicateForChildEntity(
                  criteriaBuilder, join, childField, filter.getValues(), filter);
          if (AdvancedFilterRelation.AND.equals(filter.getRelation())) {
            andPredicateList.add(childPredicate);
          } else if (AdvancedFilterRelation.OR.equals(filter.getRelation())) {
            orPredicateList.add(childPredicate);
          }
        }
      } else {
        if (filter.getValues() != null) {
          Predicate predicate =
              createPredicateForFilter(targetClass, criteriaBuilder, countQuery, root, filter);
          if (AdvancedFilterRelation.AND.equals(filter.getRelation())) {
            andPredicateList.add(predicate);
          } else if (AdvancedFilterRelation.OR.equals(filter.getRelation())) {
            orPredicateList.add(predicate);
          }
        }
      }
    }
    Predicate finalPredicate = null;
    if (!andPredicateList.isEmpty()) {
      finalPredicate = criteriaBuilder.and(andPredicateList.toArray(Predicate[]::new));
    }
    if (!orPredicateList.isEmpty()) {
      Predicate orPredicate = criteriaBuilder.or(orPredicateList.toArray(Predicate[]::new));
      finalPredicate =
          finalPredicate != null ? criteriaBuilder.and(orPredicate, finalPredicate) : orPredicate;
    }
    // Prepare the query to count "id" fields with the applied predicates
    if (finalPredicate != null) {
      countQuery.select(criteriaBuilder.count(root.get("id"))).where(finalPredicate);
    } else {
      countQuery.select(criteriaBuilder.count(root.get("id")));
    }

    return countQuery;
  }

  /*
   * creiteriaQuery not needed may be
   */

  private Predicate createPredicateForFilter(
      Class<?> targetClass,
      CriteriaBuilder criteriaBuilder,
      CriteriaQuery<?> criteriaQuery,
      Root<?> entity,
      AdvanceSearchCriteria.Filter filter) {

    /*
     * need to build predicate using the operator and column name form filter
     */
    log.info("Enter in createPredicateForFilter method ");
    // Criteria criteria = Criteria.where(filter.getColumnName());

    Field field = getFieldFromHierarchy(targetClass, filter.getColumnName());

    if (field == null) {
      throw new CommonExceptionHandler(
          "Field not found: " + filter.getColumnName(), HttpStatus.BAD_REQUEST.value());
    }

    String columnName = filter.getColumnName();
    Path<?> entityPath = null;
    boolean fieldTypeEntity = false;
    Class<?> fieldType = field.getType();
    if (fieldType.isAnnotationPresent(Entity.class)) {
      // If the field is an entity, modify the column name to point to its primary key
      // (id)
      entityPath = entity.get(columnName).get("id");
      fieldTypeEntity = true;
      columnName = "id";
      fieldType = UUID.class; // Since we're now working with the UUID primary key
    }
    Predicate predicate = null;
    switch (filter.getOperator()) {
      case IS ->
          predicate =
              isPredicateCheck(
                  columnName, entity, fieldType, criteriaBuilder, filter.getValues()); // only
        // for
        // boolean
      case ISNOT ->
          predicate =
              isNotPredicateCheck(
                  columnName, entity, fieldType, criteriaBuilder, filter.getValues()); // only
        // for
        // boolean
      case CONTAINS ->
          predicate =
              containsPredicateCheck(
                  columnName, entity, criteriaBuilder, fieldType, filter.getValues());
      case NOTCONTAINS ->
          predicate =
              notContainsPredicateCheck(
                  columnName, entity, criteriaBuilder, fieldType, filter.getValues());
      case EQUALS ->
          predicate =
              fieldTypeEntity == true
                  ? equalsPredicateCheckEntity(
                      entityPath, criteriaBuilder, fieldType, filter.getValues())
                  : equalsPredicateCheck(
                      columnName, entity, criteriaBuilder, fieldType, filter.getValues());
      case ISEMPTY ->
          predicate = isEmptyPredicateCheck(columnName, entity, criteriaBuilder, fieldType);
      case ISNOTEMPTY ->
          predicate = isNotEmptyPredicateCheck(columnName, entity, criteriaBuilder, fieldType);
      case NOTEQUALS ->
          predicate =
              fieldTypeEntity == true
                  ? notEqualsPredicateCheckEntity(
                      entityPath, criteriaBuilder, fieldType, filter.getValues())
                  : notEqualsPredicateCheck(
                      columnName, entity, fieldType, criteriaBuilder, filter.getValues());
      case GREATERTHAN ->
          predicate =
              greaterThanPredicateCheck(
                  columnName, entity, fieldType, criteriaBuilder, filter.getValues());
      case GREATERTHANOREQUAL ->
          predicate =
              greaterThanOrEqualPredicateCheck(
                  columnName, entity, fieldType, criteriaBuilder, filter.getValues());
      case LESSTHAN ->
          predicate =
              lessThanPredicateCheck(
                  columnName, entity, fieldType, criteriaBuilder, filter.getValues());
      case LESSTHANOREQUAL ->
          predicate =
              lessThanOrEqualPredicateCheck(
                  columnName, entity, fieldType, criteriaBuilder, filter.getValues());
      case BETWEEN ->
          predicate =
              betweenPredicateCheck(
                  columnName, entity, fieldType, criteriaBuilder, filter.getValues());
      case IN ->
          predicate =
              inPredicateCheck(columnName, entity, fieldType, criteriaBuilder, filter.getValues());
      default -> {}
    }
    log.info("returned predicate is : {}" + predicate);
    return predicate;
  }

  private Predicate createPredicateForChildEntity(
      CriteriaBuilder criteriaBuilder,
      Join<?, ?> join,
      String childField,
      List<String> values,
      AdvanceSearchCriteria.Filter filter) {
    Predicate predicate = null;
    switch (filter.getOperator()) {
      case EQUALS -> predicate = criteriaBuilder.equal(join.get(childField), values.get(0));
      case CONTAINS -> {
        String likePattern = "%" + values.get(0).toLowerCase() + "%";
        predicate = criteriaBuilder.like(criteriaBuilder.lower(join.get(childField)), likePattern);
      }
      default -> {}
    }
    return predicate;
  }

  private Predicate inPredicateCheck(
      String columnName,
      Root<?> root,
      Class<?> fieldType,
      CriteriaBuilder criteriaBuilder,
      List<String> values) {

    switch (getFieldType(fieldType)) {
      case Constants.STRING:
        // Handle IN clause for String values
        if (values.size() > 1) {
          // Use IN clause
          return root.get(columnName).in(values);
        } else {
          // Use LIKE clause for a single value
          return criteriaBuilder.like(root.get(columnName), "%" + values.get(0) + "%");
        }
      case Constants.UUID:
        // Handle IN clause for UUID values
        if (values.size() > 1) {
          // Convert values to UUID
          List<UUID> uuidValues =
              values.stream().map(UUID::fromString).collect(Collectors.toList());
          return root.get(columnName).in(uuidValues);
        } else {
          // If there's only one value, treat it as a single match
          UUID uuidValue = UUID.fromString(values.get(0));
          return criteriaBuilder.equal(root.get(columnName), uuidValue);
        }
      default:
        throw new CommonExceptionHandler(
            "Unsupported field type for contains operator", HttpStatus.BAD_REQUEST.value());
    }
  }

  private Predicate betweenPredicateCheck(
      String columnName,
      Root<?> root,
      Class<?> fieldType,
      CriteriaBuilder criteriaBuilder,
      List<String> values) {
    log.info("Entered in Between Criteria");
    if (fieldType == Long.class && values.size() == 2) {
      Path<Long> fromColumn = root.get(columnName);
      long startEpoch = Long.parseLong(values.get(0));
      long endEpoch = Long.parseLong(values.get(1));

      // Date startDate = new Date(startEpoch);
      // Date endDate = new Date(endEpoch);

      return criteriaBuilder.between(fromColumn, startEpoch, endEpoch);

    } else {
      throw new CommonExceptionHandler(
          "Invalid field type or values for BETWEEN operator.", HttpStatus.BAD_REQUEST.value());
    }
  }

  private Predicate lessThanOrEqualPredicateCheck(
      String columnName,
      Root<?> root,
      Class<?> fieldType,
      CriteriaBuilder criteriaBuilder,
      List<String> values) {
    return switch (getFieldType(fieldType)) {
      case Constants.UUID_TYPE, Constants.STRING, Constants.BOOLEAN ->
          throw new CommonExceptionHandler(
              "Invalid field type or values for GreaterThan operator",
              HttpStatus.BAD_REQUEST.value());
      case Constants.LONG ->
          criteriaBuilder.lessThanOrEqualTo(root.get(columnName), Long.parseLong(values.get(0)));
      case Constants.DATE ->
          criteriaBuilder.lessThanOrEqualTo(
              root.get(columnName),
              Date.from(
                  LocalDate.parse(values.get(0), dateFormatter)
                      .atStartOfDay(ZoneOffset.UTC)
                      .toInstant()));
      default -> criteriaBuilder.lessThanOrEqualTo(root.get(columnName), values.get(0));
    };
  }

  private Predicate lessThanPredicateCheck(
      String columnName,
      Root<?> root,
      Class<?> fieldType,
      CriteriaBuilder criteriaBuilder,
      List<String> values) {
    return switch (getFieldType(fieldType)) {
      case Constants.UUID_TYPE, Constants.STRING, Constants.BOOLEAN ->
          throw new CommonExceptionHandler(
              "Invalid field type or values for GreaterThan operator",
              HttpStatus.BAD_REQUEST.value());
      case Constants.LONG ->
          criteriaBuilder.lessThan(root.get(columnName), Long.parseLong(values.get(0)));
      case Constants.DATE ->
          criteriaBuilder.lessThan(
              root.get(columnName),
              Date.from(
                  LocalDate.parse(values.get(0), dateFormatter)
                      .atStartOfDay(ZoneOffset.UTC)
                      .toInstant()));
      default -> criteriaBuilder.lessThan(root.get(columnName), values.get(0));
    };
  }

  private Predicate greaterThanOrEqualPredicateCheck(
      String columnName,
      Root<?> root,
      Class<?> fieldType,
      CriteriaBuilder criteriaBuilder,
      List<String> values) {
    return switch (getFieldType(fieldType)) {
      case Constants.UUID_TYPE, Constants.STRING, Constants.BOOLEAN ->
          throw new CommonExceptionHandler(
              "Invalid field type or values for GreaterThan operator",
              HttpStatus.BAD_REQUEST.value());
      case Constants.LONG ->
          criteriaBuilder.greaterThanOrEqualTo(root.get(columnName), Long.parseLong(values.get(0)));
      case Constants.DATE ->
          criteriaBuilder.greaterThanOrEqualTo(
              root.get(columnName),
              Date.from(
                  LocalDate.parse(values.get(0), dateFormatter)
                      .atStartOfDay(ZoneOffset.UTC)
                      .toInstant()));
      default -> criteriaBuilder.greaterThanOrEqualTo(root.get(columnName), values.get(0));
    };
  }

  private Predicate greaterThanPredicateCheck(
      String columnName,
      Root<?> root,
      Class<?> fieldType,
      CriteriaBuilder criteriaBuilder,
      List<String> values) {
    return switch (getFieldType(fieldType)) {
      case Constants.UUID_TYPE, Constants.STRING, Constants.BOOLEAN ->
          throw new CommonExceptionHandler(
              "Invalid field type or values for GreaterThan operator",
              HttpStatus.BAD_REQUEST.value());
      case Constants.LONG ->
          criteriaBuilder.greaterThan(root.get(columnName), Long.parseLong(values.get(0)));
      case Constants.DATE ->
          criteriaBuilder.greaterThan(
              root.get(columnName),
              Date.from(
                  LocalDate.parse(values.get(0), dateFormatter)
                      .atStartOfDay(ZoneOffset.UTC)
                      .toInstant()));
      default -> criteriaBuilder.greaterThan(root.get(columnName), values.get(0));
    };
  }

  private Predicate notEqualsPredicateCheck(
      String columnName,
      Root<?> root,
      Class<?> fieldType,
      CriteriaBuilder criteriaBuilder,
      List<String> values) {
    return switch (getFieldType(fieldType)) {
      case Constants.STRING -> criteriaBuilder.notEqual(root.get(columnName), values.get(0));
      case Constants.UUID_TYPE ->
          criteriaBuilder.notEqual(root.get(columnName), UUID.fromString(values.get(0)));
      case Constants.BOOLEAN ->
          criteriaBuilder.notEqual(root.get(columnName), Boolean.parseBoolean(values.get(0)));
      case Constants.LONG ->
          criteriaBuilder.notEqual(root.get(columnName), Long.parseLong(values.get(0)));
      case Constants.INTEGER ->
          criteriaBuilder.notEqual(root.get(columnName), Integer.parseInt(values.get(0)));

      default ->
          throw new CommonExceptionHandler(
              "Unsupported field type for equal operator ", HttpStatus.BAD_REQUEST.value());
    };
  }

  private Predicate notEqualsPredicateCheckEntity(
      Path<?> path, CriteriaBuilder criteriaBuilder, Class<?> fieldType, List<String> values) {
    if (fieldType.equals(UUID.class)) {
      // Cast values[0] to UUID as necessary
      return criteriaBuilder.notEqual(path, UUID.fromString(values.get(0).toString()));
    }
    // Handle other field types as needed
    return criteriaBuilder.notEqual(path, values.get(0));
  }

  private Predicate isNotEmptyPredicateCheck(
      String columnName, Root<?> root, CriteriaBuilder criteriaBuilder, Class<?> fieldType) {
    return switch (getFieldType(fieldType)) {
      case Constants.STRING ->
          criteriaBuilder.or(
              criteriaBuilder.isNotNull(root.get(columnName)),
              criteriaBuilder.notEqual(root.get(columnName), ""));
      case Constants.UUID_TYPE, Constants.OBJECT_ID, Constants.LONG ->
          criteriaBuilder.or(
              criteriaBuilder.isNotNull(root.get(columnName)),
              criteriaBuilder.notEqual(root.get(columnName), 0));
      case Constants.DATE -> criteriaBuilder.isNotNull(root.get(columnName));
      default ->
          throw new CommonExceptionHandler(
              "Invalid field type or values for IsNotEmpty operator ",
              HttpStatus.BAD_REQUEST.value());
    };
  }

  private Predicate isEmptyPredicateCheck(
      String columnName, Root<?> root, CriteriaBuilder criteriaBuilder, Class<?> fieldType) {
    return switch (getFieldType(fieldType)) {
      case Constants.STRING ->
          criteriaBuilder.or(
              criteriaBuilder.isNull(root.get(columnName)),
              criteriaBuilder.equal(root.get(columnName), ""));
      case Constants.UUID_TYPE, Constants.OBJECT_ID, Constants.LONG ->
          criteriaBuilder.or(
              criteriaBuilder.isNull(root.get(columnName)),
              criteriaBuilder.equal(root.get(columnName), 0));
      case Constants.DATE -> criteriaBuilder.isNull(root.get(columnName));
      default ->
          throw new CommonExceptionHandler(
              "Invalid field type or values for IsNotEmpty operator ",
              HttpStatus.BAD_REQUEST.value());
    };
  }

  private Predicate equalsPredicateCheck(
      String columnName,
      Root<?> root,
      CriteriaBuilder criteriaBuilder,
      Class<?> fieldType,
      List<String> values) {
    String fieldTypeName = getFieldType(fieldType);
    if (Constants.ENUM.equals(fieldTypeName)) {
      // Handle enum type
      try {
        Enum<?> enumValue = Enum.valueOf((Class<Enum>) fieldType, values.get(0));
        return criteriaBuilder.equal(root.get(columnName), enumValue);
      } catch (IllegalArgumentException e) {
        return criteriaBuilder.disjunction();
      }
    } else {
      return switch (fieldTypeName) {
        case Constants.STRING ->
            criteriaBuilder.equal(
                criteriaBuilder.upper(root.get(columnName).as(String.class)),
                values.get(0).toUpperCase());
        case Constants.UUID_TYPE ->
            criteriaBuilder.equal(root.get(columnName), UUID.fromString(values.get(0)));
        case Constants.BOOLEAN ->
            criteriaBuilder.equal(root.get(columnName), Boolean.parseBoolean(values.get(0)));
        case Constants.LONG ->
            criteriaBuilder.equal(root.get(columnName), Long.parseLong(values.get(0)));
        case Constants.INTEGER ->
            criteriaBuilder.equal(root.get(columnName), Integer.parseInt(values.get(0)));
        default ->
            throw new CommonExceptionHandler(
                "Unsupported field type for equal operator ", HttpStatus.BAD_REQUEST.value());
      };
    }
  }

  private Predicate equalsPredicateCheckEntity(
      Path<?> path, CriteriaBuilder criteriaBuilder, Class<?> fieldType, List<String> values) {
    String value = values.get(0);

    if (fieldType.equals(UUID.class)) {
      // Cast values[0] to UUID as necessary
      return criteriaBuilder.equal(path, UUID.fromString(values.get(0).toString()));
    }
    // Handle other field types as needed
    if (fieldType.equals(String.class)) {
      // Case-insensitive equality comparison for String type
      return criteriaBuilder.equal(
          criteriaBuilder.upper(path.as(String.class)), value.toUpperCase());
    }
    return criteriaBuilder.equal(path, value);
  }

  private Predicate notContainsPredicateCheck(
      String columnName,
      Root<?> root,
      CriteriaBuilder criteriaBuilder,
      Class<?> fieldType,
      List<String> values) {
    switch (getFieldType(fieldType)) {
      case Constants.STRING:
        return criteriaBuilder.notLike(root.get(columnName), "%" + values.get(0) + "%");
      default:
        throw new CommonExceptionHandler(
            "Unsupported field type for notContains operator ", HttpStatus.BAD_REQUEST.value());
    }
  }

  private Predicate containsPredicateCheck(
      String columnName,
      Root<?> root,
      CriteriaBuilder criteriaBuilder,
      Class<?> fieldType,
      List<String> values) {
    switch (getFieldType(fieldType)) {
      case Constants.STRING:
        return criteriaBuilder.like(
            criteriaBuilder.upper(root.get(columnName)), "%" + values.get(0).toUpperCase() + "%");
      default:
        throw new CommonExceptionHandler(
            "Unsupported field type for contains operator ", HttpStatus.BAD_REQUEST.value());
    }
  }

  private Predicate isNotPredicateCheck(
      String columnName,
      Root<?> root,
      Class<?> fieldType,
      CriteriaBuilder criteriaBuilder,
      List<String> values) {

    switch (getFieldType(fieldType)) {
      case Constants.BOOLEAN:
        return criteriaBuilder.notEqual(root.get(columnName), Boolean.parseBoolean(values.get(0)));
      default:
        throw new CommonExceptionHandler(
            "Unsupported field type for containsCriteria operator ",
            HttpStatus.BAD_REQUEST.value());
    }
  }

  private Predicate isPredicateCheck(
      String columnName,
      Root<?> root,
      Class<?> fieldType,
      CriteriaBuilder criteriaBuilder,
      List<String> values) {

    switch (getFieldType(fieldType)) {
      case Constants.BOOLEAN:
        return criteriaBuilder.equal(root.get(columnName), Boolean.parseBoolean(values.get(0)));
      default:
        throw new CommonExceptionHandler(
            "Unsupported field type for containsCriteria operator ",
            HttpStatus.BAD_REQUEST.value());
    }
  }

  private Field getFieldFromHierarchy(Class<?> clazz, String fieldName) {
    Class<?> currentClass = clazz;
    while (currentClass != null) {
      try {
        Field field = currentClass.getDeclaredField(fieldName);
        return field;
      } catch (NoSuchFieldException e) {
        currentClass = currentClass.getSuperclass();
      }
    }
    return null;
  }

  private static String getFieldType(Class<?> fieldType) {
    if (fieldType.isEnum()) {
      return Constants.ENUM;
    }
    return StringUtils.capitalize(fieldType.getSimpleName());
  }
}
