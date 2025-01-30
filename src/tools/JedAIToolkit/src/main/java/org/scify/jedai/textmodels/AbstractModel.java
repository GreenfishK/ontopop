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

package org.scify.jedai.textmodels;

import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;
import java.io.Serializable;

/**
 *
 * @author G.A.P. II
 */

public abstract class AbstractModel implements ITextModel, Serializable {
    
    private static final long serialVersionUID = 328759404L;

    protected final int datasetId;
    protected final int nSize;
    protected static float[] NO_OF_DOCUMENTS = {0, 0};
    
    protected final RepresentationModel modelType;
    protected final SimilarityMetric simMetric;
    protected final String instanceName;
    
    public AbstractModel(int dId, int n, RepresentationModel md, SimilarityMetric sMetric, String iName) {
        datasetId = dId;
        instanceName = iName;
        modelType = md;
        nSize = n;
        simMetric = sMetric;
    }
    
    public int getDatasetId() {
        return datasetId;
    }
    
    @Override
    public String getInstanceName() {
        return instanceName;
    }

    public RepresentationModel getModelType() {
        return modelType;
    }
    
    public static float getNoOfDocuments(int datasetId) {
        return NO_OF_DOCUMENTS[datasetId];
    }
    
    public int getNSize() {
        return nSize;
    }
    
    public SimilarityMetric getSimilarityMetric() {
        return simMetric;
    }
    
    public static void resetGlobalValues(int datasetId) {
        NO_OF_DOCUMENTS[datasetId] = 0;
    }
}