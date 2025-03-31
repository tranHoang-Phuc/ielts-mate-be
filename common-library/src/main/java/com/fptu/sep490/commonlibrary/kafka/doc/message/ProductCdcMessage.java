package com.fptu.sep490.commonlibrary.kafka.doc.message;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCdcMessage {
    private Product after;

    private Product before;

    private Operation op;
}
