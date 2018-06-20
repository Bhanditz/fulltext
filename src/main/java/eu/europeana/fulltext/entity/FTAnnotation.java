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

package eu.europeana.fulltext.entity;

import org.mongodb.morphia.annotations.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by luthien on 31/05/2018.
 */
@Document(collection = "fulltextAnnotation")
public class FTAnnotation {

    @Id
    private String      id;             // annotationId
    private String      dcType;         // type = "AnnotationV2" and "SpecificResource" are implicitly added, not stored
    private String      motivation;
    private String      language;       // optional
    private Integer     textStart;
    private Integer     textEnd;
    private Integer     targetX;
    private Integer     targetY;
    private Integer     targetW;
    private Integer     targetH;
    private String      pageId;         // link to parent page


    // No args Constructor

    public FTAnnotation(String id, String dcType, String motivation, String language,
                        Integer textStart, Integer textEnd, Integer targetX,
                        Integer targetY, Integer targetW, Integer targetH, String pageId) {
        this.id         = id;
        this.dcType     = dcType;
        this.motivation = motivation;
        this.language   = language;
        this.textStart  = textStart;
        this.textEnd    = textEnd;
        this.targetX    = targetX;
        this.targetY    = targetY;
        this.targetW    = targetW;
        this.targetH    = targetH;
        this.pageId     = pageId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDcType() {
        return dcType;
    }

    public void setDcType(String dcType) {
        this.dcType = dcType;
    }

    public String getMotivation() {
        return motivation;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getTextStart() {
        return textStart;
    }

    public void setTextStart(Integer textStart) {
        this.textStart = textStart;
    }

    public Integer getTextEnd() {
        return textEnd;
    }

    public void setTextEnd(Integer textEnd) {
        this.textEnd = textEnd;
    }

    public Integer getTargetX() {
        return targetX;
    }

    public void setTargetX(Integer targetX) {
        this.targetX = targetX;
    }

    public Integer getTargetY() {
        return targetY;
    }

    public void setTargetY(Integer targetY) {
        this.targetY = targetY;
    }

    public Integer getTargetW() {
        return targetW;
    }

    public void setTargetW(Integer targetW) {
        this.targetW = targetW;
    }

    public Integer getTargetH() {
        return targetH;
    }

    public void setTargetH(Integer targetH) {
        this.targetH = targetH;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }
}