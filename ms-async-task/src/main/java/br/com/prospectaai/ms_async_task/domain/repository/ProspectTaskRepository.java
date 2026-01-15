package br.com.prospectaai.ms_async_task.domain.repository;

import br.com.prospectaai.ms_async_task.domain.entity.ProspectTask;
import br.com.prospectaai.ms_async_task.domain.enums.AsyncTaskStatus;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProspectTaskRepository extends JpaRepository<ProspectTask, Long> {
    long countByStatus(AsyncTaskStatus status);

    @Query("""
        select count(t)
        from ProspectTask t
        where t.status = br.com.prospectaai.ms_async_task.domain.enums.AsyncTaskStatus.PROCESSING
          and not exists (select 1 from ProspectionRecord r where r.task = t)
        """)
    long countProcessingWithoutRecords();

    @Query("""
        select t
        from ProspectTask t
        where t.userEmail = :userEmail
          and t.status = :status
        """)
    List<ProspectTask> findByUserEmailAndStatus(@Param(value = "userEmail") String userEmail, @Param(value = "status") AsyncTaskStatus status);

    @Query("""
        select t
        from ProspectTask t
        where t.userEmail = :userEmail
          and t.status in (:statuses)
        """)
    List<ProspectTask> findByUserEmailAndStatuses(@Param(value = "userEmail") String userEmail, @Param(value = "statuses") List<AsyncTaskStatus> statuses);
}
