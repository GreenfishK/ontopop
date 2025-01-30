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

import gnu.trove.iterator.TIntIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.scify.jedai.blockprocessing.comparisoncleaning.WeightedNodePruning;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.VertexWeight;
import org.scify.jedai.utilities.comparators.DecComparisonWeightComparator;
import org.scify.jedai.utilities.comparators.DecVertexWeightComparator;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 *
 * @author gap2
 */
public class ProgressiveWNP extends WeightedNodePruning {

    protected List<VertexWeight> sortedEntities;
    protected Set<Comparison> topComparisons;
    
    public ProgressiveWNP(WeightingScheme wScheme) {
        super(wScheme);
    }

    public List<Comparison> getSortedTopComparisons() {
        final List<Comparison> sortedTopComparisons = new ArrayList<>(topComparisons);
        sortedTopComparisons.sort(new DecComparisonWeightComparator());
        topComparisons = null;
        return sortedTopComparisons;
    }
    
    public List<VertexWeight> getSortedEntities() {
        return sortedEntities;
    }
    
    @Override
    protected List<AbstractBlock> pruneEdges() {
        return null;
    }
    
    @Override
    protected void setThreshold() {
        topComparisons = new HashSet<>();
        sortedEntities = new ArrayList<>();
        if (weightingScheme.equals(WeightingScheme.ARCS)) {
            for (int i = 0; i < noOfEntities; i++) {
                processArcsEntity(i);
                setThreshold(i);
            }
        } else {
            for (int i = 0; i < noOfEntities; i++) {
                processEntity(i);
                setThreshold(i);
            }
        }
        sortedEntities.sort(new DecVertexWeightComparator());
    }

    @Override
    protected void setThreshold(int entityId) {
        threshold = 0;
        int maxCompId = -1;
        float maxWeight = -1;
        for (TIntIterator iterator = validEntities.iterator(); iterator.hasNext();) {
            int neighborId = iterator.next();
            float currentWeight = getWeight(entityId, neighborId);
            if (maxWeight < currentWeight) {
                maxCompId = neighborId;
                maxWeight = currentWeight;
            }
            threshold += getWeight(entityId, neighborId);
        }
        
        if (0 <= maxCompId) {
            final Comparison currentTopComparison = getComparison(entityId, maxCompId);
            currentTopComparison.setUtilityMeasure(maxWeight);
            topComparisons.add(currentTopComparison);
            sortedEntities.add(new VertexWeight(entityId, threshold, validEntities.size(), null));
        } 
    }
}
