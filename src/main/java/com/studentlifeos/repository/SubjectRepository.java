package com.studentlifeos.repository;

import com.studentlifeos.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByUserId(Long userId);
}