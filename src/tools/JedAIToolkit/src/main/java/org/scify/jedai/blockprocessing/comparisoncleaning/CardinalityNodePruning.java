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
import org.scify.jedai.utilities.comparators.IncComparisonWeightComparator;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

import com.esotericsoftware.minlog.Log;

import gnu.trove.iterator.TIntIterator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 *
 * @author gap2
 */
public class CardinalityNodePruning extends CardinalityEdgePruning {
    private static final long serialVersionUID = 3907895558183619683L;
    protected int firstId;
    protected int lastId;
    
    protected Set<Comparison>[] nearestEntities;

    public CardinalityNodePruning() {
        this(WeightingScheme.JS);
    }

    public CardinalityNodePruning(WeightingScheme scheme) {
        super(scheme);
        threshold = -1;
        nodeCentric = true;
    }

    public void setWeightingScheme(WeightingScheme weightingScheme) {
        this.weightingScheme = weightingScheme;
    }
    
    @Override
    public String getMethodInfo() {
        return getMethodName() + ": a Meta-blocking method that retains for every entity, "
                + "the comparisons that correspond to its top-k weighted edges in the blocking graph.";
    }

    @Override
    public String getMethodName() {
        return "Cardinality Node Pruning";
    }

    protected boolean isValidComparison(int entityId, int neighborId) {
        if (nearestEntities[neighborId] == null) {
            return true;
        }

        if (nearestEntities[neighborId].contains(new Comparison(cleanCleanER, -1, entityId))) {
            return entityId < neighborId;
        }

        return true;
    }

    @Override
    protected List<AbstractBlock> pruneEdges() {
        nearestEntities = new HashSet[noOfEntities];
        topKEdges = new PriorityQueue<>((int) (2 * threshold), new IncComparisonWeightComparator());
        if (weightingScheme.equals(WeightingScheme.ARCS)) {
            for (int i = 0; i < noOfEntities; i++) {
                processArcsEntity(i);
                verifyValidEntities(i);
            }
        } else {
            for (int i = 0; i < noOfEntities; i++) {
                processEntity(i);
                verifyValidEntities(i);
            }
        }

        return retainValidComparisons();
    }

    protected List<AbstractBlock> retainValidComparisons() {
        final List<AbstractBlock> newBlocks = new ArrayList<>();
        final List<Comparison> retainedComparisons = new ArrayList<>();
        for (int i = 0; i < noOfEntities; i++) {
            if (nearestEntities[i] != null) {
                retainedComparisons.clear();
                for (Comparison c : nearestEntities[i]) {
                    if (isValidComparison(i, c.getEntityId2())) {
                        final Comparison correctComparison = getComparison(i, c.getEntityId2());
                        correctComparison.setUtilityMeasure(c.getUtilityMeasure());
                        retainedComparisons.add(correctComparison);
                    }
                }
                addDecomposedBlock(retainedComparisons, newBlocks);
            }
        }
        return newBlocks;
    }

    protected void setLimits() {
        firstId = 0;
        lastId = noOfEntities;
    }

    @Override
    protected void setThreshold() {
        threshold = Math.max(1, blockAssingments / noOfEntities);
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
            if (!(weight < minimumWeight)) {
                final Comparison comparison = new Comparison(cleanCleanER, -1, neighborId);
                comparison.setUtilityMeasure(weight);
                topKEdges.add(comparison);
                if (threshold < topKEdges.size()) {
                    Comparison lastComparison = topKEdges.poll();
                    minimumWeight = lastComparison.getUtilityMeasure();
                }
            }
        }

        nearestEntities[entityId] = new HashSet<>(topKEdges);
    }
}
