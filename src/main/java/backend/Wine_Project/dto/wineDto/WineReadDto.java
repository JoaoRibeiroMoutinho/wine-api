package backend.Wine_Project.dto.wineDto;

import backend.Wine_Project.model.wine.GrapeVarieties;
import backend.Wine_Project.model.wine.Region;
import backend.Wine_Project.model.wine.WineType;

import java.time.Year;
import java.util.Set;

public record WineReadDto(

        String name,
        WineType wineType,
        Set<GrapeVarieties> grapeVarietiesId,
        Region region,
        double price,
        double alcohol,
        int year
) {
}
