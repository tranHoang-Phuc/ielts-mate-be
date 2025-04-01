package fptu.sep490.sample.repository;

import fptu.sep490.sample.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p")
    List<Product> getLatestProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.id IN :productIds")
    Page<Product> findAllPublishedProductsByIds(@Param("productIds") List<Long> productIds, Pageable pageable);
}
