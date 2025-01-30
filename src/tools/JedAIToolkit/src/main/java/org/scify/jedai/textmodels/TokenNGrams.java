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
 * @author G.A.P. II
 */
public class TokenNGrams extends BagModel {

    public TokenNGrams(int dId, int n, RepresentationModel model, SimilarityMetric simMetric, String iName) {
        super(dId, n, model, simMetric, iName);

        NO_OF_DOCUMENTS[datasetId]++;
    }

    @Override
    public void updateModel(String text) {
        final String[] tokens = text.toLowerCase().split("[\\W_]");

        int noOfTokens = tokens.length;
        noOfTotalTerms += noOfTokens;
        for (int j = 0; j <= noOfTokens - nSize; j++) {
            final StringBuilder sb = new StringBuilder();
            for (int k = 0; k < nSize; k++) {
                sb.append(tokens[j + k]).append(" ");
            }
            String feature = sb.toString().trim();

            if (0 < feature.length()) {
                if (!itemsFrequency.increment(feature)) {
                    itemsFrequency.put(feature, 1);
                }
            }
        }
    }
}
