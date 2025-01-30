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

public class CharacterNGrams extends BagModel {
    
    public CharacterNGrams(int dId, int n, RepresentationModel model, SimilarityMetric simMetric, String iName) {
        super(dId, n, model, simMetric, iName);
        
        NO_OF_DOCUMENTS[datasetId]++;
    }
    
    @Override
    public void updateModel(String text) {
        String lowerCasedText = text.toLowerCase();
        int currentPosition = 0;
        final int length = lowerCasedText.length() - (nSize-1);
        while (currentPosition < length) {
            noOfTotalTerms++;
            final String term = lowerCasedText.substring(currentPosition, currentPosition + nSize);
            if (!itemsFrequency.increment(term)) {
                itemsFrequency.put(term, 1);
            }
            currentPosition++;
        }
    }
}