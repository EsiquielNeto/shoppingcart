package br.com.studies.domain.model;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.json.bind.annotation.JsonbDateFormat;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static br.com.studies.domain.model.Product.findById;


@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Cacheable
@Entity
@Table(name = "product")
public class Product extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String title;
    private String description;

    @CreationTimestamp
    @JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static Uni<List<Product>> getAllProducts() {
        return Product.listAll(Sort.by("created_at"))
                .ifNoItem()
                .after(Duration.ofMillis(10000))
                .fail()
                .onFailure()
                .recoverWithUni(Uni.createFrom().<List<Product>>item(Collections.EMPTY_LIST));
    }

    public static Uni<Product> getById(Long id) {
        return findById(id);
    }

    public static Uni<Product> updateProduct(Long id, Product product) {
        return Panache.withTransaction(() -> getById(id)
                        .onItem()
                        .ifNotNull()
                        .transform(entity -> {
                            entity.setTitle(product.getTitle());
                            entity.setDescription(product.getDescription());
                            return entity;
                        }))
                .onFailure()
                .recoverWithNull();
    }

    public static Uni<Product> addProduct(Product product) {
        return Panache.withTransaction(product::persistAndFlush)
                .replaceWith(product)
                .ifNoItem()
                .after(Duration.ofMillis(10000))
                .fail()
                .onFailure()
                .transform(ex -> new IllegalArgumentException());
    }

    public static Uni<Boolean> deleteProduct(Long id) {
        return Panache.withTransaction(() -> deleteById(id));
    }


}
