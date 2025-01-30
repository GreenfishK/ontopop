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
package org.scify.jedai.prioritization.utilities;

import com.esotericsoftware.minlog.Log;
import gnu.trove.iterator.TIntIterator;
import org.scify.jedai.blockprocessing.comparisoncleaning.CardinalityNodePruning;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.utilities.comparators.DecComparisonWeightComparator;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author gap2
 */
public class ProgressiveCNP extends CardinalityNodePruning {

    private final int comparisonsBudget;
    
    private final Set<Comparison> topComparisons;
    
    public ProgressiveCNP(int comparisonsBudget, WeightingScheme wScheme) {
        super(wScheme);
        this.comparisonsBudget = comparisonsBudget;
        topComparisons = new HashSet<>();
    }

    @Override
    public String getMethodName() {
        return "Progressive CNP";
    }
    
    public List<Comparison> getTopComparisons() {
        final List<Comparison> sortedTopComparisons = new ArrayList<>(topComparisons);
        sortedTopComparisons.sort(new DecComparisonWeightComparator());
        return sortedTopComparisons;
    }

    @Override
    protected List<AbstractBlock> retainValidComparisons() {
        return null;
    }
    
    @Override
    protected void setThreshold() {
        threshold = Math.max(1, 2 * comparisonsBudget / noOfEntities);
        Log.info(getMethodName() + " Threshold \t:\t" + threshold);
    }
    
    @Override
    protected void verifyValidEntities(int entityId) {
        if (validEntities.isEmpty()) {
            return;
        }

        topKEdges.clear();
        minimumWeight = Float.MIN_VALUE;
        for (TIntIterator iterator = validEntities.iterator(); iterator.hasNext();) {
            int neighborId = iterator.next();
            float weight = getWeight(entityId, neighborId);
            if (minimumWeight <= weight) {
                final Comparison comparison = getComparison(entityId, neighborId);
                comparison.setUtilityMeasure(weight);
                topKEdges.add(comparison);
                if (threshold < topKEdges.size()) {
                    Comparison lastComparison = topKEdges.poll();
                    minimumWeight = lastComparison.getUtilityMeasure();
                }
            }
        }
        
        topComparisons.addAll(topKEdges);
    }
}
