package dk.statsbiblioteket.reklamefixer;

import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.transform.TransformerException;
import java.io.InputStream;

public class CommercialMetadata {

    private static final String NAMESPACE = "http://www.pbcore.org/PBCore/PBCoreNamespace.html";
    private static final XPathSelector NAMESPACE_SELECTOR = DOM.createXPathSelector(
            "namespace", NAMESPACE
    );
    private static final String XPATH_DOCUMENT = "/namespace:PBCoreDescriptionDocument";
    private static final String XPATH_ASSET_TYPE = XPATH_DOCUMENT + "/namespace:pbcoreAssetType";
    private static final String XPATH_ALTERNATIVE_TITLE =
            XPATH_DOCUMENT + "/namespace:pbcoreTitle/namespace:titleType[text()='alternative']/../namespace:title";
    private static final String XPATH_DESCRIPTION =
            XPATH_DOCUMENT + "/namespace:pbcoreDescription/namespace:description";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String uuid;
    private Document dom;

    public CommercialMetadata(String uuid, String metadata) {
        this.uuid = uuid;
        this.dom = DOM.stringToDOM(metadata, true);
    }

    public CommercialMetadata(String uuid, InputStream metadata) {
        this.uuid = uuid;
        this.dom = DOM.streamToDOM(metadata, true);
    }

    public String getAssetType() {
        return selectNode(XPATH_ASSET_TYPE).getTextContent();
    }

    public String getUuid() {
        return uuid;
    }


    public boolean moveAlternativeTitle() {
        String alternativeTitle = getAndRemoveAlternativeTitle();
        if(alternativeTitle.equals("")){
            log.debug("Alternative title for {} is empty. Nothing is moved.", getUuid());
            return false;
        }
        setDescription(alternativeTitle);
        return true;
    }

    private String getAndRemoveAlternativeTitle() {
        Node titleNode = selectNode(XPATH_ALTERNATIVE_TITLE);
        String result = titleNode.getTextContent();
        titleNode.setTextContent("");

        return result;
    }

    public String getAlternativeTitle() {
        Node titleNode = selectNode(XPATH_ALTERNATIVE_TITLE);
        String result = titleNode.getTextContent();

        return result;
    }

    public String getDescription() {
        Node descriptionNode = selectNode(XPATH_DESCRIPTION);
        return descriptionNode.getTextContent();
    }

    private void setDescription(String text) {
        Node descriptionNode = selectNode(XPATH_DESCRIPTION);
        descriptionNode.setTextContent(text);
    }



    // For Tv2 commercials

    /**
     * Inserts tv2 information into metadata if it does not already exist.
     * @return true if information is added. false if information already exists.
     */
    public boolean insertTv2Info() {
        Node publisherNode = selectNode("/namespace:PBCoreDescriptionDocument/namespace:pbcorePublisher[1]/namespace:publisher[1]");
        if(publisherNode != null && publisherNode.getTextContent().equals("tv2d")){
            log.debug("Publisher node already exists for {}. " +
                    "Assumes that all TV2 info has already been added.", getUuid());
            return false;
        }

        Node documentNode = selectNode(XPATH_DOCUMENT);
        Node successorNode = selectNode("/namespace:PBCoreDescriptionDocument/namespace:pbcoreInstantiation");

        Node pbcorePublisher1 = createPbcorePublisherNode("tv2d", "channel_name");
        Node pbcorePublisher2 = createPbcorePublisherNode("TV 2", "kanalnavn");

        documentNode.insertBefore(pbcorePublisher2, successorNode);
        documentNode.insertBefore(pbcorePublisher1, pbcorePublisher2);

        return true;
    }

    private Node createPbcorePublisherNode(String publisherText, String publisherRoleText) {
        Node pbcorePublisher = dom.createElementNS(NAMESPACE, "pbcorePublisher");
        Node publisher = dom.createElementNS(NAMESPACE, "publisher");
        Node publisherRole = dom.createElementNS(NAMESPACE, "publisherRole");

        publisher.setTextContent(publisherText);
        publisherRole.setTextContent(publisherRoleText);
        pbcorePublisher.appendChild(publisher);
        pbcorePublisher.appendChild(publisherRole);
        return pbcorePublisher;
    }




    public Node selectNode(String xpath) {
        return NAMESPACE_SELECTOR.selectNode(dom, xpath);
    }

    public String getMetadata() {
        try {
            return DOM.domToString(dom);
        } catch (TransformerException e) {
            throw new RuntimeException("Unexpected error when generating metadata XML", e);
        }
    }
}
