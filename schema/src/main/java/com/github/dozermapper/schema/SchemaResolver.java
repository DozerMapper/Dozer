/*
 * Copyright 2005-2017 Dozer Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dozermapper.schema;

import java.net.URL;

/**
 * Resolve xsd file via flat classpath or OSGi bundle.
 */
public interface SchemaResolver {

    /**
     * Gets dozer.xsd as URL.
     *
     * @param fileName dozer.xsd filename to resolve on classpath
     * @return dozer.xsd URL
     */
    URL get(String fileName);
}
