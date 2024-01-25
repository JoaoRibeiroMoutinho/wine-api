package backend.Wine_Project.dto.wineDto;

import backend.Wine_Project.model.wine.GrapeVarieties;
import backend.Wine_Project.model.wine.Region;
import backend.Wine_Project.model.wine.WineType;

import java.time.Year;
import java.util.List;
import java.util.Set;

public record WineCreateDto(
        String name,
        Long wineTypeId,
        Set<Long> grapeVarietiesId,
        Long regionId,
        double price,
        double alcohol,
        int year
) {
}
