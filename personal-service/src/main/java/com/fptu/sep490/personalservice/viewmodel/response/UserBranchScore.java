package com.fptu.sep490.personalservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record UserBranchScore(
        @JsonProperty("branch_score")
        String branchScore,
        @JsonProperty("number_of_users")
        Integer numberOfUsers,
        @JsonProperty("fill")
        String color
) {
}
