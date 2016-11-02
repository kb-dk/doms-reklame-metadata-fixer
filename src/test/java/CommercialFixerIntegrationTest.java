import dk.statsbiblioteket.reklamefixer.CommercialFixer;
import dk.statsbiblioteket.reklamefixer.CommercialMetadata;
import dk.statsbiblioteket.reklamefixer.doms.DOMSClient;
import dk.statsbiblioteket.reklamefixer.doms.DOMSCommercialMetadataQuerier;
import dk.statsbiblioteket.reklamefixer.doms.DOMSCommercialMetadataUpdater;
import dk.statsbiblioteket.reklamefixer.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.doms.reklamefixer.wsgen.centralwebservice.CentralWebservice;
import dk.statsbiblioteket.doms.reklamefixer.wsgen.centralwebservice.InvalidCredentialsException;
import dk.statsbiblioteket.doms.reklamefixer.wsgen.centralwebservice.InvalidResourceException;
import dk.statsbiblioteket.doms.reklamefixer.wsgen.centralwebservice.MethodFailedException;
import dk.statsbiblioteket.doms.reklamefixer.wsgen.centralwebservice.Relation;
import dk.statsbiblioteket.util.Strings;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.XMLUnit;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.testng.Assert.assertTrue;

public class CommercialFixerIntegrationTest {

    public static final int NUMBER_OF_EACH_TEST_COMMERCIAL = 3;
    private DOMSClient domsClientSpy;
    private ArrayList<String> ids;
    private String correctCinemaMetadata;
    private String correctTv2Metadata;
    private PrintStream originalStdout;
    private PrintStream stdoutMock;

    @BeforeMethod
    public void setUp() throws Exception {

        stdoutMock = mock(PrintStream.class);
        originalStdout = System.out;
        System.setOut(stdoutMock);

        PropertyBasedRegistrarConfiguration configuration
                = new PropertyBasedRegistrarConfiguration(
                getClass().getResourceAsStream("/doms-reklamefixer-test.properties"));
        DOMSClient domsClient = new DOMSClient(configuration);
        domsClientSpy = spy(domsClient);

        String cinemaMetadata = Strings.flush(getClass().getResourceAsStream("cinema-metadata-example.xml"));
        String tv2Metadata = Strings.flush(getClass().getResourceAsStream("tv2-metadata-example.xml"));

        correctCinemaMetadata = Strings.flush(getClass().getResourceAsStream("updated-cinema-metadata-example.xml"));
        correctTv2Metadata = Strings.flush(getClass().getResourceAsStream("updated-tv2-metadata-example.xml"));

        ids = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_EACH_TEST_COMMERCIAL; i++) {
            String id = createObject(domsClientSpy, cinemaMetadata);
            ids.add(id);
        }
        for (int i = 0; i < NUMBER_OF_EACH_TEST_COMMERCIAL; i++) {
            String id = createObject(domsClientSpy, tv2Metadata);
            ids.add(id);
        }

        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        for (String id : ids) {
            System.out.println("Deleting object:" + id);
            deleteObject(domsClientSpy, id);
        }

