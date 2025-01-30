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

import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.ComparisonIterator;
import org.scify.jedai.utilities.comparators.DecComparisonWeightComparator;
import org.scify.jedai.utilities.comparators.IncComparisonWeightComparator;

import java.util.*;

/**
 *
 * @author gap2
 */
public class ProgressiveCNPDecomponsedBlocks extends AbstractDecomposedBlocksProcessing {

    private final int maxComsPerEntity;

    private float[] minimumWeight;

    private Queue<Comparison>[] topComsPerEntity;

    public ProgressiveCNPDecomponsedBlocks(int comparisonsBudget, List<AbstractBlock> inputBlocks) {
        super(inputBlocks);
        maxComsPerEntity = Math.max(1, 2 * comparisonsBudget / noOfEntities);
        minimumWeight = new float[noOfEntities];
        for (int i = 0; i < minimumWeight[i]; i++) {
            minimumWeight[i] = -1;
        }
        topComsPerEntity = new PriorityQueue[noOfEntities];

        analyzeComparisons(inputBlocks);
    }

    private void analyzeComparisons(List<AbstractBlock> inputBlocks) {
        if (isCleanCleanER) {
            for (AbstractBlock block : inputBlocks) {
                final ComparisonIterator cIterator = block.getComparisonIterator();
                while (cIterator.hasNext()) {
                    final Comparison c = cIterator.next();

                    processEntityComparison(c.getEntityId1(), c);
                    processEntityComparison(c.getEntityId2() + datasetLimit, c);
                }
            }
        } else {
            for (AbstractBlock block : inputBlocks) {
                final ComparisonIterator cIterator = block.getComparisonIterator();
                while (cIterator.hasNext()) {
                    final Comparison c = cIterator.next();

                    processEntityComparison(c.getEntityId1(), c);
                    processEntityComparison(c.getEntityId2(), c);
                }
            }
        }

        minimumWeight = null;
        setComparisonIterator();
    }

    private void setComparisonIterator() {
        final Set<Comparison> topComparisons = new HashSet<>();
        for (int i = 0; i < noOfEntities; i++) {
            if (topComsPerEntity[i] != null) {
                topComparisons.addAll(topComsPerEntity[i]);
            }
        }
        topComsPerEntity = null;
        
        final List<Comparison> sortedTopComparisons = new ArrayList<>(topComparisons);
        sortedTopComparisons.sort(new DecComparisonWeightComparator());
        compIterator = sortedTopComparisons.iterator();
    }
    
    private void processEntityComparison(int entityId, Comparison c) {
        if (minimumWeight[entityId] < c.getUtilityMeasure()) {
            if (topComsPerEntity[entityId] == null) {
                topComsPerEntity[entityId] = new PriorityQueue<>(2 * maxComsPerEntity, new IncComparisonWeightComparator());
            }

            topComsPerEntity[entityId].add(c);
            if (maxComsPerEntity < topComsPerEntity[entityId].size()) {
                Comparison lastComparison = topComsPerEntity[entityId].poll();
                minimumWeight[entityId] = lastComparison.getUtilityMeasure();
            }
        }
    }
}
