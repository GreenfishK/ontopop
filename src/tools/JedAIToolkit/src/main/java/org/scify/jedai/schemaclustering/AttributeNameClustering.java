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
package org.scify.jedai.schemaclustering;

import org.scify.jedai.datamodel.Attribute;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;

/**
 *
 * @author GAP2
 */
public class AttributeNameClustering extends AbstractAttributeClustering {

    public AttributeNameClustering(RepresentationModel model, SimilarityMetric metric) {
        this(0, model, metric);
    }

    public AttributeNameClustering(float a, RepresentationModel model, SimilarityMetric metric) {
        super(a, model, metric);
    }
    
    @Override
    protected void updateModel(int datasetId, Attribute attribute) {
        int attributeId = attrNameIndex.get(attribute.getName()) - 1;
        attributeModels[datasetId][attributeId].updateModel(attribute.getName());
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it clusters together attributes with similar names as"
                + " determined by the selected combination of representation model and similarity metric.";
    }

    @Override
    public String getMethodName() {
        return "Attribute Name Clustering";
    }

}
