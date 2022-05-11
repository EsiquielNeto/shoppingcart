package br.com.studies.application.controller;

import br.com.studies.domain.model.Product;
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

@Consumes()
@Produces(MediaType.APPLICATION_JSON)
@Path("/v1/products")
public class ProductController {
    
    @GET
    public Uni<Response> getProducts() {
        return Product.getAllProducts()
                .onItem().transform(Response::ok)
                .onItem().transform(Response.ResponseBuilder::build);
    }

    @GET
    @Path("{id}")
    public Uni<Response> getSingleProduct(@PathParam("id") Long id) {
        return Product.getById(id)
                .onItem().ifNotNull().transform(product -> Response.ok(product).build())
                .onItem().ifNull().continueWith(Response.ok().status(NOT_FOUND)::build);
    }

    @POST
    public Uni<Response> addProduct(Product product) {
        return Product.addProduct(product)
                .onItem().transform(id -> URI.create("v1/products/" + id.getId()))
                .onItem().transform(uri -> Response.created(uri))
                .onItem().transform(Response.ResponseBuilder::build);
    }

    @PUT
    @Path("{id}")
    public Uni<Response> updateProduct(@PathParam("id") Long id, Product product) {
        if (product == null || product.getDescription() == null) {
            throw new WebApplicationException("Product description was not set on request.", 422);
        }
        return Product.updateProduct(id, product)
                .onItem().ifNotNull().transform(entity -> Response.ok(entity).build())
                .onItem().ifNull().continueWith(Response.ok().status(NOT_FOUND)::build);
    }
    
    @DELETE
    @Path("{id}")
    public Uni<Response> deleteProduct(@PathParam("id") Long id) {
        return Product.deleteProduct(id)
                .onItem().transform(entity -> !entity ? Response.serverError().status(NOT_FOUND).build()
                        : Response.ok().status(Response.Status.OK).build());
    }
}
