package br.com.prospectaai.ms_async_task.domain.repository;

import br.com.prospectaai.ms_async_task.domain.entity.ProspectTask;
import br.com.prospectaai.ms_async_task.domain.enums.AsyncTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProspectTaskRepository extends JpaRepository<ProspectTask, Long> {
    long countByStatus(AsyncTaskStatus status);

    @Query("""
        select count(t)
        from ProspectTask t
        where t.status = br.com.prospectaai.ms_async_task.domain.enums.AsyncTaskStatus.PROCESSING
          and not exists (select 1 from ProspectionRecord r where r.task = t)
        """)
    long countProcessingWithoutRecords();
}
