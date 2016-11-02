package dk.statsbiblioteket.reklamefixer.doms;

import dk.statsbiblioteket.reklamefixer.CommercialMetadata;
import dk.statsbiblioteket.reklamefixer.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.doms.reklamefixer.wsgen.centralwebservice.CentralWebservice;
import dk.statsbiblioteket.doms.reklamefixer.wsgen.centralwebservice.CentralWebserviceService;
import dk.statsbiblioteket.doms.reklamefixer.wsgen.centralwebservice.InvalidCredentialsException;
import dk.statsbiblioteket.doms.reklamefixer.wsgen.centralwebservice.InvalidResourceException;
import dk.statsbiblioteket.doms.reklamefixer.wsgen.centralwebservice.MethodFailedException;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import java.util.Arrays;
import java.util.Map;

/**
 * Central web service methods used in this project
 */
public class DOMSClient {
    private static final QName CENTRAL_WEBSERVICE_SERVICE = new QName(
            "http://central.doms.statsbiblioteket.dk/",
            "CentralWebserviceService");

    private static final String CONNECT_TIMEOUT = "com.sun.xml.ws.connect.timeout";
    private static final String REQUEST_TIMEOUT = "com.sun.xml.ws.request.timeout";

    private static final String DC_DATASTREAM_ID = "PBCORE";

    private final PropertyBasedRegistrarConfiguration configuration;
    private CentralWebservice centralWebservice;

    public DOMSClient(PropertyBasedRegistrarConfiguration configuration) {
        this.configuration = configuration;
    }

    public String getDatastreamContents(String objectId) throws MethodFailedException, InvalidResourceException, InvalidCredentialsException {
        return getCentralWebservice().getDatastreamContents(objectId, DC_DATASTREAM_ID);
    }

    public CentralWebservice getCentralWebservice() {
        if (centralWebservice == null) {
            disableEntityExpansionLimit();
            centralWebservice =
                    new CentralWebserviceService(configuration.getDomsWSAPIEndpoint(), CENTRAL_WEBSERVICE_SERVICE)
                            .getCentralWebservicePort();
            Map<String, Object> context = ((BindingProvider) centralWebservice).getRequestContext();
            context.put(BindingProvider.USERNAME_PROPERTY, configuration.getUsername());
            context.put(BindingProvider.PASSWORD_PROPERTY, configuration.getPassword());
            int domsWSAPIEndpointTimeout = configuration.getDomsWSAPIEndpointTimeout();
            context.put(CONNECT_TIMEOUT, domsWSAPIEndpointTimeout);
            context.put(REQUEST_TIMEOUT, domsWSAPIEndpointTimeout);
        }
        return centralWebservice;
    }

    private void disableEntityExpansionLimit() {
        // JDK 1.7 u45+ enables a security feature per default that limits the number of entity expansions allowed
        // This causes JAX-WS to fail after having run for a while.
        System.getProperties().setProperty("jdk.xml.entityExpansionLimit", "0");
    }

    public void markInProgressObject(String objectId) throws MethodFailedException,
            InvalidResourceException, InvalidCredentialsException {
        getCentralWebservice().markInProgressObject(
                Arrays.asList(objectId), "Preparing to update object for doms-reklame-metadata-fixer");
    }

    public void modifyDatastream(String objectId, CommercialMetadata metadata) throws MethodFailedException,
            InvalidResourceException, InvalidCredentialsException {
        getCentralWebservice().modifyDatastream(
                objectId, DC_DATASTREAM_ID, metadata.getMetadata(), "Updating object for doms-reklame-metadata-fixer");
    }

    public void markPublishedObject(String objectId) throws MethodFailedException,
            InvalidResourceException, InvalidCredentialsException {
        getCentralWebservice().markPublishedObject(
                Arrays.asList(objectId), "Done updating object for doms-reklame-metadata-fixer");
    }

    public boolean isActive(String objectId) throws MethodFailedException,
            InvalidResourceException, InvalidCredentialsException {
        String state = getState(objectId);
        return "A".equals(state);
    }

    public String getState(String objectId) throws MethodFailedException,
            InvalidResourceException, InvalidCredentialsException {
        return getCentralWebservice().getObjectProfile(objectId).getState();
    }
}
