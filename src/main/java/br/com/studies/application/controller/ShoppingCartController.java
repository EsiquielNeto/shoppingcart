package br.com.studies.application.controller;


import br.com.studies.domain.model.ShoppingCart;
import io.smallrye.mutiny.Uni;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/v1/carts")
public class ShoppingCartController {
    
    @GET
    public Uni<Response> getCarts() {
        return ShoppingCart.getAllShoppingCarts()
                .onItem().transform(Response::ok)
                .onItem().transform(Response.ResponseBuilder::build);
    }

    @GET
    @Path("{id}")
    public Uni<Response> getSingleCart(@PathParam("id") Long id) {
        return ShoppingCart.findByShoppingCartId(id)
                .onItem().transform(shoppingCart -> Response.ok(shoppingCart).build())
                .onItem().ifNull().continueWith(Response.ok().status(NOT_FOUND)::build);
    }

    @POST
    public Uni<Response> createShoppingCart(ShoppingCart shoppingCart) {
        if (shoppingCart == null || shoppingCart.getName() == null) {
            throw new WebApplicationException("ShoppingCart name was not set on request.", 422);
        }
        return ShoppingCart.createShoppingCart(shoppingCart)
                .onItem().transform(cart -> URI.create("/v1/carts/" + cart.getId()))
                .onItem().transform(Response::created)
                .onItem().transform(Response.ResponseBuilder::build);
    }

    @PUT
    @Path("{cartid}/{productid}")
    public Uni<Response> update(@PathParam("cartid") Long id, @PathParam("productid") Long product) {
        return ShoppingCart.addProductToShoppingCart(id, product)
                .onItem().ifNotNull().transform(entity -> Response.ok(entity).build())
                .onItem().ifNull().continueWith(Response.ok().status(NOT_FOUND)::build);

    }

    @DELETE
    @Path("{cartid}/{productid}")
    public Uni<Response> delete(@PathParam("cartid") Long id, @PathParam("productid") Long product) {
        return ShoppingCart.deleteFromShoppingCart(id, product)
                .onItem().ifNotNull().transform(entity -> Response.ok(entity).build())
                .onItem().ifNull().continueWith(Response.ok().status(NOT_FOUND)::build);
    }

}
