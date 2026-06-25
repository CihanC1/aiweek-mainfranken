package de.mainfrankenit.shared.web;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import java.time.Instant;
import java.util.*;
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Exception> {
    public record ErrorResponse(String code,String message,Instant timestamp,List<String> details){}
    public Response toResponse(Exception ex){
        int status=ex instanceof WebApplicationException w?w.getResponse().getStatus():ex instanceof ConstraintViolationException||ex instanceof IllegalArgumentException?400:500;
        var details=ex instanceof ConstraintViolationException c?c.getConstraintViolations().stream().map(v->v.getPropertyPath()+": "+v.getMessage()).toList():List.<String>of();
        String message=status==500?"Unexpected server error":ex.getMessage();
        String code=status==404?"NOT_FOUND":status==400?"VALIDATION_ERROR":status==401?"UNAUTHORIZED":status==403?"FORBIDDEN":"INTERNAL_ERROR";
        return Response.status(status).entity(new ErrorResponse(code,message,Instant.now(),details)).build();
    }
}