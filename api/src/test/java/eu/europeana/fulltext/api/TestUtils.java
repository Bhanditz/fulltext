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

package eu.europeana.fulltext.api;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.entity.Target;
import eu.europeana.fulltext.api.model.v2.AnnotationBodyV2;
import eu.europeana.fulltext.api.model.v2.AnnotationFullBodyV2;
import eu.europeana.fulltext.api.model.v2.AnnotationPageV2;
import eu.europeana.fulltext.api.model.v2.AnnotationV2;
import eu.europeana.fulltext.api.model.v3.AnnotationBodyV3;
import eu.europeana.fulltext.api.model.v3.AnnotationPageV3;
import eu.europeana.fulltext.api.model.v3.AnnotationV3;
import eu.europeana.fulltext.api.service.CacheUtils;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;

import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_EDM_JSONLD;
import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_IIIF_V2;
import static eu.europeana.fulltext.api.config.FTDefinitions.MEDIA_TYPE_IIIF_V3;

/**
 * Created by luthien on 26/09/2018.
 */

class TestUtils {

    private static final String RESOURCEBASEURL     = "http://data.europeana.eu/fulltext/";
    private static final String IIIFBASEURL         = "https://iiif.europeana.eu/presentation/";
    private static final String ANNOTATIONBASEURL   = "https://data.europeana.eu/annotation/";
    

    private static final String DS_ID   = "ds1";
    private static final String LCL_ID  = "lc1";
    private static final String MOTIV_2 = "sc:painting";
    private static final String MOTIV_3 = "transcribing";

    static AnnotationBodyV2 anbv2_1;
    static AnnotationBodyV2 anbv2_2;
    static AnnotationBodyV2 anbv2_3;
    static AnnotationV2     annv2_1;
    static AnnotationV2     annv2_2;
    static AnnotationV2     annv2_3;
    static AnnotationV2[]   ansv2_1;
    static AnnotationPageV2 anpv2_1;

    static AnnotationBodyV3 anbv3_1;
    static AnnotationBodyV3 anbv3_2;
    static AnnotationBodyV3 anbv3_3;
    static AnnotationV3     annv3_1;
    static AnnotationV3     annv3_2;
    static AnnotationV3     annv3_3;
    static AnnotationV3[]   ansv3_1;
    static AnnotationPageV3 anpv3_1;

    static Resource   res_1;
    static Target     tgt_1;
    static Target     tgt_2;
    static Target     tgt_3;
    static Target     tgt_4;
    static Annotation ann_1;
    static Annotation ann_2;
    static Annotation ann_3;
    static AnnoPage   anp_1;

    static Date lastModifiedDate = Date.from(LocalDate.of(2015 , Month.FEBRUARY , 23)
                                                      .atStartOfDay(ZoneId.systemDefault()).toInstant());
    static Date theDayBefore = Date.from(LocalDate.of(2015 , Month.FEBRUARY , 22)
                                                      .atStartOfDay(ZoneId.systemDefault()).toInstant());

    static{
        // default prepare the AnnotationPages
        prepareAnnotationPages();

        // build example AnnoPage bean with all containing entities, to mock the Repository with
        res_1 = new Resource("ft1", "en", "Wickie willah Koeckebacke!", DS_ID, LCL_ID);
        tgt_1 = new Target(60,100,30,14);
        tgt_2 = new Target(95,102,53,15);
        tgt_3 = new Target(60,96,404,19);
        tgt_4 = new Target(59,138,133,25);
        ann_1 = new Annotation("an1", 'W', 0, 7, Arrays.asList(tgt_1));
        ann_2 = new Annotation("an2", 'W', 9, 18, Arrays.asList(tgt_2), "en");
        ann_3 = new Annotation("an3", 'L', 0, 214, Arrays.asList(tgt_3, tgt_4));
        anp_1 = new AnnoPage(DS_ID, LCL_ID, "pg1", "tg1", res_1);
        anp_1.setAns(Arrays.asList(new Annotation[] {ann_1, ann_2, ann_3}));
        anp_1.setTgtId(getTargetIdBaseUrl("pg1"));
        anp_1.setModified(lastModifiedDate);
    }

    // prepares AnnotationPage entity beans (Annotations WITHOUT context)
    private static void prepareAnnotationPages(){
        prepareAnnotationPageV2();
        prepareAnnotationPageV3();
    }

    public static void prepareAnnotationPageV2(){
        buildAnnotationBodiesV2();
        buildAnnotationsV2(false);
        ansv2_1 = new AnnotationV2[] {annv2_1, annv2_2, annv2_3};
        anpv2_1 = createAnnotationPageV2("pg1", ansv2_1);
    }

    public static void prepareAnnotationPageV3(){
        buildAnnotationBodiesV3();
        buildAnnotationsV3(false);
        ansv3_1 = new AnnotationV3[] {annv3_1, annv3_2, annv3_3};
        anpv3_1 = createAnnotationPageV3("pg1", ansv3_1);
    }

    // prepares Annotations entity beans only (Annotations WITH context)
    public static void prepareAnnotationsV2(){
        buildAnnotationBodiesV2();
        buildAnnotationsV2(true);
    }

    // prepares Annotations entity beans only (Annotations WITH context)
    public static void prepareAnnotationsV3(){
        buildAnnotationBodiesV3();
        buildAnnotationsV3(true);
    }

    private static void buildAnnotationBodiesV2(){
        anbv2_1 = createAnnotationBodyV2("0", "7", "ft1");
        anbv2_2 = createAnnotationFullBodyV2("9", "18", "en", "ft1");
        anbv2_3 = createAnnotationBodyV2("0", "214", "ft1");
    }

