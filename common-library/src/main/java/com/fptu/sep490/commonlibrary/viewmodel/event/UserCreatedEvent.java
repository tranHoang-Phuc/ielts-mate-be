package com.fptu.sep490.commonlibrary.viewmodel.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreatedEvent implements Serializable {
    String userId;
    String email;
}