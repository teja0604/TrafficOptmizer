package com.routeoptimizer.repository;

import com.routeoptimizer.model.Road;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoadRepository extends JpaRepository<Road, Long> {
    boolean existsByFromCityIdAndToCityId(Long fromCityId, Long toCityId);
}
