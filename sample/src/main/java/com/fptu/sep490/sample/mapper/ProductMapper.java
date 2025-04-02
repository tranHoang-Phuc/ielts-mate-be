package com.fptu.sep490.sample.mapper;

import com.fptu.sep490.sample.model.Product;
import com.fptu.sep490.sample.viewmodel.ProductGetVm;
import com.fptu.sep490.sample.viewmodel.ProductPostVm;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product toModel(ProductPostVm vm);
    ProductGetVm toProductGetVm(Product product);
}
