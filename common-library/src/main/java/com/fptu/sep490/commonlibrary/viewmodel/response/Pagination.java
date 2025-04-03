package com.fptu.sep490.commonlibrary.viewmodel.response;

public record Pagination(
        int currentPage,
        int totalPages,
        int pageSize,
        int totalItems,
        boolean hasNextPage,
        boolean hasPreviousPage
) {

}

