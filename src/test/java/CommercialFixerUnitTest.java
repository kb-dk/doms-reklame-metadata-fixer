import dk.statsbiblioteket.reklamefixer.CommercialFixer;
import dk.statsbiblioteket.reklamefixer.CommercialMetadata;
import dk.statsbiblioteket.reklamefixer.doms.DOMSCommercialMetadataQuerier;
import dk.statsbiblioteket.reklamefixer.doms.DOMSCommercialMetadataUpdater;
import dk.statsbiblioteket.util.Strings;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.XMLUnit;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CommercialFixerUnitTest {

    private DOMSCommercialMetadataQuerier domsQuerierMock;
    private DOMSCommercialMetadataUpdater domsUpdaterMock;
    private CommercialFixer commercialFixer;
    private String uuid1;
    private String uuid2;
    private List<String> commercialUuids;

    @BeforeMethod
    public void setup() {
        uuid1 = "uuid:1";
        uuid2 = "uuid:2";
        commercialUuids = Arrays.asList(uuid1, uuid2);

        domsQuerierMock = mock(DOMSCommercialMetadataQuerier.class);
        domsUpdaterMock = mock(DOMSCommercialMetadataUpdater.class);

        commercialFixer =
                new CommercialFixer(domsQuerierMock, domsUpdaterMock, commercialUuids);


        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
    }

    @Test
    public void test_fixAllCommercials_forCinemaMetadataExamples() throws Exception {

        String metadataString = Strings.flush(getClass().getResourceAsStream("/cinema-metadata-example.xml"));
        CommercialMetadata metadata1 = new CommercialMetadata(uuid1, metadataString);
        CommercialMetadata metadata2 = new CommercialMetadata(uuid2, metadataString);

        when(domsQuerierMock.retrieveMetadata(any())).thenReturn(Arrays.asList(metadata1, metadata2));



        commercialFixer.fixAllCommercials();



        verify(domsQuerierMock).retrieveMetadata(commercialUuids);

        // Verify that DOMS is updated with correct metadata.
        ArgumentCaptor<List<CommercialMetadata>> metadataCaptor = ArgumentCaptor.forClass((Class)List.class);

        verify(domsUpdaterMock).updateMetadata(metadataCaptor.capture());

        List<CommercialMetadata> updatedCommercials = metadataCaptor.getValue();
        assertEquals(updatedCommercials.size(), 2);

        String correctCinemaMetadata = Strings.flush(getClass().getResourceAsStream("updated-cinema-metadata-example.xml"));

        for (int i = 0; i < 2; i++) {
            String cinemaMetadata = updatedCommercials.get(i).getMetadata();
            DetailedDiff diff = new DetailedDiff(XMLUnit.compareXML(correctCinemaMetadata, cinemaMetadata));
            boolean cinemaMetadataIsCorrect = diff.identical();
            assertTrue(cinemaMetadataIsCorrect, diff.getAllDifferences().toString());
        }
    }

    @Test
    public void test_fixAllCommercials_forTv2MetadataExamples() throws Exception {

        String metadataString = Strings.flush(getClass().getResourceAsStream("/tv2-metadata-example.xml"));
        CommercialMetadata metadata1 = new CommercialMetadata(uuid1, metadataString);
        CommercialMetadata metadata2 = new CommercialMetadata(uuid2, metadataString);

        when(domsQuerierMock.retrieveMetadata(any())).thenReturn(Arrays.asList(metadata1, metadata2));



        commercialFixer.fixAllCommercials();



        verify(domsQuerierMock).retrieveMetadata(commercialUuids);

        // Verify that DOMS is updated with correct metadata.
        ArgumentCaptor<List<CommercialMetadata>> metadataCaptor = ArgumentCaptor.forClass((Class)List.class);

        verify(domsUpdaterMock).updateMetadata(metadataCaptor.capture());

        List<CommercialMetadata> updatedCommercials = metadataCaptor.getValue();
        assertEquals(updatedCommercials.size(), 2);

        String correctTv2Metadata = Strings.flush(getClass().getResourceAsStream("updated-tv2-metadata-example.xml"));

        for (int i = 0; i < 2; i++) {
            String tv2Metadata = updatedCommercials.get(i).getMetadata();
            DetailedDiff diff = new DetailedDiff(XMLUnit.compareXML(correctTv2Metadata, tv2Metadata));
            boolean tv2MetadataIsCorrect = diff.identical();
            assertTrue(tv2MetadataIsCorrect, diff.getAllDifferences().toString());
        }
    }

    @Test
    public void test_fixAllCommercials_forUpdatedMetadataExamples() {

        InputStream cinemaMetadataStream = getClass().getResourceAsStream("/updated-cinema-metadata-example.xml");
        CommercialMetadata cinemaMetadata = new CommercialMetadata(uuid1, cinemaMetadataStream);

        InputStream tv2MetadataStream = getClass().getResourceAsStream("/updated-tv2-metadata-example.xml");
        CommercialMetadata tv2Metadata = new CommercialMetadata(uuid2, tv2MetadataStream);

        when(domsQuerierMock.retrieveMetadata(any())).thenReturn(Arrays.asList(cinemaMetadata, tv2Metadata));



        commercialFixer.fixAllCommercials();



        verify(domsQuerierMock).retrieveMetadata(commercialUuids);
        // DOMS should not be updated when metadata is already correct.
        verifyNoMoreInteractions(domsUpdaterMock);
    }

}
