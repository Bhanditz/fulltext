/*
 * Copyright 2007-2018 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 *
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */

package eu.europeana.fulltext.loader.service;

import com.ctc.wstx.evt.WDTD;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.entity.Target;
import eu.europeana.fulltext.loader.config.LoaderSettings;
import eu.europeana.fulltext.loader.exception.ArchiveReadException;
import eu.europeana.fulltext.loader.exception.ConfigurationException;
import eu.europeana.fulltext.loader.exception.DuplicateDefinitionException;
import eu.europeana.fulltext.loader.exception.IllegalValueException;
import eu.europeana.fulltext.loader.exception.LoaderException;
import eu.europeana.fulltext.loader.exception.MissingDataException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Service for parsing fulltext xml files
 * Originally created by luthien on 18/07/2018 with SAX parser
 *
 * Refactored to use faster Stax parser by
 * @author Patrick Ehlert
 * on 18-10-2018
 *
 * Note that during parsing warnings (non-fatal problems) are logged using LogFile.OUT which is prepared in advance to
 * collect parsing output. Fatal errors are thrown exceptions, but we can recover from some of these errors for example,
 * when parsing an individual annotation fails we simply skip that annotation.
 */
@Service
public class XMLParserService {

    private static final Logger LOG = LogManager.getLogger(XMLParserService.class);

    private static final XMLInputFactory inputFactory = XMLInputFactory.newInstance();

    private static final String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String RDF = "RDF";

    private static final String FULLTEXTRESOURCE = "FullTextResource";
    private static final String FULLTEXTRESOURCE_ABOUT = "about";
    private static final String FULLTEXTRESOURCE_VALUE = "value";
    private static final String FULLTEXTRESOURCE_LANGUAGE = "language";

    private static final String ANNOTATION = "Annotation";
    private static final String ANNOTATION_ID = "ID";

    private static final String ANNOTATION_TYPE = "type";
    private static final char   ANNOTATION_TYPE_PAGE = 'P';

    private static final String ANNOTATION_MOTIVATION = "motivatedBy";
    private static final String ANNOTATION_MOTIVATION_TEXT = "resource";

    private static final String ANNOTATION_TARGET = "hasTarget";
    private static final String ANNOTATION_TARGET_RESOURCE = "resource";
    private static final String ANNOTATION_TARGET_XYWHPOS    = "#xywh=";

    private static final String ANNOTATION_HASBODY = "hasBody";
    private static final String ANNOTATION_HASBODY_RESOURCE = "specificResource";
    private static final String ANNOTATION_HASBODY_RESOURCE_VALUE = "about";
    private static final String ANNOTATION_HASBODY_RESOURCE_CHARPOS = "#char=";
    private static final String ANNOTATION_HASBODY_RESOURCE_LANGUAGE = "language";

