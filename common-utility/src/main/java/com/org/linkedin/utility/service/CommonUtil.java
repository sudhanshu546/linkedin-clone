package com.org.linkedin.utility.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.linkedin.utility.criterialQuery.JpaCriteriaQueryBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class CommonUtil {

  @PersistenceContext private final EntityManager entityManager;

  private final JpaCriteriaQueryBuilder jpaQueryBuilder;

  /**
   * Generates a unique pocket name by replacing spaces with underscores and converting to
   * lowercase.
   *
   * @param name the name of the pocket
   * @return the unique pocket name
   */
  public String generateUniquePocketName(String name) {
    log.info("Enter  generateUniquePocketName  Method");
    String uniqueName = name.trim().toLowerCase().replaceAll("\\s+", "_");
    log.info("Exit  generateUniquePocketName Method");
    return uniqueName;
  }

  /**
   * Checks if the given string is a valid JSON.
   *
   * @param jsonString the JSON string to validate
   * @return true if the string is valid JSON, false otherwise
   */
  public static boolean isValidJson(String jsonString) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.readTree(jsonString);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Generates a CriteriaQuery for JPA based on the provided filters and collection class.
   *
   * @param filters the list of filters to apply
   * @param collectionClass the class of the collection
   * @return the CriteriaQuery object
   */
  public CriteriaQuery<?> getJpaQuery(
      List<AdvanceSearchCriteria.Filter> filters, Class<?> collectionClass) {
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    return jpaQueryBuilder.generateJpaCriteriaQuery(filters, criteriaBuilder, collectionClass);
  }

  /**
   * Adds a filter to ensure only non-deleted records are retrieved.
   *
   * @param filters the list of filters to which the "isDeleted" filter will be added
   */
  public void addIsEnabledFilter(List<AdvanceSearchCriteria.Filter> filters) {
    AdvanceSearchCriteria.Filter isDeletedfilter = new AdvanceSearchCriteria.Filter();
    isDeletedfilter.setColumnName(Constants.IS_DELETED);
    isDeletedfilter.setOperator(AdvancedFilterOperator.IS);
    isDeletedfilter.setValues(List.of("false"));
    isDeletedfilter.setRelation(AdvancedFilterRelation.AND);
    filters.add(isDeletedfilter);
  }

  /**
   * Adds a filter to sort records by the latest update date in descending order.
   *
   * @param filters the list of filters to which the sorting filter will be added
   */
  public void sortByLatestRecord(List<AdvanceSearchCriteria.Filter> filters) {
    AdvanceSearchCriteria.Filter updatedByLatestGroupBooking = new AdvanceSearchCriteria.Filter();
    updatedByLatestGroupBooking.setColumnName("lastModifiedDate");
    updatedByLatestGroupBooking.setRelation(AdvancedFilterRelation.AND);
    updatedByLatestGroupBooking.setSortDirection(AdvancedFilterSortDirection.DESC);
    // add default filter of updated by latest group-booking
    filters.add(updatedByLatestGroupBooking);
  }

  public long getTotalCount(Class<?> className, List<AdvanceSearchCriteria.Filter> filters) {
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> countQuery =
        jpaQueryBuilder.generateJpaCriteriaCountQuery(filters, criteriaBuilder, className);
    long totalCount = entityManager.createQuery(countQuery).getSingleResult();
    return totalCount;
  }
}
