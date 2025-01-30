/*
 * Copyright [2016-2020] [George Papadakis (gpapadis@yahoo.gr)]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scify.jedai.utilities.enumerations;

import org.scify.jedai.schemaclustering.AttributeNameClustering;
import org.scify.jedai.schemaclustering.AttributeValueClustering;
import org.scify.jedai.schemaclustering.HolisticAttributeClustering;
import org.scify.jedai.schemaclustering.ISchemaClustering;

/**
 *
 * @author GAP2
 */
public enum SchemaClusteringMethod {
    ATTRIBUTE_NAME_CLUSTERING,
    ATTRIBUTE_VALUE_CLUSTERING,
    HOLISTIC_ATTRIBUTE_CLUSTERING;

    public static ISchemaClustering getModel(RepresentationModel model, SimilarityMetric simMetric, SchemaClusteringMethod scMethod) {
        switch (scMethod) {
            case ATTRIBUTE_NAME_CLUSTERING:
                return new AttributeNameClustering(model, simMetric);
            case ATTRIBUTE_VALUE_CLUSTERING:
                return new AttributeValueClustering(model, simMetric);
            case HOLISTIC_ATTRIBUTE_CLUSTERING:
                return new HolisticAttributeClustering(model, simMetric);
            default:
                return null;
        }
    }
}
