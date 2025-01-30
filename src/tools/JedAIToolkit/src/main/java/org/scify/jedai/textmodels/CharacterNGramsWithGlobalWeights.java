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
import com.esotericsoftware.minlog.Log;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author G.A.P. II
 */
public class CharacterNGramsWithGlobalWeights extends CharacterNGrams {

    protected final static TObjectIntMap<String>[] DOC_FREQ = new TObjectIntMap[2];

    public CharacterNGramsWithGlobalWeights(int did, int n, RepresentationModel model, SimilarityMetric simMetric, String iName) {
        super(did, n, model, simMetric, iName);
    }

    @Override
    public void finalizeModel() {
        if (DOC_FREQ[datasetId] == null) {
            DOC_FREQ[datasetId] = new TObjectIntHashMap<>();
        }

        itemsFrequency.keySet().stream().filter((keyValue) -> (!DOC_FREQ[datasetId].increment(keyValue))).forEachOrdered((keyValue) -> {
            DOC_FREQ[datasetId].put(keyValue, 1);
        });
    }

    protected float getARCSSimilarity(CharacterNGramsWithGlobalWeights oModel) {
        final Set<String> commonKeys = new HashSet<>(itemsFrequency.keySet());
        commonKeys.retainAll(oModel.getItemsFrequency().keySet());

        float similarity = 0;
        if (datasetId == DATASET_1 && datasetId == oModel.getDatasetId()) { // Dirty ER
            similarity = commonKeys.stream().map((key) -> DOC_FREQ[DATASET_1].get(key)).map((frequency) -> 1.0f / ((float) Math.log1p(frequency * (frequency - 1.0f) / 2.0f) / (float) Math.log(2))).reduce(similarity, (accumulator, _item) -> accumulator + _item);
        } else if (datasetId != oModel.getDatasetId()) { // Clean-Clean ER
            similarity = commonKeys.stream().map((key) -> 1.0f / ((float) Math.log1p(((float) DOC_FREQ[DATASET_1].get(key)) * DOC_FREQ[DATASET_2].get(key)) / (float) Math.log(2))).reduce(similarity, (accumulator, _item) -> accumulator + _item);
        } else {
            throw new IllegalStateException("Both models come from dataset 1.");
        }

        return similarity;
    }

    protected float getIdfWeight(String keyValue) {
        int frequency = DOC_FREQ[datasetId].get(keyValue);
        if (frequency == 0) {
            return 0;
        }

        if (NO_OF_DOCUMENTS[datasetId] < frequency) {
            Log.error("Error in the computation of IDF weights!!!");
            return 0;
        }
        
        return (float) Math.log10(NO_OF_DOCUMENTS[datasetId] / (1.0f + frequency));
    }

    protected float getSigmaSimilarity(CharacterNGramsWithGlobalWeights oModel) {
        float totalTerms2 = oModel.getNoOfTotalTerms();
        final TObjectIntMap<String> itemVector2 = oModel.getItemsFrequency();

        float numerator = 0.0f;
        for (TObjectIntIterator<String> iterator = itemsFrequency.iterator(); iterator.hasNext();) {
            iterator.advance();
            int frequency2 = itemVector2.get(iterator.key());
            if (0 < frequency2) {
                numerator += iterator.value() / noOfTotalTerms * getIdfWeight(iterator.key())
                           + frequency2 / totalTerms2 * oModel.getIdfWeight(iterator.key());
            }
        }

        final Set<String> allKeys = new HashSet<>(itemsFrequency.keySet());
        allKeys.addAll(itemVector2.keySet());
        float denominator = 0.0f;
        denominator = allKeys.stream().map((key) -> itemsFrequency.get(key) / noOfTotalTerms  * getIdfWeight(key) + 
                itemVector2.get(key) / totalTerms2 * oModel.getIdfWeight(key)).reduce(denominator, (accumulator, _item) -> accumulator + _item);

        return numerator / denominator;
    }

    @Override
    public float getSimilarity(ITextModel oModel) {
        switch (simMetric) {
            case ARCS_SIMILARITY:
                return getARCSSimilarity((CharacterNGramsWithGlobalWeights) oModel);
            case COSINE_SIMILARITY:
                return getTfIdfCosineSimilarity((CharacterNGramsWithGlobalWeights) oModel);
            case GENERALIZED_JACCARD_SIMILARITY:
                return getTfIdfGeneralizedJaccardSimilarity((CharacterNGramsWithGlobalWeights) oModel);
            case SIGMA_SIMILARITY:
                return getSigmaSimilarity((CharacterNGramsWithGlobalWeights) oModel);
            default:
                throw new IllegalStateException(
                    "The given similarity metric is incompatible with the bag representation model.");
        }
    }

    protected float getTfIdfCosineSimilarity(CharacterNGramsWithGlobalWeights oModel) {
        float totalTerms2 = oModel.getNoOfTotalTerms();
        final TObjectIntMap<String> itemVector2 = oModel.getItemsFrequency();

        float numerator = 0.0f;
        for (TObjectIntIterator<String> iterator = itemsFrequency.iterator(); iterator.hasNext();) {
            iterator.advance();
            int frequency2 = itemVector2.get(iterator.key());
            if (0 < frequency2) {
                numerator += (iterator.value() / noOfTotalTerms) * getIdfWeight(iterator.key())
                           * (frequency2 / totalTerms2) * oModel.getIdfWeight(iterator.key());
            }
        }

        float denominator = getVectorMagnitude() * oModel.getVectorMagnitude();
        return numerator / denominator;
    }

    protected float getTfIdfGeneralizedJaccardSimilarity(CharacterNGramsWithGlobalWeights oModel) {
        float totalTerms2 = oModel.getNoOfTotalTerms();
        final TObjectIntMap<String> itemVector2 = oModel.getItemsFrequency();

        float numerator = 0.0f;
        for (TObjectIntIterator<String> iterator = itemsFrequency.iterator(); iterator.hasNext();) {
            iterator.advance();
            int frequency2 = itemVector2.get(iterator.key());
            if (0 < frequency2) {
                numerator += Math.min(iterator.value() / noOfTotalTerms * getIdfWeight(iterator.key()),
                                      frequency2 / totalTerms2 * oModel.getIdfWeight(iterator.key()));
            }
        }

        final Set<String> allKeys = new HashSet<>(itemsFrequency.keySet());
        allKeys.addAll(itemVector2.keySet());
        float denominator = 0.0f;
        denominator = allKeys.stream().map((key) -> Math.max(itemsFrequency.get(key) / noOfTotalTerms  * getIdfWeight(key),
                itemVector2.get(key) / totalTerms2 * oModel.getIdfWeight(key))).reduce(denominator, (accumulator, _item) -> accumulator + _item);

        return numerator / denominator;
    }

    @Override
    protected float getVectorMagnitude() {
        float magnitude = 0.0f;
        for (TObjectIntIterator<String> iterator = itemsFrequency.iterator(); iterator.hasNext();) {
            iterator.advance();
            magnitude += Math.pow(iterator.value() * getIdfWeight(iterator.key()) / noOfTotalTerms, 2.0);
        }

        return (float) Math.sqrt(magnitude);
    }
    
    public static void resetGlobalValues(int datasetId) {
        NO_OF_DOCUMENTS[datasetId] = 0;
        if (DOC_FREQ[datasetId] != null) {
            DOC_FREQ[datasetId].clear();
        }
    }
}
