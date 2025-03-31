package com.fptu.sep490.commonlibrary.kafka.doc.message;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private long id;
    private boolean isPublished;
}
