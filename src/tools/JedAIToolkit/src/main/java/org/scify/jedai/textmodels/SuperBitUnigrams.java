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

/**
 *
 * @author GAP2
 */
public class SuperBitUnigrams extends TokenNGramsWithGlobalWeights {

    public SuperBitUnigrams(String instanceName) {
        super(DATASET_1, 1, RepresentationModel.TOKEN_UNIGRAMS_TF_IDF, SimilarityMetric.COSINE_SIMILARITY, instanceName);
    }

    public static int getCorpusDimensionality() {
        return DOC_FREQ[DATASET_1].size();
    }

    public double[] getVector() {
        int counter = 0;
        double[] tfIdfVector = new double[getCorpusDimensionality()];
        for (String token : DOC_FREQ[DATASET_1].keySet()) {
            tfIdfVector[counter++] = itemsFrequency.get(token) / noOfTotalTerms * getIdfWeight(token);
        }
        return tfIdfVector;
    }
}