    // parser configuration
    static {
        inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);
    }

    private LoaderSettings settings;

    public XMLParserService(LoaderSettings settings) {
        this.settings = settings;
    }

    /**
     * Parse an fulltext xml file and return an AnnoPage object that is ready to be stored in the database
     * @param pageId full text page number
     * @param xmlStream xml file input stream
     * @param file name of the xml file (for logging purposes)
     * @return AnnotationPage object
     * @throws LoaderException when there is a fatal error processing this file
     */
    public AnnoPage parse(String pageId, InputStream xmlStream, String file) throws LoaderException {
        return parse(pageId, xmlStream, file, null);
    }

    /**
     * Parse an fulltext xml file and return an AnnoPage object that is ready to be stored in the database
     * @param pageId full text page number
     * @param xmlStream xml file input stream
     * @param file name of the xml file (for logging purposes)
     * @param progressAnnotation keep track of number of processed annotations
     * @return AnnotationPage object
     * @throws LoaderException when there is a fatal error processing this file
     */
    public AnnoPage parse(String pageId, InputStream xmlStream, String file, ProgressLogger progressAnnotation) throws LoaderException {

        AnnoPage result = new AnnoPage();
        result.setPgId(pageId);

        XMLEventReader reader = null;
        try {
            reader = inputFactory.createXMLEventReader(xmlStream);
            while(reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                LOG.debug(getEventDescription(event));
                if (event.isStartElement()) {
                    StartElement se = (StartElement) event;
                    switch (se.getName().getLocalPart()) {
                        case RDF              : break; // simply ignore
                        case FULLTEXTRESOURCE : parseFullTextResource(reader, se, result, file); break;
                        case ANNOTATION       : parseAnnotation(reader, se, result, progressAnnotation, file); break;
                        default: logUnknownElement(file, se);
                    }
                }
            }
        } catch (XMLStreamException e) {
            throw new ArchiveReadException("Error reading file "+file, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    LOG.error("Error closing input stream "+file, e);
                }
            }
        }

        checkAnnoPageComplete(result, file);

        LogFile.OUT.debug("{} - processed OK", file);
        return result;
    }

    /**
     * The edm:FullTextResource element consists of a language and a value element but also has a rdf:about attribute
     * from which we retrieve the resourceId
     */
    private void parseFullTextResource(XMLEventReader reader, StartElement fullTextElement, AnnoPage annoPage, String file)
            throws LoaderException, XMLStreamException {
        // there should only be 1 fullTextResource per file, so no resource should be present yet in the annoPage
        if (annoPage.getRes() != null) {
            throw new DuplicateDefinitionException(file + " - Multiple edm:FullTextResource elements found!");
        }
        Resource newResource = new Resource();
        annoPage.setRes(newResource);

        // get all ids (and set them in both AnnoPage and Resource)
        Attribute a = fullTextElement.getAttributeByName(new QName(RDF_NAMESPACE, FULLTEXTRESOURCE_ABOUT));
        parseFullTextResourceId(a.getValue(), annoPage, file);

        // get language and text
        while (reader.hasNext()) {
            XMLEvent e = reader.nextEvent();
            if (reachedEndElement(e, FULLTEXTRESOURCE)) {
                break;
            } else if (e.isStartElement()) {
                StartElement se = (StartElement) e;
                switch (se.getName().getLocalPart()) {
                    case FULLTEXTRESOURCE_LANGUAGE: newResource.setLang(reader.getElementText()); break;
                    case FULLTEXTRESOURCE_VALUE: newResource.setValue(reader.getElementText()); break;
                    default: logUnknownElement(file, se);
                }
            } else if (!(e instanceof Characters)) {
                logUnknownElement(file, e);
            }
        }

        checkResourceComplete(annoPage.getRes(), file);
    }

    /**
     * Process a fulltext url and separate into different id parts.
     * Expected format of the fulltext url is http://data.europeana.eu/fulltext/<datasetId>/<localId>/<resourceId>
     */
    private void parseFullTextResourceId(String ftResourceUrl, AnnoPage annoPage, String file) throws LoaderException {
        if (ftResourceUrl == null) {
            throw new MissingDataException(file + " - No resource text url was defined");
        } else if (ftResourceUrl.startsWith(settings.getResourceBaseUrl())) {
            String identifiers = StringUtils.removeStartIgnoreCase(ftResourceUrl, settings.getResourceBaseUrl());
            String[] ids = StringUtils.split(identifiers, '/');
            if (ids.length != 3){
                throw new MissingDataException(file + " - Error retrieving ids from text url: "+ftResourceUrl);
            }
            annoPage.setDsId(ids[0]);
            annoPage.setLcId(ids[1]);
            annoPage.getRes().setDsId(ids[0]);
            annoPage.getRes().setLcId(ids[1]);
            annoPage.getRes().setId(ids[2]);
        } else {
            throw new ConfigurationException(file + " - ENTITY text value '" + ftResourceUrl + "' doesn't start with configured" +
                    "resource base url '" + settings.getResourceBaseUrl() + "'");
        }
    }

    /**
     * Processes an oa:Annotation element and adds it to the AnnoPage. Note that if an error occurs we skip the
     * annotation and do not add it to the AnnoPage. We do log all annotations that are skipped
     * @return true if annotation was processed and added to AnnoPage object, otherwise false
     */
    private boolean parseAnnotation(XMLEventReader reader, StartElement annotationElement, AnnoPage annoPage,
                                    ProgressLogger progressAnnotation, String file)
            throws XMLStreamException {
        Annotation anno = new Annotation();
        boolean result = false;
        try {
            parseAnnotationId(annotationElement, anno);
            while (reader.hasNext()) {
                XMLEvent e = reader.nextEvent();
                if (reachedEndElement(e, ANNOTATION)) {
                    break;
                } else if (e.isStartElement()) {
                    StartElement se = (StartElement) e;
                    switch (se.getName().getLocalPart()) {
                        case ANNOTATION_TYPE      : this.parseAnnotationType(reader.getElementText(), anno); break;
                        case ANNOTATION_MOTIVATION:
                            // October 2018: for now there is no need for this 'motivation' information so we skip it
                            //this.parseAnnotationMotivation(se, anno);
                            break;
                        case ANNOTATION_HASBODY   : this.parseAnnotationHasBody(reader, anno, file); break;
                        case ANNOTATION_TARGET    : this.parseAnnotationTarget(se, annoPage, anno); break;
                        default: // do nothing, just skip unknown start elements (e.g. confidence, styledBy)
                    }
                } else {
                    // do nothing, just skip other (end) elements until we get to end of annotation
                }
            }
            result = addAnnotationToAnnoPage(annoPage, anno);
            if (progressAnnotation != null) {
                if (result) {
                    progressAnnotation.addItemOk();
                } else {
                    progressAnnotation.addItemFail();
                }
            }
        } catch (LoaderException e) {
            LogFile.OUT.error("{} - Skipping annotation with id {} because {}", file, anno.getAnId(), e.getMessage());
            if (progressAnnotation != null) {
                progressAnnotation.addItemFail();
            }
        }
        return result;
    }

    /**
     * Only add the annotation to the list of annotations if:
     * 1. The annotation has an annotation type
     * 2. The annotation type is 'W', 'B' or 'L' (i.e. NOT 'P') and has a target
     * 3.    or the annotation type is 'P'
     * Note that if there are no text coordinates, we do save it
     * @return true if a new annotation was added to the list, otherwise false
     */
    private boolean addAnnotationToAnnoPage(AnnoPage annoPage, Annotation anno) throws LoaderException {
        if (anno.getDcType() == Character.MIN_VALUE) {
            throw new MissingDataException("no annotation type defined");
        }
        if (anno.getDcType() != ANNOTATION_TYPE_PAGE && (anno.getTgs() == null || anno.getTgs().isEmpty())) {
            throw new MissingDataException("no annotation target defined");
        }

        if (annoPage.getAns() == null) {
            annoPage.setAns(new ArrayList<>());
        }
        return annoPage.getAns().add(anno);
    }

    /**
     * The oa:Annotation element has an 'rdf:ID' attribute. ID values start with a slash character which we filter out
     */
    private void parseAnnotationId(StartElement annotationElement, Annotation anno) throws LoaderException {
        Attribute att = annotationElement.getAttributeByName(new QName(RDF_NAMESPACE, ANNOTATION_ID));
        if (att == null) {
            throw new MissingDataException("no annotation id found");
        }
        String annoId = att.getValue();
        if (annoId.startsWith("/")) {
            anno.setAnId(annoId.substring(1, annoId.length()));
        } else {
            anno.setAnId(annoId);
        }
    }

    /**
     * dc:type is a required field of an annotation
     * We only save the first letter of the type (to save disk space)
     */
    private void parseAnnotationType(String typeValue, Annotation anno) throws LoaderException {
        if (StringUtils.isEmpty(typeValue)) {
            throw new MissingDataException("no annotation type found for annotation " + anno.getAnId());
        }
        anno.setDcType(typeValue.toUpperCase(Locale.GERMANY).charAt(0));
    }

    /**
     * oa:MotivatedBy is an optional field of an annotation
     */
    private void parseAnnotationMotivation(StartElement motivationElement, Annotation anno) {
        Attribute att = motivationElement.getAttributeByName(new QName(RDF_NAMESPACE, ANNOTATION_MOTIVATION_TEXT));
        if (att != null) {
            anno.setMotiv(att.getValue());
        }
    }

    /**
     * The oa:hasBody element should contain a oa:SpecificResource which holds the start and end coordinates of the text
     * of an annotation
     */
    private void parseAnnotationHasBody(XMLEventReader reader, Annotation anno, String file)
            throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent e = reader.nextEvent();
            if (reachedEndElement(e, ANNOTATION_HASBODY)) {
                break;
            } else if (e.isStartElement()) {
                StartElement se = (StartElement) e;
                if (ANNOTATION_HASBODY_RESOURCE.equalsIgnoreCase(se.getName().getLocalPart())) {
                   parseAnnotationTextCoordinates(se, anno, file);
                } else if (ANNOTATION_HASBODY_RESOURCE_LANGUAGE.equalsIgnoreCase(se.getName().getLocalPart())) {
                    parseAnnotationTextLanguage(reader.getElementText(), anno);
                } else {
                   // we simply ignore unknown elements here like 'hasSource' and 'styleClass'
                }
            }
        }
    }

    /**
     * Parse the text coordinates at the end attribute value of the  oa:hasBody/oa:specificResource tag
     * Note that we rely on the calling method to go the the end of the 'oa:hasBody' section when we're done
     */
    private void parseAnnotationTextCoordinates(StartElement specificRsElement, Annotation anno, String file) {
        Attribute att = specificRsElement.getAttributeByName(new QName(RDF_NAMESPACE, ANNOTATION_HASBODY_RESOURCE_VALUE));
        if (att == null || StringUtils.isEmpty(att.getValue())) {
            LogFile.OUT.warn(file + " - No specific resource text defined");
        } else {
            String[] urlAndCoordinates = att.getValue().split(ANNOTATION_HASBODY_RESOURCE_CHARPOS);
            if (urlAndCoordinates.length == 1) {
                LogFile.OUT.warn(file + " - No " + ANNOTATION_HASBODY_RESOURCE_CHARPOS + " defined in resource text " +att.getValue());
            } else {
                String[] fromTo = urlAndCoordinates[1].split(",");
                parseFromToInteger(fromTo[0], FromTo.FROM, anno, file);
                parseFromToInteger(fromTo[1], FromTo.TO, anno, file);
            }
        }
    }

    private enum FromTo { FROM, TO }
    private void parseFromToInteger(String value, FromTo fromTo, Annotation anno, String file) {
        if (StringUtils.isEmpty(value)) {
            LogFile.OUT.warn(file + " - Empty resource text " + fromTo + " value");
        } else {
            try {
                if (FromTo.FROM.equals(fromTo)) {
                    anno.setFrom(Integer.valueOf(value));
                } else if (FromTo.TO.equals(fromTo)) {
                    anno.setTo(Integer.valueOf(value));
                }
            } catch (NumberFormatException nfe) {
                LogFile.OUT.error(file + " - Resource text " + fromTo +" value '" + value +
                        "' is not an integer");
            }
        }
    }

    /**
     * dc:language is an optional element of an annotation
     */
    private void parseAnnotationTextLanguage(String language, Annotation anno) {
        if (StringUtils.isNotEmpty(language)) {
            anno.setLang(language);
        }
    }

    /**
     * The hasTarget tag should have an attribute with as value the image url and coordinates.
     * Note that we only need this for
     * Also coordinates and image url are required, hence the validity checks
     */
    private void parseAnnotationTarget(StartElement targetElement, AnnoPage annoPage,
                                       Annotation anno) throws LoaderException {
        Attribute att = targetElement.getAttributeByName(new QName(RDF_NAMESPACE, ANNOTATION_TARGET_RESOURCE));
        if (att == null || StringUtils.isEmpty(att.getValue()) ) {
            throw new MissingDataException("no annotation target url defined");
        }

        // parse the target url
        String[] urlAndCoordinates = att.getValue().split(ANNOTATION_TARGET_XYWHPOS);
        // for Page annotations the target is optional, for all others it is required
        if (anno.getDcType() != ANNOTATION_TYPE_PAGE && urlAndCoordinates.length == 1) {
            throw new MissingDataException("no " + ANNOTATION_TARGET_XYWHPOS + " defined in target url " + att.getValue());
        }

        // we only need to set the imageUrl once in the AnnoPage object, all subsequent annotations will have the same url
        if (annoPage.getTgtId() == null) {
            annoPage.setTgtId(urlAndCoordinates[0]);
        }

        // set target
        if (urlAndCoordinates.length > 1) {
            Target t = createTarget(urlAndCoordinates[1]);
            if (anno.getTgs() == null) {
                anno.setTgs(new ArrayList<>());
            }
            anno.getTgs().add(t);
        }
    }

    private Target createTarget(String coordinates) throws LoaderException {
        String[] separatedCoordinates = coordinates.split(",");
        if (separatedCoordinates.length != 4) {
            throw new IllegalValueException("target '" + coordinates +
                    "' doesn't have 4 integers separated with a comma");
        }
        try {
            return new Target(Integer.valueOf(separatedCoordinates[0]),
                              Integer.valueOf(separatedCoordinates[1]),
                              Integer.valueOf(separatedCoordinates[2]),
                              Integer.valueOf(separatedCoordinates[3]));
        } catch (NumberFormatException nfe) {
            throw new IllegalValueException("target '" + coordinates +
                    "' doesn't have 4 integers separated with a comma");
        }
    }

    private void checkAnnoPageComplete(AnnoPage annoPage, String file) throws LoaderException {
        if (StringUtils.isEmpty(annoPage.getDsId())) {
            throw new MissingDataException(file + " - No annotation page dataset id defined");
        }
        if (StringUtils.isEmpty(annoPage.getLcId())) {
            throw new MissingDataException(file + " - No annotation page local id defined");
        }
        if (StringUtils.isEmpty(annoPage.getPgId())) {
            throw new MissingDataException(file + " - No annotation page id defined");
        }
        if (StringUtils.isEmpty(annoPage.getTgtId())) {
            throw new MissingDataException(file + " - No annotation page target id defined");
        }
        if (annoPage.getAns() == null || annoPage.getAns().isEmpty()) {
            throw new MissingDataException(file + " - Annotation page doesn't contain any annotations");
        }
        if (annoPage.getModified() == null) {
            throw new MissingDataException(file + " - No last modified date set");
        }
    }

    /**
     * Check if the minimal required resource information is present
     */
    private void checkResourceComplete(Resource res, String file) throws LoaderException {
        if (StringUtils.isEmpty(res.getDsId())) {
            throw new MissingDataException(file + " - No resource dataset id defined");
        }
        if (StringUtils.isEmpty(res.getLcId())) {
            throw new MissingDataException(file + " - No resource local id defined");
        }
        if (StringUtils.isEmpty(res.getId())) {
            throw new MissingDataException(file + " - No resource id defined");
        }
        // text and language are optional
    }

    private boolean reachedEndElement(XMLEvent e, String elementName) {
        return e.isEndElement() && elementName.equals(((EndElement) e).getName().getLocalPart());
    }

    private void logUnknownElement(String file, XMLEvent event) {
        // for now just log to output
        LOG.info("{} - Unknown xml event {}", file, getEventDescription(event));
    }

    /**
     * For debugging purposes
     */
    private String getEventDescription(XMLEvent e) {
        if (e.isAttribute()) {
            Attribute a = (Attribute) e;
            return "Attribute" + a.getName() + ", value " + a.getValue();
        } else if (e.isStartElement()) {
            return "StartElement " + ((StartElement) e).getName();
        } else if (e.isEndElement()) {
            return "EndElement " + ((EndElement) e).getName();
        } else if (e.isCharacters()) {
            Characters c = (Characters) e;
            if (c.isIgnorableWhiteSpace()) {
                return "Ignorable whitespace characters";
            } else {
                return "Characters '" + c.getData() + "'";
            }
        } else if (e.isStartDocument()) {
            return "Start of document";
        } else if (e.isEndDocument()) {
            return "End of document";
        } else if (e.isEntityReference()) {
            EntityReference ef = (EntityReference) e;
            return "Entity reference " + ef.getName() + ", value " + ef.getDeclaration();
        } else if (e.isProcessingInstruction()) {
            ProcessingInstruction pi = (ProcessingInstruction) e;
            return "Processing instruction target " + pi.getTarget() + ", data " + pi.getData();
        } else if (e.isNamespace()) {
            return "Namespace " + ((Namespace) e).getName();
        } else if (e instanceof WDTD) {
            WDTD wdtd = (WDTD) e;
            StringBuilder s = new StringBuilder("WDTD ").append(wdtd.getRootName());
//            if (!wdtd.getEntities().isEmpty()) {
//                s.append(", entities: ");
//                for (EntityDeclaration ed : wdtd.getEntities()) {
//                    s.append(ed.getName()).append(" ");
//                }
//            }
//            if (!wdtd.getNotations().isEmpty()) {
//                s.append(", notations: ");
//                for (NotationDeclaration nd : wdtd.getNotations()) {
//                    s.append(nd.getName()).append(" ");
//                }
//            }
            return s.toString();
        }
        return e.toString();
    }
}