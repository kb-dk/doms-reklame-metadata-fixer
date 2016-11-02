package dk.statsbiblioteket.reklamefixer.doms;

import dk.statsbiblioteket.reklamefixer.CommercialMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DOMSCommercialMetadataQuerier {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DOMSClient domsClient;

    public DOMSCommercialMetadataQuerier(DOMSClient domsClient) {
        this.domsClient = domsClient;
    }

    public List<CommercialMetadata> retrieveMetadata(List<String> uuids) {
        List<CommercialMetadata> result = new ArrayList<>();
        for(String uuid : uuids){
            try {
                String dataStream = domsClient.getDatastreamContents(uuid);
                result.add(new CommercialMetadata(uuid, dataStream));
            } catch (Exception e) {
                log.error("Error while trying to to read PBCORE from '{}'. Writing object id to stdout.", uuid, e);
                System.out.println(uuid + "\tFailed to retrieve object.");
            }
        }
        return result;
    }

}
