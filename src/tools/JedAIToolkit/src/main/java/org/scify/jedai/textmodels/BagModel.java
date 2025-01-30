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
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author G.A.P. II
 */
public abstract class BagModel extends AbstractModel {

    protected float noOfTotalTerms;
    protected final TObjectIntMap<String> itemsFrequency;

    public BagModel(int dId, int n, RepresentationModel md, SimilarityMetric sMetric, String iName) {
        super(dId, n, md, sMetric, iName);

        itemsFrequency = new TObjectIntHashMap<>();
    }

    @Override
    public void finalizeModel() {
    }
    
    protected float getEnhancedJaccardSimilarity(BagModel oModel) {
        TObjectIntMap<String> itemVector1 = itemsFrequency;
        TObjectIntMap<String> itemVector2 = oModel.getItemsFrequency();
        if (itemVector2.size() < itemVector1.size()) {
            itemVector1 = oModel.getItemsFrequency();
            itemVector2 = itemsFrequency;
        }

        float numerator = 0.0f;
        for (TObjectIntIterator<String> iterator = itemVector1.iterator(); iterator.hasNext();) {
            iterator.advance();
            numerator += Math.min(iterator.value(), itemVector2.get(iterator.key()));
        }

        float denominator = noOfTotalTerms + oModel.getNoOfTotalTerms() - numerator;
        return numerator / denominator;
    }
    
    @Override
    public float getEntropy(boolean normalized) {
        float entropy = 0.0f;
        for (TObjectIntIterator<String> iterator = itemsFrequency.iterator(); iterator.hasNext();) {
            iterator.advance();
            float p_i = (iterator.value() / noOfTotalTerms);
            entropy -= (p_i * (Math.log10(p_i) / Math.log10(2.0d)));
        }
        
        if (normalized) {
            float maxEntropy = (float) Math.log10(noOfTotalTerms) / (float) Math.log10(2.0f);
            return entropy / maxEntropy;
        } 
            
        return entropy;
    }

    public TObjectIntMap<String> getItemsFrequency() {
        return itemsFrequency;
    }

    protected float getJaccardSimilarity(BagModel oModel) {
        final Set<String> commonKeys = new HashSet<>(itemsFrequency.keySet());
        commonKeys.retainAll(oModel.getItemsFrequency().keySet());

        float numerator = commonKeys.size();
        float denominator = itemsFrequency.size() + oModel.getItemsFrequency().size() - numerator;
        return numerator / denominator;
    }

    protected float getNoOfTotalTerms() {
        return noOfTotalTerms;
    }

    @Override
    public Set<String> getSignatures() {
        return itemsFrequency.keySet();
    }

    @Override
    public float getSimilarity(ITextModel oModel) {
        switch (simMetric) {
            case COSINE_SIMILARITY:
                return getTfCosineSimilarity((BagModel) oModel);
            case ENHANCED_JACCARD_SIMILARITY:
                return getEnhancedJaccardSimilarity((BagModel) oModel);
            case GENERALIZED_JACCARD_SIMILARITY:
                return getTfGeneralizedJaccardSimilarity((BagModel) oModel);
            case JACCARD_SIMILARITY:
                return getJaccardSimilarity((BagModel) oModel);
            default:
                throw new IllegalStateException(
                    "The given similarity metric is incompatible with the bag representation model.");
        }
    }

    protected float getTfCosineSimilarity(BagModel oModel) {
        float totalTerms2 = oModel.getNoOfTotalTerms();

        TObjectIntMap<String> itemVector1 = itemsFrequency;
        TObjectIntMap<String> itemVector2 = oModel.getItemsFrequency();
        if (itemVector2.size() < itemVector1.size()) {
            itemVector1 = oModel.getItemsFrequency();
            itemVector2 = itemsFrequency;
        }

        float numerator = 0.0f;
        for (TObjectIntIterator<String> iterator = itemVector1.iterator(); iterator.hasNext();) {
            iterator.advance();
            numerator += iterator.value() * itemVector2.get(iterator.key()) / noOfTotalTerms / totalTerms2;
        }

        float denominator = getVectorMagnitude() * oModel.getVectorMagnitude();
        return numerator / denominator;
    }

    protected float getTfGeneralizedJaccardSimilarity(BagModel oModel) {
        float totalTerms1 = noOfTotalTerms;
        float totalTerms2 = oModel.getNoOfTotalTerms();
        TObjectIntMap<String> itemVector1 = itemsFrequency;
        TObjectIntMap<String> itemVector2 = oModel.getItemsFrequency();
        if (itemVector2.size() < itemVector1.size()) {
            itemVector1 = oModel.getItemsFrequency();
            itemVector2 = itemsFrequency;

            totalTerms1 = oModel.getNoOfTotalTerms();
            totalTerms2 = noOfTotalTerms;
        }

        float numerator = 0.0f;
        for (TObjectIntIterator<String> iterator = itemVector1.iterator(); iterator.hasNext(); ) {
            iterator.advance();
            numerator += Math.min(iterator.value() / totalTerms1, itemVector2.get(iterator.key()) / totalTerms2);
        }

        final Set<String> allKeys = new HashSet<>(itemVector1.keySet());
        allKeys.addAll(itemVector2.keySet());
        float denominator = 0.0f;
        for (String key : allKeys) {
            denominator += Math.max(itemVector1.get(key) / totalTerms1, itemVector2.get(key) / totalTerms2);
        }
        
        return numerator / denominator;
    }

    protected float getVectorMagnitude() {
        float magnitude = 0.0f;
        for (TObjectIntIterator<String> iterator = itemsFrequency.iterator(); iterator.hasNext();) {
            iterator.advance();
            magnitude += Math.pow(iterator.value() / noOfTotalTerms, 2.0);
        }

        return (float) Math.sqrt(magnitude);
    }
}
