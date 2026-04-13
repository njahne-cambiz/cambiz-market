package com.cambiz.market.repository;

import com.cambiz.market.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findByNameEnIgnoreCase(String nameEn);
    
    Optional<Category> findByNameFrIgnoreCase(String nameFr);
    
    List<Category> findByParentIsNullOrderBySortOrderAsc();
    
    List<Category> findByParentIdOrderBySortOrderAsc(Long parentId);
}