    private static void buildAnnotationsV2(boolean includeContext){
        annv2_1 = createAnnotationV2("an1", anbv2_1,
                                     new String[]{getTargetIdUrl("pg1", "60","100","30","14")},
                                     "Word", includeContext);
        annv2_2 = createAnnotationV2("an2", anbv2_2,
                                     new String[]{getTargetIdUrl("pg1", "95","102","53","15")},
                                     "Word", includeContext);
        annv2_3 = createAnnotationV2("an3", anbv2_3,
                                     new String[]{getTargetIdUrl("pg1", "60","96","404","19"),
                                             getTargetIdUrl("pg1", "59","138","133","25")},
                                     "Line", includeContext);
    }

    private static void buildAnnotationBodiesV3(){
        anbv3_1 = createAnnotationBodyV3("0", "7", "ft1");
        anbv3_2 = createAnnotationBodyV3("9", "18", "en", "ft1");
        anbv3_3 = createAnnotationBodyV3("0", "214", "ft1");
    }

    private static void buildAnnotationsV3(boolean includeContext){
        annv3_1 = createAnnotationV3("an1", anbv3_1,
                                     new String[]{getTargetIdUrl("pg1", "60","100","30","14")},
                                     "Word", includeContext);
        annv3_2 = createAnnotationV3("an2", anbv3_2,
                                     new String[]{getTargetIdUrl("pg1", "95","102","53","15")},
                                     "Word", includeContext);
        annv3_3 = createAnnotationV3("an3", anbv3_3,
                                     new String[]{getTargetIdUrl("pg1", "60","96","404","19"),
                                             getTargetIdUrl("pg1", "59","138","133","25")},
                                     "Line", includeContext);
    }

    private static AnnotationPageV2 createAnnotationPageV2(String pageId, AnnotationV2[] resources){
        AnnotationPageV2 anp = new AnnotationPageV2(getAnnopageIdUrl(pageId));
        anp.setResources(resources);
        return anp;
    }

    private static AnnotationBodyV2 createAnnotationBodyV2(String from, String to, String resId){
        return new AnnotationBodyV2(getResourceIdUrl(from, to, resId));
    }

    private static AnnotationFullBodyV2 createAnnotationFullBodyV2(String from, String to, String language, String resId){
        AnnotationFullBodyV2 anb = new AnnotationFullBodyV2(getResourceIdUrl(from, to, resId));
        anb.setFull(getResourceIdBaseUrl(resId));
        anb.setLanguage(language);
        return anb;
    }

    private static AnnotationV2 createAnnotationV2(String annoId, AnnotationBodyV2 resource,
                                                   String[] on, String dcType, boolean includeContext) {
        AnnotationV2 ann = new AnnotationV2(getAnnoIdUrl(annoId));
        if (includeContext) ann.setContext(new String[]{MEDIA_TYPE_IIIF_V2, MEDIA_TYPE_EDM_JSONLD});
        ann.setResource(resource);
        ann.setOn(on);
        ann.setDcType(dcType);
        ann.setMotivation(MOTIV_2);
        return ann;
    }

    private static AnnotationPageV3 createAnnotationPageV3(String pageId, AnnotationV3[] items){
        AnnotationPageV3 anp = new AnnotationPageV3(getAnnopageIdUrl(pageId));
        anp.setItems(items);
        return anp;
    }

    private static AnnotationBodyV3 createAnnotationBodyV3(String from, String to, String resId){
        return new AnnotationBodyV3(getResourceIdUrl(from, to, resId));
    }

    private static AnnotationBodyV3 createAnnotationBodyV3(String from, String to, String language, String resId){
        AnnotationBodyV3 anb = new AnnotationBodyV3(getResourceIdUrl(from, to, resId), "SpecificResource");
        anb.setSource(getResourceIdBaseUrl(resId));
        anb.setLanguage(language);
        return anb;
    }

    private static AnnotationV3 createAnnotationV3(String annoId, AnnotationBodyV3 body,
                                                   String[] target, String dcType, boolean includeContext) {
        AnnotationV3 ann = new AnnotationV3(getAnnoIdUrl(annoId));
        if (includeContext) ann.setContext(new String[]{MEDIA_TYPE_IIIF_V3, MEDIA_TYPE_EDM_JSONLD});
        ann.setBody(body);
        ann.setTarget(target);
        ann.setDcType(dcType);
        ann.setMotivation(MOTIV_3);
        return ann;
    }

    private static String getResourceIdUrl(String from, String to, String resId){
        return getResourceIdBaseUrl(resId) + "#char=" + from + "," + to;
    }

    private static String getResourceIdBaseUrl(String resId){
        return RESOURCEBASEURL + DS_ID + "/" + LCL_ID + "/" + resId;
    }

    private static String getTargetIdUrl(String pageId, String x, String y, String w, String h){
        return getTargetIdBaseUrl(pageId) + "#xywh=" + x + "," + y + "," + w + "," + h;
    }

    private static String getTargetIdBaseUrl(String pageId){
        return IIIFBASEURL + DS_ID + "/" + LCL_ID + "/canvas/" + pageId;
    }
    
    private static String getAnnoIdUrl(String annoId){
        return ANNOTATIONBASEURL + DS_ID + "/" + LCL_ID + "/" + annoId;
    }

    private static String getAnnopageIdUrl(String pageId){
        return IIIFBASEURL + DS_ID + "/" + LCL_ID + "/annopage/" + pageId;
    }

    public static String getETagFromIds(String dsId, String lcId, String pgOrAnnoID,
                                        String iiifVersion, String appVersion, boolean weakETag){
        return CacheUtils.generateETag(dsId + lcId + pgOrAnnoID,
                                       CacheUtils.dateToZonedUTC(lastModifiedDate),
                                       iiifVersion, appVersion, weakETag);
    }

}
