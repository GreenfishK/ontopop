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
package org.scify.jedai.blockprocessing.comparisoncleaning;

import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.DecomposedBlock;
import org.scify.jedai.utilities.comparators.IncComparisonWeightComparator;
import org.scify.jedai.utilities.enumerations.WeightingScheme;
import com.esotericsoftware.minlog.Log;
import gnu.trove.iterator.TIntIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 *
 * @author gap2
 */
public class CardinalityEdgePruning extends WeightedEdgePruning {
    private static final long serialVersionUID = 292989752882791840L;

    protected float minimumWeight;
    
    protected Queue<Comparison> topKEdges;

    public CardinalityEdgePruning() {
        super(WeightingScheme.ARCS);
    }

    public CardinalityEdgePruning(WeightingScheme scheme) {
        super(scheme);
        nodeCentric = false;
    }

    public void setWeightingScheme(WeightingScheme weightingScheme) {
        this.weightingScheme = weightingScheme;
    }
    
    protected void addDecomposedBlock(Collection<Comparison> comparisons, List<AbstractBlock> newBlocks) {
        if (comparisons.isEmpty()) {
            return;
        }

        final int[] entityIds1 = new int[comparisons.size()];
        final int[] entityIds2 = new int[comparisons.size()];
        final int[] weights = new int[comparisons.size()];
        
        int index = 0;
        for (Comparison comparison : comparisons) {
            entityIds1[index] = comparison.getEntityId1();
            entityIds2[index] = comparison.getEntityId2();
            weights[index] = discretizeComparisonWeight(comparison.getUtilityMeasure());
            index++;
        }

        newBlocks.add(new DecomposedBlock(cleanCleanER, entityIds1, entityIds2, weights));
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": a Meta-blocking method that retains the comparisons "
                + "that correspond to the top-K weighted edges in the blocking graph.";
    }

    @Override
    public String getMethodName() {
        return "Cardinality Edge Pruning";
    }

    @Override
    protected List<AbstractBlock> pruneEdges() {
        setTopKEdges();
        final List<AbstractBlock> newBlocks = new ArrayList<>();
        addDecomposedBlock(topKEdges, newBlocks);
        return newBlocks;
    }

    protected void setTopKEdges() {
        minimumWeight = Float.MIN_VALUE;
        topKEdges = new PriorityQueue<>((int) (2 * threshold), new IncComparisonWeightComparator());

        int limit = cleanCleanER ? datasetLimit : noOfEntities;
        if (weightingScheme.equals(WeightingScheme.ARCS)) {
            for (int i = 0; i < limit; i++) {
                processArcsEntity(i);
                verifyValidEntities(i);
            }
        } else {
            for (int i = 0; i < limit; i++) {
                processEntity(i);
                verifyValidEntities(i);
            }
        }
    }
    
    @Override
    protected void setThreshold() {
        threshold = blockAssingments / 2;
        
        Log.info(getMethodName() + " Threshold \t:\t" + threshold);
    }

    protected void verifyValidEntities(int entityId) {
        for (TIntIterator iterator = validEntities.iterator(); iterator.hasNext();) {
            int neighborId = iterator.next();
            float weight = getWeight(entityId, neighborId);
            if (!(weight < minimumWeight)) {
                final Comparison comparison = getComparison(entityId, neighborId);
                comparison.setUtilityMeasure(weight);
                topKEdges.add(comparison);
                if (threshold < topKEdges.size()) {
                    final Comparison lastComparison = topKEdges.poll();
                    minimumWeight = lastComparison.getUtilityMeasure();
                }
            }
        }
    }
}
