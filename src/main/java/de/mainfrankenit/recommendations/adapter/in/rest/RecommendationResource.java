package de.mainfrankenit.recommendations.adapter.in.rest;
import de.mainfrankenit.recommendations.application.RecommendationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.*;
@Path("/") @Produces(MediaType.APPLICATION_JSON)
public class RecommendationResource {
    @Inject RecommendationService service;

    @POST
    @Path("api/recommendations/preview")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<RecommendationService.Recommendation> preview(RecommendationService.Preferences p){return service.preview(p);}
}