        System.setOut(originalStdout);
    }

    @Test
    public void test_fixAllCommercials_withoutUpdatingDOMS() throws Exception {

        DOMSCommercialMetadataQuerier domsQuerier = new DOMSCommercialMetadataQuerier(domsClientSpy);

        DOMSCommercialMetadataUpdater domsUpdaterMock = mock(DOMSCommercialMetadataUpdater.class);

        CommercialFixer commercialFixer =
                new CommercialFixer(domsQuerier, domsUpdaterMock, ids);



        commercialFixer.fixAllCommercials();



        // Verify that DOMS is updated with correct metadata.
        ArgumentCaptor<List<CommercialMetadata>> metadataCaptor = ArgumentCaptor.forClass((Class)List.class);

        verify(domsUpdaterMock).updateMetadata(metadataCaptor.capture());

        List<CommercialMetadata> updatedCommercials = metadataCaptor.getValue();

        for (CommercialMetadata updatedCommercial : updatedCommercials) {
            String assetType = updatedCommercial.getAssetType();
            boolean biografreklamefilm = assetType.equals("Biografreklamefilm");
            boolean tv2reklamefilm = assetType.equals("Tv2reklamefilm");

            assertTrue(biografreklamefilm || tv2reklamefilm,
                    "Asset type of commercials should be either 'Biografreklamefilm' or 'Tv2reklamefilm'" +
                    ", but was '" + assetType + "'");

            String metadata = updatedCommercial.getMetadata();

            if(biografreklamefilm) {
                DetailedDiff diff = new DetailedDiff(XMLUnit.compareXML(correctCinemaMetadata, metadata));
                boolean cinemaMetadataIsCorrect = diff.identical();
                assertTrue(cinemaMetadataIsCorrect, diff.getAllDifferences().toString());
            }
            else {
                DetailedDiff diff = new DetailedDiff(XMLUnit.compareXML(correctTv2Metadata, metadata));
                boolean tv2MetadataIsCorrect = diff.identical();
                assertTrue(tv2MetadataIsCorrect, diff.getAllDifferences().toString());
            }
        }
    }

    @Test
    public void test_fixAllCommercials() throws Exception {

        DOMSCommercialMetadataQuerier domsQuerier = new DOMSCommercialMetadataQuerier(domsClientSpy);

        DOMSCommercialMetadataUpdater domsUpdater = new DOMSCommercialMetadataUpdater(domsClientSpy);

        CommercialFixer commercialFixer =
                new CommercialFixer(domsQuerier, domsUpdater, ids);



        commercialFixer.fixAllCommercials();



        // Verify that DOMS is updated with correct metadata.
        for (int i = 0; i < NUMBER_OF_EACH_TEST_COMMERCIAL; i++) {
            String cinemaMetadataId = ids.get(i);
            String tv2MetadataId = ids.get(i+NUMBER_OF_EACH_TEST_COMMERCIAL);
            String cinemaMetadata = domsClientSpy.getDatastreamContents(cinemaMetadataId);
            String tv2Metadata = domsClientSpy.getDatastreamContents(tv2MetadataId);

            DetailedDiff diff = new DetailedDiff(XMLUnit.compareXML(correctCinemaMetadata, cinemaMetadata));
            boolean cinemaMetadataIsCorrect = diff.identical();
            assertTrue(cinemaMetadataIsCorrect, diff.getAllDifferences().toString());

            diff = new DetailedDiff(XMLUnit.compareXML(correctTv2Metadata, tv2Metadata));
            boolean tv2MetadataIsCorrect = diff.identical();
            assertTrue(tv2MetadataIsCorrect, diff.getAllDifferences().toString());
        }
    }

    @Test
    public void test_fixAllCommercials_whenObjectsAreNotActive() {
        // Change object states to Deleted.
        ids.forEach(id -> deleteObject(domsClientSpy, id));

        DOMSCommercialMetadataQuerier domsQuerier = new DOMSCommercialMetadataQuerier(domsClientSpy);

        DOMSCommercialMetadataUpdater domsUpdater = new DOMSCommercialMetadataUpdater(domsClientSpy);

        CommercialFixer commercialFixer =
                new CommercialFixer(domsQuerier, domsUpdater, ids);



        commercialFixer.fixAllCommercials();



        for (String id : ids) {
            verify(stdoutMock).println(id + "\tObject has nonactive state: D");
        }

    }

    @Test
    public void test_fixAllCommercials_whenObjectsCannotBeRetrieved() throws Exception {
        doThrow(new MethodFailedException("", "")).when(domsClientSpy).getDatastreamContents(anyString());

        DOMSCommercialMetadataQuerier domsQuerier = new DOMSCommercialMetadataQuerier(domsClientSpy);

        DOMSCommercialMetadataUpdater domsUpdaterMock = mock(DOMSCommercialMetadataUpdater.class);

        CommercialFixer commercialFixer =
                new CommercialFixer(domsQuerier, domsUpdaterMock, ids);



        commercialFixer.fixAllCommercials();



        for (String id : ids) {
            verify(stdoutMock).println(id + "\tFailed to retrieve object.");
        }

        verifyNoMoreInteractions(domsUpdaterMock);

    }

    @Test
    public void test_fixAllCommercials_whenObjectsCannotBeUpdated() throws Exception {
        doThrow(new MethodFailedException("", "")).when(domsClientSpy).markInProgressObject(anyString());

        DOMSCommercialMetadataQuerier domsQuerier = new DOMSCommercialMetadataQuerier(domsClientSpy);

        DOMSCommercialMetadataUpdater domsUpdater = new DOMSCommercialMetadataUpdater(domsClientSpy);

              CommercialFixer commercialFixer =
                new CommercialFixer(domsQuerier, domsUpdater, ids);



        commercialFixer.fixAllCommercials();



        for (String id : ids) {
            verify(stdoutMock).println(id + "\tFailed to update object.");
        }

    }

    private String createObject(DOMSClient client, String contents){
        CentralWebservice centralWebservice = client.getCentralWebservice();
        try {
            String id = centralWebservice.newObject("doms:Template_Reklamefilm", Arrays.asList(), "Creating test object for doms-reklame-metadata-fixer");
            centralWebservice.modifyDatastream(id, "PBCORE", contents, "Creating test object for doms-reklame-metadata-fixer");
            Relation rel = new Relation();
            rel.setSubject(id);
            rel.setObject("doms:Template_ReklameFile");
            rel.setPredicate("http://doms.statsbiblioteket.dk/relations/default/0/1/#hasFile");
            centralWebservice.addRelation(id, rel, "Adding test relation for doms-reklame-metadata-fixer");
            centralWebservice.markPublishedObject(
                    Arrays.asList(id), "Done creating test object for doms-reklame-metadata-fixer");
            return id;

        } catch (InvalidCredentialsException | InvalidResourceException | MethodFailedException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteObject(DOMSClient client, String... id){
        CentralWebservice centralWebservice = client.getCentralWebservice();
        try {
            centralWebservice.deleteObject(Arrays.asList(id), "Deleting test object for doms-reklame-metadata-fixer");

        } catch (InvalidCredentialsException | InvalidResourceException | MethodFailedException e) {
            throw new RuntimeException(e);
        }
    }

}
