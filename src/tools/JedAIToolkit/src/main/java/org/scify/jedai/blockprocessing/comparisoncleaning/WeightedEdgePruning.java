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

import com.esotericsoftware.minlog.Log;
import gnu.trove.iterator.TIntIterator;

import java.util.ArrayList;
import java.util.List;
import org.scify.jedai.configuration.gridsearch.IntGridSearchConfiguration;
import org.scify.jedai.configuration.randomsearch.IntRandomSearchConfiguration;

/**
 *
 * @author gap2
 */
public class WeightedEdgePruning extends AbstractMetablocking {
    private static final long serialVersionUID = 4621154328472546492L;

    protected float noOfEdges;

    protected final IntGridSearchConfiguration gridWScheme;
    protected final IntRandomSearchConfiguration randomWScheme;
    
    public WeightedEdgePruning() {
        this(WeightingScheme.CBS);
    }

    public WeightedEdgePruning(WeightingScheme scheme) {
        super(scheme);
        nodeCentric = false;
        
        gridWScheme = new IntGridSearchConfiguration(WeightingScheme.values().length - 1, 0, 1);
        randomWScheme = new IntRandomSearchConfiguration(WeightingScheme.values().length, 0);
    }
    
    public void setWeightingScheme(WeightingScheme weightingScheme) {
        this.weightingScheme = weightingScheme;
    }
    
    @Override
    public String getMethodConfiguration() {
        return getParameterName(0) + "=" + weightingScheme;
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": a Meta-blocking method that retains all comparisons "
                + "that have a weight higher than the average edge weight in the blocking graph.";
    }

    @Override
    public String getMethodName() {
        return "Weighted Edge Pruning";
    }
    
    @Override
    public int getNumberOfGridConfigurations() {
        return gridWScheme.getNumberOfConfigurations();
    }

    protected void processArcsEntity(int entityId) {
        validEntities.clear();
        flags = new int[noOfEntities];
        for (int i = 0; i < noOfEntities; i++) {
            flags[i] = -1;
        }
        
        final int[] associatedBlocks = entityIndex.getEntityBlocks(entityId, 0);
        if (associatedBlocks.length == 0) {
            return;
        }

        for (int blockIndex : associatedBlocks) {
            float blockComparisons = cleanCleanER ? bBlocks[blockIndex].getNoOfComparisons() : uBlocks[blockIndex].getNoOfComparisons();
            setNormalizedNeighborEntities(blockIndex, entityId);
            for (TIntIterator tIterator = neighbors.iterator(); tIterator.hasNext();) {
                int neighborId = tIterator.next();
                if (flags[neighborId] != entityId) {
                    counters[neighborId] = 0;
                    flags[neighborId] = entityId;
                }

                counters[neighborId] += 1 / blockComparisons;
                validEntities.add(neighborId);
            }
        }
    }

    protected void processEntity(int entityId) {
        validEntities.clear();
        flags = new int[noOfEntities];
        for (int i = 0; i < noOfEntities; i++) {
            flags[i] = -1;
        }
        
        final int[] associatedBlocks = entityIndex.getEntityBlocks(entityId, 0);
        if (associatedBlocks.length == 0) {
            return;
        }

        for (int blockIndex : associatedBlocks) {
            setNormalizedNeighborEntities(blockIndex, entityId);
            for (TIntIterator tIterator = neighbors.iterator(); tIterator.hasNext();) {
                int neighborId = tIterator.next();
                if (flags[neighborId] != entityId) {
                    counters[neighborId] = 0;
                    flags[neighborId] = entityId;
                }

                counters[neighborId]++;
                validEntities.add(neighborId);
            }
        }
    }

    @Override
    protected List<AbstractBlock> pruneEdges() {
        final List<AbstractBlock> newBlocks = new ArrayList<>();
        int limit = cleanCleanER ? datasetLimit : noOfEntities;
        if (weightingScheme.equals(WeightingScheme.ARCS)) {
            for (int i = 0; i < limit; i++) {
                processArcsEntity(i);
                verifyValidEntities(i, newBlocks);
            }
        } else {
            for (int i = 0; i < limit; i++) {
                processEntity(i);
                verifyValidEntities(i, newBlocks);
            }
        }
        return newBlocks;
    }

    @Override
    public void setNextRandomConfiguration() {
        int schemeId = (Integer) randomWScheme.getNextRandomValue();
        weightingScheme = WeightingScheme.values()[schemeId];
    }

    @Override
    public void setNumberedGridConfiguration(int iterationNumber) {
        int schemeId = (Integer) gridWScheme.getNumberedValue(iterationNumber);
        weightingScheme = WeightingScheme.values()[schemeId];
    }
    
    @Override
    public void setNumberedRandomConfiguration(int iterationNumber) {
        int schemeId = (Integer) randomWScheme.getNumberedRandom(iterationNumber);
        weightingScheme = WeightingScheme.values()[schemeId];
    }
    
    @Override
    protected void setThreshold() {
        noOfEdges = 0;
        threshold = 0;

        int limit = cleanCleanER ? datasetLimit : noOfEntities;
        if (weightingScheme.equals(WeightingScheme.ARCS)) {
            for (int i = 0; i < limit; i++) {
                processArcsEntity(i);
                updateThreshold(i);
            }
        } else {
            for (int i = 0; i < limit; i++) {
                processEntity(i);
                updateThreshold(i);
            }
        }

        threshold /= noOfEdges;

        Log.info("Edge Pruning Weight Threshold\t:\t" + threshold);
    }

    protected void updateThreshold(int entityId) {
        noOfEdges += validEntities.size();
        for (TIntIterator tIterator = validEntities.iterator(); tIterator.hasNext();) {
            threshold += getWeight(entityId, tIterator.next());
        }
    }

    protected void verifyValidEntities(int entityId, List<AbstractBlock> newBlocks) {
        retainedNeighbors.clear();
        retainedNeighborsWeights.clear();
        if (!cleanCleanER) {
            for (TIntIterator tIterator = validEntities.iterator(); tIterator.hasNext();) {
                int neighborId = tIterator.next();
                float weight = getWeight(entityId, neighborId);
                if (threshold <= weight) {
                    retainedNeighbors.add(neighborId);
                    retainedNeighborsWeights.add(discretizeComparisonWeight(weight));
                }
            }
            addDecomposedBlock(entityId, retainedNeighbors, retainedNeighborsWeights, newBlocks);
        } else {
            if (entityId < datasetLimit) {
                for (TIntIterator tIterator = validEntities.iterator(); tIterator.hasNext();) {
                    int neighborId = tIterator.next();
                    float weight = getWeight(entityId, neighborId);
                    if (threshold <= weight) {
                        retainedNeighbors.add(neighborId - datasetLimit);
                        retainedNeighborsWeights.add(discretizeComparisonWeight(weight));
                    }
                }
                addDecomposedBlock(entityId, retainedNeighbors, retainedNeighborsWeights, newBlocks);
            } else {
                for (TIntIterator tIterator = validEntities.iterator(); tIterator.hasNext();) {
                    int neighborId = tIterator.next();
                    float weight = getWeight(entityId, neighborId);
                    if (threshold <= weight) {
                        retainedNeighbors.add(neighborId);
                        retainedNeighborsWeights.add(discretizeComparisonWeight(weight));
                    }
                }
                addReversedDecomposedBlock(entityId - datasetLimit, retainedNeighbors, retainedNeighborsWeights, newBlocks);
            }
        }
    }
}
