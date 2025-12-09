package com.project.moneyj.trip.dto;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryListRequestDTO {

    @NotNull
    private List<CategoryDTO> categoryDTOList;
}
