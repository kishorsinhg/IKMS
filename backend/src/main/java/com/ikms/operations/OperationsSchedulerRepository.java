package com.ikms.operations;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationsSchedulerRepository extends JpaRepository<OperationsScheduler, String> {

  List<OperationsScheduler> findAllByOrderByDisplayNameAsc();
}

