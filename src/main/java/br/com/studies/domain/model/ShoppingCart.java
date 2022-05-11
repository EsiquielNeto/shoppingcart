package br.com.studies.domain.model;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple4;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.reactive.mutiny.Mutiny;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import java.time.Duration;
import java.util.List;
import java.util.Set;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@NamedQueries(
        value = {
                @NamedQuery(name = "ShoppingCart.findAll",
                        query = "SELECT c FROM ShoppingCart c LEFT JOIN FECTH c.cartItems item LEFT JOIN FECTH item.product"),
                @NamedQuery(name = "ShoppingCart.getById",
                        query = "SELECT c FROM ShoppingCart c LEFT JOIN FETCH c.cartItems item LEFT JOIN FETCH item.product where c.id = ?1")
        })
public class ShoppingCart extends PanacheEntityBase {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "cart_total")
    private int cartTotal;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn
    private Set<ShoppingCartItem> cartItems;

    private String name;

    public void calculateCartTotal() {
        cartTotal = cartItems.stream().mapToInt(ShoppingCartItem::getQuantity).sum();
    }

    public static Uni<ShoppingCart> findByShoppingCartId(Long id) {
        return find("#ShoppingCart.getById", id).firstResult();
    }

    public static Uni<List<ShoppingCart>> getAllShoppingCarts() {
        return find("#ShoppingCart.findAll").list();
    }

    public static Multi<ShoppingCart> findAllWithJoinfetch() {
        return stream("SELECT c FROM ShoppingCart c LEFT JOIN FETCH c.cartItems");
    }

    public static Uni<ShoppingCart> createShoppingCart(ShoppingCart shoppingCart) {
        return Panache.withTransaction(shoppingCart::persist)
                .replaceWith(shoppingCart)
                .ifNoItem()
                .after(Duration.ofMillis(10000))
                .fail()
                .onFailure()
                .transform(IllegalArgumentException::new);
    }

    public static Uni<ShoppingCart> addProductToShoppingCart(Long shoppingCartId, Long productId) {
        Uni<ShoppingCart> cart = findById(shoppingCartId);
        Uni<Set<ShoppingCartItem>> cartItems = cart
                .chain(shoppingCart -> Mutiny.fetch(shoppingCart.getCartItems()))
                .onFailure()
                .recoverWithNull();

        Uni<Product> product = Product.findById(productId);
        Uni<ShoppingCartItem> items = ShoppingCartItem.findByCartIdByProductId(shoppingCartId, productId).toUni();

        Uni<Tuple4<ShoppingCart, Set<ShoppingCartItem>, ShoppingCartItem, Product>> responses = Uni.combine()
                .all()
                .unis(cart, cartItems, items, product)
                .asTuple();

        return Panache.withTransaction(() -> responses
                .onItem()
                .ifNotNull()
                .transform(entity -> {
                    if (entity.getItem1() == null || entity.getItem4() == null || entity.getItem2() == null) {
                        return null;
                    }

                    if (entity.getItem3() == null) {
                        ShoppingCartItem cartItem = ShoppingCartItem.builder()
                                .cart(entity.getItem1())
                                .product(entity.getItem4())
                                .quantity(1)
                                .build();

                        entity.getItem2().add(cartItem);
                    } else {
                        int sumQuantity = entity.getItem3().getQuantity() + 1;
                        entity.getItem3().setQuantity(sumQuantity);
                    }
                    entity.getItem1().calculateCartTotal();
                    return entity.getItem1();
                })
        );
    }

    public static Uni<ShoppingCart> deleteFromShoppingCart(Long shoppingCartId, Long productId) {
        Uni<ShoppingCart> cart = findById(shoppingCartId);
        Uni<Set<ShoppingCartItem>> cartItems = cart
                .chain(shoppingCart -> Mutiny.fetch(shoppingCart.getCartItems()))
                .onFailure()
                .recoverWithNull();

        Uni<Product> product = Product.findById(productId);
        Uni<ShoppingCartItem> items = ShoppingCartItem.findByCartIdByProductId(shoppingCartId, productId).toUni();

        Uni<Tuple4<ShoppingCart, Set<ShoppingCartItem>, ShoppingCartItem, Product>> responses = Uni.combine()
                .all()
                .unis(cart, cartItems, items, product)
                .asTuple();

        return Panache.withTransaction(() -> responses
                .onItem()
                .ifNotNull()
                .transform(entity -> {
                    if (entity.getItem1() == null || entity.getItem4() == null || entity.getItem3() == null) {
                        return null;
                    }

                    int subtractQuantity = entity.getItem3().getQuantity() -1;
                    entity.getItem3().setQuantity(subtractQuantity);

                    if (entity.getItem3().getQuantity() == 0) {
                        entity.getItem2().remove(entity.getItem3());
                    }

                    entity.getItem1().calculateCartTotal();
                    return entity.getItem1();
                })
        );
    }
}
