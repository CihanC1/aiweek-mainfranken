package de.mainfrankenit.events.adapter.in.rest;

import de.mainfrankenit.events.application.TaxonomyService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/taxonomy") @Produces(MediaType.APPLICATION_JSON)
public class TaxonomyResource {
    @Inject TaxonomyService taxonomy;

    @GET
    public List<TaxonomyService.CategoryView> categories() {
        return taxonomy.categories();
    }
}
