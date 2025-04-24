package com.fptu.sep490.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipientUser {
    String email;
    String userId;
    String firstName;
    String lastName;
}
