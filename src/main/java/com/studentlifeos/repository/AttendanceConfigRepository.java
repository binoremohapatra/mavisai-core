package com.studentlifeos.repository;

import com.studentlifeos.domain.AttendanceConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceConfigRepository extends JpaRepository<AttendanceConfig, Long> {
    Optional<AttendanceConfig> findTopByUserIdOrderByIdDesc(Long userId);
}




