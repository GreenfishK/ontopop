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

import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.HashSet;
import java.util.Set;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;

/**
 *
 * @author GAP2
 */
public class MinHashUnigrams extends TokenNGramsWithGlobalWeights {
    
    protected final Set<String> termsList;
    
    public MinHashUnigrams(String instanceName) {
        super(DATASET_1, 1, RepresentationModel.TOKEN_UNIGRAMS, SimilarityMetric.JACCARD_SIMILARITY, instanceName);
        
        termsList = new HashSet<>();
    }

    @Override
    public void finalizeModel() {
        if (DOC_FREQ[datasetId] == null) {
            DOC_FREQ[datasetId] = new TObjectIntHashMap<>();
        }
        
        termsList.forEach((term) -> {
            int vocabularySize = DOC_FREQ[DATASET_1].size();
            DOC_FREQ[DATASET_1].putIfAbsent(term, vocabularySize);
        });
    }

    public static int getCorpusDimensionality() {
        return DOC_FREQ[DATASET_1].size();
    }
    
    public Set<Integer> getTermIds() {
        final Set<Integer> termIds = new HashSet<>();
        termsList.forEach((term) -> {
            termIds.add(DOC_FREQ[DATASET_1].get(term));
        });
        return termIds;
    }
    
    @Override
    public void updateModel(String text) {
        final String[] tokens = text.toLowerCase().split("[\\W_]");
        for (String token : tokens) {
            int counter = 0;
            String currentTerm;
            do {
                counter++;
                currentTerm = token + "#" + counter;
            } while (termsList.contains(currentTerm));
            termsList.add(currentTerm);
        }
    }
}
