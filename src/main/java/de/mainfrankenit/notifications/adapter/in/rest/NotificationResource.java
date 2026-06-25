package de.mainfrankenit.notifications.adapter.in.rest;
import de.mainfrankenit.notifications.domain.Notification;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.*;
@Path("/") @Produces(MediaType.APPLICATION_JSON)
public class NotificationResource {
    public record NotificationView(UUID id,UUID eventId,String type,String title,String body,Instant createdAt) {
        static NotificationView of(Notification notification) {
            return new NotificationView(
                    notification.id,
                    notification.event == null ? null : notification.event.id,
                    notification.type.name(),
                    notification.title,
                    notification.body,
                    notification.createdAt);
        }
    }

    @GET @Path("api/users/{id}/notifications")
    public List<NotificationView> user(@PathParam("id") UUID id,@QueryParam("limit") @DefaultValue("20") int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 100));
        return Notification.<Notification>find("user.id = ?1 order by createdAt desc", id)
                .page(0, cappedLimit)
                .list()
                .stream()
                .map(NotificationView::of)
                .toList();
    }
}