package br.com.prospectaai.ms_async_task.domain.repository;

import br.com.prospectaai.ms_async_task.domain.entity.ProspectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface ProspectionRecordRepository extends JpaRepository<ProspectionRecord, Long> {
    long countByCreatedAtBetween(Instant start, Instant end);

    @Query("select count(distinct r.endereco) from ProspectionRecord r where r.endereco is not null and r.endereco <> ''")
    long countDistinctEnderecos();

    @Query("select r.endereco from ProspectionRecord r where r.endereco is not null and r.endereco <> ''")
    List<String> findAllEnderecosNonNull();
}
