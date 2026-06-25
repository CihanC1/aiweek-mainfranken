package de.mainfrankenit.identity.application;
import de.mainfrankenit.identity.domain.AppUser;
import de.mainfrankenit.identity.domain.AuthSession;
import de.mainfrankenit.identity.domain.PasswordResetToken;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotAuthorizedException;
import org.mindrot.jbcrypt.BCrypt;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
@ApplicationScoped
public class AuthService {
    public record Tokens(String access,String refresh,AppUser user) {}
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public Tokens register(String displayName,String email,String password) {
        String normalized=normalizeEmail(email);
        if(AppUser.count("email",normalized)>0) throw new IllegalArgumentException("Diese E-Mail-Adresse wird bereits verwendet.");
        var user=new AppUser(); user.displayName=displayName.trim(); user.email=normalized;
        user.passwordHash=BCrypt.hashpw(password,BCrypt.gensalt(12)); user.persist();
        return issue(user);
    }

    @Transactional
    public Tokens login(String email,String password) {
        AppUser user=AppUser.find("email",normalizeEmail(email)).firstResult();
        if(user==null||!user.enabled||user.passwordHash==null||!BCrypt.checkpw(password,user.passwordHash))
            throw new NotAuthorizedException("E-Mail-Adresse oder Passwort ist falsch.");
        AuthSession.update("revoked=true where user=?1 and revoked=false",user);
        return issue(user);
    }

    @Transactional
    public Tokens refresh(String token) {
        if(token==null) throw new NotAuthorizedException("Refresh-Token fehlt.");
        AuthSession old=AuthSession.find("refreshTokenHash=?1 and revoked=false",hash(token)).firstResult();
        if(old==null||old.refreshExpiresAt.isBefore(Instant.now())) throw new NotAuthorizedException("Sitzung ist abgelaufen.");
        old.revoked=true;
        return issue(old.user);
    }

    public AppUser current(String token) {
        if(token==null) throw new NotAuthorizedException("Anmeldung erforderlich.");
        AuthSession session=AuthSession.find("accessTokenHash=?1 and revoked=false",hash(token)).firstResult();
        if(session==null||session.accessExpiresAt.isBefore(Instant.now())||!session.user.enabled)
            throw new NotAuthorizedException("Sitzung ist abgelaufen.");
        return session.user;
    }

    @Transactional public void logout(String access,String refresh) {
        if(access!=null) AuthSession.update("revoked=true where accessTokenHash=?1",hash(access));
        if(refresh!=null) AuthSession.update("revoked=true where refreshTokenHash=?1",hash(refresh));
    }

    @Transactional
    public String forgot(String email) {
        AppUser user=AppUser.find("email",normalizeEmail(email)).firstResult();
        if(user==null) return null;
        PasswordResetToken.update("used=true where user=?1 and used=false",user);
        String raw=token(); var reset=new PasswordResetToken(); reset.user=user; reset.tokenHash=hash(raw);
        reset.expiresAt=Instant.now().plus(Duration.ofMinutes(30)); reset.persist(); return raw;
    }

    @Transactional
    public void reset(String token,String password) {
        PasswordResetToken reset=PasswordResetToken.find("tokenHash=?1 and used=false",hash(token)).firstResult();
        if(reset==null||reset.expiresAt.isBefore(Instant.now())) throw new IllegalArgumentException("Der Link ist ungültig oder abgelaufen.");
        reset.used=true; reset.user.passwordHash=BCrypt.hashpw(password,BCrypt.gensalt(12));
        AuthSession.update("revoked=true where user=?1",reset.user);
    }

    private Tokens issue(AppUser user) {
        String access=token(),refresh=token(); var s=new AuthSession(); s.user=user;
        s.accessTokenHash=hash(access);s.refreshTokenHash=hash(refresh);
        s.accessExpiresAt=Instant.now().plus(Duration.ofMinutes(15));s.refreshExpiresAt=Instant.now().plus(Duration.ofDays(30));s.persist();
        return new Tokens(access,refresh,user);
    }
    private String token(){byte[] b=new byte[32];random.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    private String hash(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private String normalizeEmail(String email){return email==null?"":email.trim().toLowerCase(Locale.ROOT);}
}