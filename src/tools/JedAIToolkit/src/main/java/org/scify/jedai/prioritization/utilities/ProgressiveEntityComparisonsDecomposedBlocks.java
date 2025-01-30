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
import org.scify.jedai.datamodel.VertexWeight;
import org.scify.jedai.utilities.comparators.DecComparisonWeightComparator;
import org.scify.jedai.utilities.comparators.DecVertexWeightComparator;

import java.util.*;

/**
 *
 * @author gap2
 */
public class ProgressiveEntityComparisonsDecomposedBlocks extends AbstractDecomposedBlocksProcessing {

    protected Iterator<VertexWeight> entityIterator;

    public ProgressiveEntityComparisonsDecomposedBlocks(List<AbstractBlock> inputBlocks) {
        super(inputBlocks);
        analyzeComparisons(inputBlocks);
    }

    private void analyzeComparisons(List<AbstractBlock> inputBlocks) {
        final int[] comparisonsPerEntity = new int[noOfEntities];
        final float[] totalWeightPerEntity = new float[noOfEntities];
        final Comparison[] topComparisonPerEntity = new Comparison[noOfEntities];
        if (isCleanCleanER) {
            for (AbstractBlock block : inputBlocks) {
                final ComparisonIterator cIterator = block.getComparisonIterator();
                while (cIterator.hasNext()) {
                    final Comparison c = cIterator.next();

                    comparisonsPerEntity[c.getEntityId1()]++;
                    totalWeightPerEntity[c.getEntityId1()] += c.getUtilityMeasure();

                    int entityId2 = c.getEntityId2() + datasetLimit;
                    comparisonsPerEntity[entityId2]++;
                    totalWeightPerEntity[entityId2] += c.getUtilityMeasure();

                    if (topComparisonPerEntity[c.getEntityId1()] == null) {
                        topComparisonPerEntity[c.getEntityId1()] = c;
                    } else if (topComparisonPerEntity[c.getEntityId1()].getUtilityMeasure() < c.getUtilityMeasure()) {
                        topComparisonPerEntity[c.getEntityId1()] = c;
                    }

                    if (topComparisonPerEntity[entityId2] == null) {
                        topComparisonPerEntity[entityId2] = c;
                    } else if (topComparisonPerEntity[entityId2].getUtilityMeasure() < c.getUtilityMeasure()) {
                        topComparisonPerEntity[entityId2] = c;
                    }
                }
            }
        } else {
            for (AbstractBlock block : inputBlocks) {
                final ComparisonIterator cIterator = block.getComparisonIterator();
                while (cIterator.hasNext()) {
                    final Comparison c = cIterator.next();

                    comparisonsPerEntity[c.getEntityId1()]++;
                    totalWeightPerEntity[c.getEntityId1()] += c.getUtilityMeasure();

                    comparisonsPerEntity[c.getEntityId2()]++;
                    totalWeightPerEntity[c.getEntityId2()] += c.getUtilityMeasure();

                    if (topComparisonPerEntity[c.getEntityId1()] == null) {
                        topComparisonPerEntity[c.getEntityId1()] = c;
                    } else if (topComparisonPerEntity[c.getEntityId1()].getUtilityMeasure() < c.getUtilityMeasure()) {
                        topComparisonPerEntity[c.getEntityId1()] = c;
                    }

                    if (topComparisonPerEntity[c.getEntityId2()] == null) {
                        topComparisonPerEntity[c.getEntityId2()] = c;
                    } else if (topComparisonPerEntity[c.getEntityId2()].getUtilityMeasure() < c.getUtilityMeasure()) {
                        topComparisonPerEntity[c.getEntityId2()] = c;
                    }
                }
            }
        }

        final Set<Comparison> topComparisons = new HashSet<>(Arrays.asList(topComparisonPerEntity));
        final List<Comparison> sortedTopComparisons = new ArrayList<>(topComparisons);
        sortedTopComparisons.sort(new DecComparisonWeightComparator());
        compIterator = sortedTopComparisons.iterator();

        final List<VertexWeight> sortedEntities = new ArrayList<>();
        for (int i = 0; i < noOfEntities; i++) {
            sortedEntities.add(new VertexWeight(i, totalWeightPerEntity[i], comparisonsPerEntity[i], null));
        }
        sortedEntities.sort(new DecVertexWeightComparator());
        entityIterator = sortedEntities.iterator();
    }

    public Iterator<VertexWeight> getEntityIterator() {
        return entityIterator;
    }
}
