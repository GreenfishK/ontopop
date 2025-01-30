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
import org.scify.jedai.utilities.enumerations.WeightingScheme;
import gnu.trove.iterator.TIntIterator;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author gap2
 */
public class WeightedNodePruning extends WeightedEdgePruning {
    private static final long serialVersionUID = -2453344619247460484L;
    protected int firstId;
    protected int lastId;
    protected float[] averageWeight;

    public WeightedNodePruning() {
        this(WeightingScheme.ARCS);
    }

    public WeightedNodePruning(WeightingScheme scheme) {
        super(scheme);
        nodeCentric = true;
    }

    public void setWeightingScheme(WeightingScheme weightingScheme) {
        this.weightingScheme = weightingScheme;
    }
    
    @Override
    public String getMethodInfo() {
        return getMethodName() + ": a Meta-blocking method that retains for every entity, the comparisons "
                + "that correspond to edges in the blocking graph that are exceed the average edge weight "
                + "in the respective node neighborhood.";
    }

    @Override
    public String getMethodName() {
        return "Weighted Node Pruning";
    }

    protected float getValidWeight(int entityId, int neighborId) {
        float weight = getWeight(entityId, neighborId);
        boolean inNeighborhood1 = averageWeight[entityId] <= weight;
        boolean inNeighborhood2 = averageWeight[neighborId] <= weight;

        if (inNeighborhood1 || inNeighborhood2) {
            if (entityId < neighborId) {
                return weight;
            }
        }

        return -1;
    }

    @Override
    protected List<AbstractBlock> pruneEdges() {
        final List<AbstractBlock> newBlocks = new ArrayList<>();
        if (weightingScheme.equals(WeightingScheme.ARCS)) {
            for (int i = 0; i < noOfEntities; i++) {
                processArcsEntity(i);
                verifyValidEntities(i, newBlocks);
            }
        } else {
            for (int i = 0; i < noOfEntities; i++) {
                processEntity(i);
                verifyValidEntities(i, newBlocks);
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
        averageWeight = new float[noOfEntities];
        if (weightingScheme.equals(WeightingScheme.ARCS)) {
            for (int i = 0; i < noOfEntities; i++) {
                processArcsEntity(i);
                setThreshold(i);
                averageWeight[i] = threshold;
            }
        } else {
            for (int i = 0; i < noOfEntities; i++) {
                processEntity(i);
                setThreshold(i);
                averageWeight[i] = threshold;
            }
        }
    }

    protected void setThreshold(int entityId) {
        threshold = 0;
        for (TIntIterator iterator = validEntities.iterator(); iterator.hasNext();) {
            threshold += getWeight(entityId, iterator.next());
        }
        threshold /= validEntities.size();
    }

    @Override
    protected void verifyValidEntities(int entityId, List<AbstractBlock> newBlocks) {
        retainedNeighbors.clear();
        retainedNeighborsWeights.clear();
        if (!cleanCleanER) {
            for (TIntIterator tIterator = validEntities.iterator(); tIterator.hasNext();) {
                int neighborId = tIterator.next();
                float weight = getValidWeight(entityId, neighborId);
                if (0 <= weight) {
                    retainedNeighbors.add(neighborId);
                    retainedNeighborsWeights.add(discretizeComparisonWeight(weight));
                }
            }
            addDecomposedBlock(entityId, retainedNeighbors, retainedNeighborsWeights, newBlocks);
        } else {
            if (entityId < datasetLimit) {
                for (TIntIterator tIterator = validEntities.iterator(); tIterator.hasNext();) {
                    int neighborId = tIterator.next();
                    float weight = getValidWeight(entityId, neighborId);
                    if (0 <= weight) {
                        retainedNeighbors.add(neighborId - datasetLimit);
                        retainedNeighborsWeights.add(discretizeComparisonWeight(weight));
                    }
                }
                addDecomposedBlock(entityId, retainedNeighbors, retainedNeighborsWeights, newBlocks);
            } else {
                for (TIntIterator tIterator = validEntities.iterator(); tIterator.hasNext();) {
                    int neighborId = tIterator.next();
                    float weight = getValidWeight(entityId, neighborId);
                    if (0 <= weight) {
                        retainedNeighbors.add(neighborId);
                        retainedNeighborsWeights.add(discretizeComparisonWeight(weight));
                    }
                }
                addReversedDecomposedBlock(entityId - datasetLimit, retainedNeighbors, retainedNeighborsWeights, newBlocks);
            }
        }
    }
}
