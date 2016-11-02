package dk.statsbiblioteket.reklamefixer.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * Configuration read by property file.
 */
public class PropertyBasedRegistrarConfiguration {

    private static final String USER_NAME_KEY = "doms-reklamefixer.userName";
    private static final String PASSWORD_KEY = "doms-reklamefixer.password";
    private static final String DOMS_WS_API_ENDPOINT_KEY = "doms-reklamefixer.domsWSAPIEndpoint";
    private static final String DOMS_WS_API_ENDPOINT_TIMEOUT_KEY = "doms-reklamefixer.domsWSAPIEndpointTimeoutInMillis";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Properties properties;

    public PropertyBasedRegistrarConfiguration(File propertiesFile) {
        try {
            loadProperties(new FileInputStream(propertiesFile));
        } catch (FileNotFoundException e) {
            throw new InitializationFailedException("Properties file not found (" + propertiesFile.getAbsolutePath() + ")", e);
        }
        log.debug("Read properties for '" + propertiesFile.getAbsolutePath() + "'");
    }

    public PropertyBasedRegistrarConfiguration(InputStream properties) {
        loadProperties(properties);
    }

    private void loadProperties(InputStream inputStream) {
        this.properties = new Properties();
        try {
            this.properties.load(inputStream);
        } catch (IOException e) {
            throw new InitializationFailedException("Unable to load properties from", e);
        }
        log.debug("Read configuration properties");
    }

    public String getUsername() {
        return properties.getProperty(USER_NAME_KEY);
    }

    public String getPassword() {
        return properties.getProperty(PASSWORD_KEY);
    }

    public URL getDomsWSAPIEndpoint() {
        try {
            return new URL(properties.getProperty(DOMS_WS_API_ENDPOINT_KEY));
        } catch (MalformedURLException e) {
            throw new InitializationFailedException("Invalid property for '" + DOMS_WS_API_ENDPOINT_KEY + "'", e);
        }
    }

    public int getDomsWSAPIEndpointTimeout() {
        try {
            return Integer.parseInt(properties.getProperty(DOMS_WS_API_ENDPOINT_TIMEOUT_KEY));
        } catch (NumberFormatException e) {
            throw new InitializationFailedException("Invalid property for '" + DOMS_WS_API_ENDPOINT_TIMEOUT_KEY + "'", e);
        }
    }
}
