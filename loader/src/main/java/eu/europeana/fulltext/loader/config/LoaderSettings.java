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

package eu.europeana.fulltext.loader.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * Contains settings from loader.properties and loader.user.properties files
 * @author Lúthien
 * Created on 31/05/2018
 */
@Configuration
@Component
@PropertySource("classpath:loader.properties")
@PropertySource(value = "classpath:loader.user.properties", ignoreResourceNotFound = true)
public class LoaderSettings {

    @Value("${resource.baseurl}")
    private String resourceBaseUrl;

    @Value("${batch.base.directory}")
    private String batchBaseDirectory;

    @Value("${stop.error.save}")
    private Boolean stopOnSaveError;

    public String getResourceBaseUrl() {
        return resourceBaseUrl;
    }

    public String getBatchBaseDirectory() { return batchBaseDirectory; }

    /**
     * @return true if the loader should stop the current loading process when there is an error saving data to Mongo
     */
    public Boolean isStopOnSaveError() {
        return stopOnSaveError;
    }

}
