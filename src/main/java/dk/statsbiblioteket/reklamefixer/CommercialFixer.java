package dk.statsbiblioteket.reklamefixer;

import dk.statsbiblioteket.reklamefixer.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.reklamefixer.doms.DOMSClient;
import dk.statsbiblioteket.reklamefixer.doms.DOMSCommercialMetadataQuerier;
import dk.statsbiblioteket.reklamefixer.doms.DOMSCommercialMetadataUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CommercialFixer {

    private static final String UUIDS_FILENAME = "/commercial-uuids";
    private static final Logger log = LoggerFactory.getLogger(CommercialFixer.class);
    private DOMSCommercialMetadataQuerier metadataQuerier;
    private DOMSCommercialMetadataUpdater metadataUpdater;
    private List<String> commercialUuids;


    public static void main(String[] args) {
        PropertyBasedRegistrarConfiguration config = new PropertyBasedRegistrarConfiguration(
                new File(System.getProperty("user.home"), "doms-reklamefixer.properties"));
        CommercialFixer commercialFixer = new CommercialFixer(config);

        commercialFixer.fixAllCommercials();
    }


    public CommercialFixer(PropertyBasedRegistrarConfiguration configuration) {
        this(new DOMSClient(configuration));
    }

    public CommercialFixer(DOMSClient domsClient) {
        this(new DOMSCommercialMetadataQuerier(domsClient),
                new DOMSCommercialMetadataUpdater(domsClient),
                fetchUuids());
    }

    public CommercialFixer(DOMSCommercialMetadataQuerier metadataQuerier, DOMSCommercialMetadataUpdater metadataUpdater,
                           List<String> commercialUuids) {
        this.metadataQuerier = metadataQuerier;
        this.metadataUpdater = metadataUpdater;
        this.commercialUuids = commercialUuids;
    }

    public void fixAllCommercials() {
        log.info("Retrieving commercial metadata.");
        List<CommercialMetadata> allCommercialMetadata = metadataQuerier.retrieveMetadata(commercialUuids);
        List<CommercialMetadata> cinemaCommercialMetadata = new ArrayList<>();
        List<CommercialMetadata> tv2CommercialMetadata = new ArrayList<>();

        for (CommercialMetadata commercial : allCommercialMetadata) {
            String assetType = commercial.getAssetType();
            if(assetType.equals("Biografreklamefilm")){
                cinemaCommercialMetadata.add(commercial);
            }
            else if(assetType.equals("Tv2reklamefilm")){
                tv2CommercialMetadata.add(commercial);
            }
        }
        log.info("Cinema commercial metadata retrieved: {}. Tv2 commercial metadata retrieved: {}.",
                cinemaCommercialMetadata.size(), tv2CommercialMetadata.size());

        List<CommercialMetadata> updatedCinemaCommercialMetadata = updateCinemaMetadata(cinemaCommercialMetadata);
        List<CommercialMetadata> updatedTv2CommercialMetadata = updateTv2Metadata(tv2CommercialMetadata);

        List<CommercialMetadata> updatedMetadata = new ArrayList<>();
        updatedMetadata.addAll(updatedCinemaCommercialMetadata);
        updatedMetadata.addAll(updatedTv2CommercialMetadata);

        log.info("Updating metadata for {} cinema commercials and {} TV2 commercials.",
                updatedCinemaCommercialMetadata.size(), updatedTv2CommercialMetadata.size());
        if(updatedMetadata.size() > 0){
            metadataUpdater.updateMetadata(updatedMetadata);
        }
    }

    private List<CommercialMetadata> updateCinemaMetadata(List<CommercialMetadata> commercialMetadata) {
        List<CommercialMetadata> result = new ArrayList<>();
        commercialMetadata.forEach(metadata -> {
                        if(metadata.moveAlternativeTitle()){
                            result.add(metadata);
                        }
                    });
        return result;
    }

    private List<CommercialMetadata> updateTv2Metadata(List<CommercialMetadata> commercialMetadata) {
        List<CommercialMetadata> result = new ArrayList<>();
        commercialMetadata.forEach(metadata -> {
            boolean tv2InfoInserted = metadata.insertTv2Info();
            boolean alternativeTitleMoved = metadata.moveAlternativeTitle();
            if(tv2InfoInserted || alternativeTitleMoved){
                result.add(metadata);
            }
        });
        return result;
    }

    private static ArrayList<String> fetchUuids() {
        log.info("Fetching ids from '{}'", UUIDS_FILENAME);
        InputStream uuidsStream = CommercialFixer.class.getResourceAsStream(UUIDS_FILENAME);
        ArrayList<String> result = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(uuidsStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error trying to fetch ids from '" + UUIDS_FILENAME + "'.");
        }
        return result;
    }

}
