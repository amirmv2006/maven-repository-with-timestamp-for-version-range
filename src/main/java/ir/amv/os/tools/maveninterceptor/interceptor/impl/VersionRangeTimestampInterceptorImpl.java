package ir.amv.os.tools.maveninterceptor.interceptor.impl;

import ir.amv.os.tools.maveninterceptor.interceptor.IRequestInterceptor;
import ir.amv.os.tools.maveninterceptor.interceptor.RequestInterceptContext;
import ir.amv.os.tools.maveninterceptor.interceptor.exc.RequestInterceptException;
import ir.amv.os.tools.maveninterceptor.interceptor.impl.artifactory.IArtifactoryApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * @author Amir
 */
@Component
public class VersionRangeTimestampInterceptorImpl
        implements IRequestInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(VersionRangeTimestampInterceptorImpl.class);

    public static final String N_A = "N/A";

    // for each threshold we have different maven-metadata, so the key is combination of metadata path and threshold
    // date
    private WaitASecondMap<String, String> sha1LiterallyHashMap = new WaitASecondMap<>();
    private WaitASecondMap<String, String> md5LiterallyHashMap = new WaitASecondMap<>();
    @Autowired
    private IArtifactoryApi artifactoryApi;

    @Override
    public int rank() {
        return 1;
    }

    @Override
    public void intercept(final RequestInterceptContext context) throws RequestInterceptException {
        String requestURI = context.getRequestURI();
        if (requestURI.endsWith("maven-metadata.xml")) {
            try {
                LOGGER.info("got request for maven-metadata: '{}', threshold: '{}'", requestURI, context
                        .getThreshold());
                Map<String, Date> versionReleaseMap = getReleaseMapFromArtifactoryApi(requestURI);
                LOGGER.info("version release map: '{}', threshold: '{}'", versionReleaseMap, context.getThreshold());
                String[] hashes = transformMetaData(context.getInputStream().apply(null), context.getOutputStream(),
                        context.getThreshold(), versionReleaseMap, requestURI);
                LOGGER.info("transform finished");
                md5LiterallyHashMap.put(requestURI + ".md5" + context.getThreshold().getTime(), hashes[0]);
                sha1LiterallyHashMap.put(requestURI + ".sha1" + context.getThreshold().getTime(), hashes[1]);
                context.setFinished(true);
            } catch (Exception e) {
                // release the locks
                md5LiterallyHashMap.put(requestURI + ".md5" + context.getThreshold().getTime(), N_A);
                sha1LiterallyHashMap.put(requestURI + ".sha1" + context.getThreshold().getTime(), N_A);
                throw new RequestInterceptException("Exception", e);
            }
        } else {
            handleHash(context, "sha1", sha1LiterallyHashMap);
            handleHash(context, "md5", md5LiterallyHashMap);
        }
    }

    private Map<String, Date> getReleaseMapFromArtifactoryApi(final String requestURI) throws IOException, ParseException {
        String artifactPath = requestURI.substring(0, requestURI.length() - "maven-metadata.xml".length());
        return artifactoryApi.getReleaseDatesFor(artifactPath);
    }

    private void handleHash(final RequestInterceptContext context, final String algorithm,
                            final WaitASecondMap<String, String> hashMap) throws RequestInterceptException {
        if (context.getRequestURI().endsWith("maven-metadata.xml." + algorithm)) {
            LOGGER.info("got request for maven-metadata hash: '{}', algorithm: '{}', threshold: '{}'", context
                    .getRequestURI(), algorithm, context.getThreshold());
            try {
                String key = context.getRequestURI() + context.getThreshold().getTime();
                String hash = hashMap.iWantThisKeyAndIllWait(key);
                if (!hash.equals(N_A)) {
                    new BufferedWriter(new OutputStreamWriter(context.getOutputStream())).write(hash);
                    context.setFinished(true);
                } else {
                    LOGGER.error("problem with request '{}', not returning hash", context.getRequestURI());
                }
                hashMap.remove(key);
            } catch (Exception e) {
                LOGGER.error("problem with request", e);
                throw new RequestInterceptException("Exception", e);
            }
        }
    }

    private String[] transformMetaData(InputStream is, OutputStream os, final Date thresholdDate, Map<String, Date>
            versionReleaseMap, final String requestURI) throws
            ParserConfigurationException, SAXException, IOException, XPathExpressionException, TransformerException, NoSuchAlgorithmException, ParseException {
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder builder =
                factory.newDocumentBuilder();
        Document document = builder.parse(is);
        XPath xPath = XPathFactory.newInstance().newXPath();
        Node evaluate = (Node) xPath.compile("/metadata/versioning/versions").evaluate(document, XPathConstants.NODE);
        if (evaluate != null) {
            NodeList childNodes = evaluate.getChildNodes();
            String latestVersion = "UNKNOWN";
            String latestReleaseVersion = "UNKNOWN";
            boolean removeFromNowOn = false;
            for (int i = 0; i < childNodes.getLength(); i++) {
                if (i == 0) {
                    continue; // skip the first one so that we will have at least one version left! otherwise things
                    // will go south
                }
                Node item = childNodes.item(i);
                if (item.getNodeName().equals("version")) {
                    String version = item.getTextContent();
                    Date releaseDate = versionReleaseMap.get(version);
                    if (removeFromNowOn || (releaseDate != null && releaseDate.after(thresholdDate))) {
                        if (removeFromNowOn) {
                            LOGGER.debug("removing version '{}', same reason as previous version removal!");
                        } else {
                            LOGGER.debug("removing version '{}', release date '{}' is after threshold '{}'", version,
                                    releaseDate, thresholdDate);
                        }
                        removeFromNowOn = true;
                        evaluate.removeChild(item);
                    } else {
                        if (!version.endsWith("-SNAPSHOT")) {
                            latestReleaseVersion = version;
                        }
                        latestVersion = version;
                    }
                }
            }
            evaluate = (Node) xPath.compile("/metadata/version").evaluate(document, XPathConstants.NODE);
            if (evaluate != null) {
                evaluate.setTextContent(latestVersion);
            }
            evaluate = (Node) xPath.compile("/metadata/versioning/latest").evaluate(document, XPathConstants.NODE);
            if (evaluate != null) {
                evaluate.setTextContent(latestVersion);
            }
            evaluate = (Node) xPath.compile("/metadata/versioning/release").evaluate(document, XPathConstants.NODE);
            if (evaluate != null) {
                evaluate.setTextContent(latestReleaseVersion);
            }
            evaluate = (Node) xPath.compile("/metadata/versioning/lastUpdated").evaluate(document, XPathConstants.NODE);
            if (evaluate != null) {
                Date lastReleaseDate = versionReleaseMap.get(latestVersion);
                evaluate.setTextContent(new SimpleDateFormat("yyyyMMddHHmmss").format(lastReleaseDate));
            }
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            DigestListOutputStream digestOS = new DigestListOutputStream(os, Arrays.asList(sha1Digest, md5Digest));
            transformer.transform(new DOMSource(document), new StreamResult(new OutputStreamWriter(digestOS)));
            HexBinaryAdapter hexBinaryAdapter = new HexBinaryAdapter();
            return new String[]{hexBinaryAdapter.marshal(md5Digest.digest()), hexBinaryAdapter.marshal(sha1Digest.digest())};
        }
        return new String[]{N_A, N_A};
    }
}
