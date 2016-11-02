package dk.statsbiblioteket.reklamefixer.doms;

import dk.statsbiblioteket.reklamefixer.CommercialMetadata;
import dk.statsbiblioteket.doms.reklamefixer.wsgen.centralwebservice.InvalidCredentialsException;
import dk.statsbiblioteket.doms.reklamefixer.wsgen.centralwebservice.InvalidResourceException;
import dk.statsbiblioteket.doms.reklamefixer.wsgen.centralwebservice.MethodFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DOMSCommercialMetadataUpdater {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private DOMSClient domsClient;

    public DOMSCommercialMetadataUpdater(DOMSClient domsClient) {
        this.domsClient = domsClient;
    }

    public void updateMetadata(List<CommercialMetadata> metadataList) {
        metadataList.forEach(this::update);
    }

    private void update(CommercialMetadata metadata) {
        String objectId = metadata.getUuid();
        try {
            if (!domsClient.isActive(objectId)) {
                log.error("Object {} is not currently published. Cannot update. Writing object id to stdout.", objectId);
                System.out.println(objectId + "\tObject has nonactive state: " + domsClient.getState(objectId));
                return;
            }
            domsClient.markInProgressObject(objectId);
            domsClient.modifyDatastream(objectId, metadata);
            domsClient.markPublishedObject(objectId);

        } catch (MethodFailedException | InvalidResourceException | InvalidCredentialsException e) {
            log.error("Error while trying to update '{}'. Writing object id to stdout.", objectId, e);
            System.out.println(objectId + "\tFailed to update object.");
        }
    }
}
