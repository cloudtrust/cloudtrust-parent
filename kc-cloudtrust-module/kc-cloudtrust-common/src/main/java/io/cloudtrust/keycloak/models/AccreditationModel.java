package io.cloudtrust.keycloak.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(value = "creationMillis")
public class AccreditationModel {
    private static final Logger LOG = Logger.getLogger(AccreditationModel.class);
    private final DateFormat cloudtrustDateFormat = new SimpleDateFormat("dd.MM.yyyy");

    private String type;
    private String expiryDate;
    private Boolean revoked;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    @JsonIgnore
    public boolean isValid() {
        Instant today = Instant.now().truncatedTo(ChronoUnit.DAYS);
        return isValid(today);
    }

    public boolean isValid(Instant reference) {
        if (type == null || (revoked != null && revoked) || expiryDate == null) {
            return false;
        }
        try {
            Instant accreditationInstant = cloudtrustDateFormat.parse(expiryDate).toInstant();
            return accreditationInstant.isAfter(reference);
        } catch (ParseException e) {
            LOG.warn("Could not parse expiryDate " + expiryDate);
            return false;
        }
    }

    public Boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
    }

    public static AccreditationModel tryParse(String json) {
        if (json != null) {
            try {
                return new ObjectMapper().readValue(json, AccreditationModel.class);
            } catch (IOException e) {
                LOG.warnf(e, "Can't parse %s", json);
            }
        }
        return null;
    }

    public String toJSON() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }
}
