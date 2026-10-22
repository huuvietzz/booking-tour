package com.example.tour.tour.repository;

import com.example.tour.tour.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Integer>,
        JpaSpecificationExecutor<Location> {

    Optional<Location> findByLocationIdAndIsActiveTrue(int locationId);

    // Địa điểm đang hiển thị, theo tên A → Z (dropdown / bộ lọc phía khách)
    List<Location> findByIsActiveTrueOrderByLocationNameAsc();
}