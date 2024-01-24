package backend.Wine_Project.repository;

import backend.Wine_Project.grapeVarietiesDto.GrapeVarietiesDto;
import backend.Wine_Project.model.GrapeVarieties;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GrapeVarietiesRepository extends JpaRepository<GrapeVarieties, Long> {

    Optional<GrapeVarieties> findByName(String name);
}