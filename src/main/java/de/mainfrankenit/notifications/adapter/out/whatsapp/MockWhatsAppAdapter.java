package de.mainfrankenit.notifications.adapter.out.whatsapp;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
@ApplicationScoped
public class MockWhatsAppAdapter implements WhatsAppPort {
    @ConfigProperty(name="whatsapp.provider", defaultValue="mock") String provider;
    @ConfigProperty(name="twilio.account-sid") Optional<String> accountSid;
    @ConfigProperty(name="twilio.auth-token") Optional<String> authToken;
    @ConfigProperty(name="twilio.whatsapp-from") Optional<String> from;
    private final HttpClient client=HttpClient.newHttpClient();
    public SendResult send(String phone,String message){if(phone==null||!phone.matches("^\\+[1-9][0-9]{7,14}$"))return new SendResult(false,null,"Invalid E.164 phone number");if("twilio".equalsIgnoreCase(provider))return sendTwilio(phone,message);return new SendResult(true,"mock-"+UUID.randomUUID(),null);}
    private SendResult sendTwilio(String phone,String message){var sid=accountSid.orElse("");var token=authToken.orElse("");var sender=from.orElse("");if(blank(sid)||blank(token)||blank(sender))return new SendResult(false,null,"Twilio configuration is incomplete");try{var endpoint=URI.create("https://api.twilio.com/2010-04-01/Accounts/"+sid+"/Messages.json");var body=form("From",sender)+"&"+form("To","whatsapp:"+phone)+"&"+form("Body",message);var auth=Base64.getEncoder().encodeToString((sid+":"+token).getBytes(StandardCharsets.UTF_8));var request=HttpRequest.newBuilder(endpoint).header("Authorization","Basic "+auth).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build();var response=client.send(request,HttpResponse.BodyHandlers.ofString());if(response.statusCode()>=200&&response.statusCode()<300)return new SendResult(true,"twilio-"+UUID.randomUUID(),null);return new SendResult(false,null,"Twilio returned HTTP "+response.statusCode()+": "+response.body());}catch(Exception e){return new SendResult(false,null,e.getMessage());}}
    private String form(String key,String value){return URLEncoder.encode(key,StandardCharsets.UTF_8)+"="+URLEncoder.encode(value,StandardCharsets.UTF_8);}
    private boolean blank(String value){return value==null||value.isBlank();}
}