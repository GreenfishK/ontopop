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

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.BilateralBlock;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.UnilateralBlock;
import org.scify.jedai.utilities.IConstants;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

import java.util.List;

/**
 *
 * @author G.A.P. II
 */
public abstract class AbstractMetablocking extends AbstractComparisonCleaning implements IConstants {
    private static final long serialVersionUID = 8416422399063318708L;

    protected boolean nodeCentric;

    protected int[] flags;

    protected float threshold;
    protected float blockAssingments;
    protected float distinctComparisons;
    protected float[] comparisonsPerEntity;
    protected float[] counters;

    protected ChiSquareTest chiSquaredTest;
    protected final TIntList neighbors;
    protected final TIntList retainedNeighbors;
    protected final TIntList retainedNeighborsWeights;
    protected WeightingScheme weightingScheme;

    public AbstractMetablocking(WeightingScheme wScheme) {
        super();
        
        if (wScheme.equals(WeightingScheme.PEARSON_X2)) {
            chiSquaredTest = new ChiSquareTest();
        }
        
        neighbors = new TIntArrayList();
        retainedNeighbors = new TIntArrayList();
        retainedNeighborsWeights = new TIntArrayList();
        weightingScheme = wScheme;
    }

    protected abstract List<AbstractBlock> pruneEdges();

    protected abstract void setThreshold();

    @Override
    protected List<AbstractBlock> applyMainProcessing() {
        counters = new float[noOfEntities];

        blockAssingments = 0;
        if (cleanCleanER) {
            for (BilateralBlock bBlock : bBlocks) {
                blockAssingments += bBlock.getTotalBlockAssignments();
            }
        } else {
            for (UnilateralBlock uBlock : uBlocks) {
                blockAssingments += uBlock.getTotalBlockAssignments();
            }
        }

        if (weightingScheme.equals(WeightingScheme.EJS)) {
            setStatistics();
        }

        setThreshold();
        return pruneEdges();
    }

    protected int discretizeComparisonWeight(float weight) {
        return (int) (weight * DISCRETIZATION_FACTOR);
    }

    protected Comparison getComparison(int entityId, int neighborId) {
        if (!cleanCleanER) {
            if (entityId < neighborId) {
                return new Comparison(cleanCleanER, entityId, neighborId);
            } else {
                return new Comparison(cleanCleanER, neighborId, entityId);
            }
        } else {
            if (entityId < datasetLimit) {
                return new Comparison(cleanCleanER, entityId, neighborId - datasetLimit);
            } else {
                return new Comparison(cleanCleanER, neighborId, entityId - datasetLimit);
            }
        }
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves a single parameter:\n"
                + "1)" + getParameterDescription(0) + ".\n";
    }

    protected int[] getNeighborEntities(int blockIndex, int entityId) {
        if (cleanCleanER) {
            if (entityId < datasetLimit) {
                return bBlocks[blockIndex].getIndex2Entities();
            } else {
                return bBlocks[blockIndex].getIndex1Entities();
            }
        }
        return uBlocks[blockIndex].getEntities();
    }

    @Override
    public JsonArray getParameterConfiguration() {
        final JsonObject obj1 = new JsonObject();
        obj1.put("class", "org.scify.jedai.utilities.enumerations.WeightingScheme");
        obj1.put("name", getParameterName(0));
        obj1.put("defaultValue", "org.scify.jedai.utilities.enumerations.WeightingScheme.JS");
        obj1.put("minValue", "-");
        obj1.put("maxValue", "-");
        obj1.put("stepValue", "-");
        obj1.put("description", getParameterDescription(0));

        final JsonArray array = new JsonArray();
        array.add(obj1);
        return array;
    }

    @Override
    public String getParameterDescription(int parameterId) {
        switch (parameterId) {
            case 0:
                return "The " + getParameterName(0) + " determines the function that assigns weights to the edges of the Blocking Graph.";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        switch (parameterId) {
            case 0:
                return "Weighting Scheme";
            default:
                return "invalid parameter id";
        }
    }

    protected float getWeight(int entityId, int neighborId) {
        switch (weightingScheme) {
            case ARCS:
                return counters[neighborId];
            case CBS:
                return counters[neighborId];
            case ECBS:
                return (float) (counters[neighborId] * Math.log10((float) noOfBlocks / entityIndex.getNoOfEntityBlocks(entityId, 0)) * Math.log10((float) noOfBlocks / entityIndex.getNoOfEntityBlocks(neighborId, 0)));
            case JS:
                return counters[neighborId] / (entityIndex.getNoOfEntityBlocks(entityId, 0) + entityIndex.getNoOfEntityBlocks(neighborId, 0) - counters[neighborId]);
            case EJS:
                float probability = counters[neighborId] / (entityIndex.getNoOfEntityBlocks(entityId, 0) + entityIndex.getNoOfEntityBlocks(neighborId, 0) - counters[neighborId]);
                return (float)(probability * Math.log10(distinctComparisons / comparisonsPerEntity[entityId]) * Math.log10(distinctComparisons / comparisonsPerEntity[neighborId]));
            case PEARSON_X2:
                long[] v = new long[2];
                v[0] = (long) counters[neighborId];
                v[1] = entityIndex.getNoOfEntityBlocks(entityId, 0) - v[0];

                long[] v_ = new long[2];
                v_[0] = entityIndex.getNoOfEntityBlocks(neighborId, 0) - v[0];
                v_[1] = (int) (noOfBlocks - (v[0] + v[1] + v_[0]));

                return (float) chiSquaredTest.chiSquare(new long[][]{v, v_});
        }
        return -1;
    }

    protected void setNormalizedNeighborEntities(int blockIndex, int entityId) {
        neighbors.clear();
        if (cleanCleanER) {
            if (entityId < datasetLimit) {
                for (int originalId : bBlocks[blockIndex].getIndex2Entities()) {
                    neighbors.add(originalId + datasetLimit);
                }
            } else {
                for (int originalId : bBlocks[blockIndex].getIndex1Entities()) {
                    neighbors.add(originalId);
                }
            }
        } else {
            if (!nodeCentric) {
                for (int neighborId : uBlocks[blockIndex].getEntities()) {
                    if (neighborId < entityId) {
                        neighbors.add(neighborId);
                    }
                }
            } else {
                for (int neighborId : uBlocks[blockIndex].getEntities()) {
                    if (neighborId != entityId) {
                        neighbors.add(neighborId);
                    }
                }
            }
        }
    }

    protected void setStatistics() {
        distinctComparisons = 0;
        comparisonsPerEntity = new float[noOfEntities];
        final TIntSet distinctNeighbors = new TIntHashSet();
        for (int i = 0; i < noOfEntities; i++) {
            final int[] associatedBlocks = entityIndex.getEntityBlocks(i, 0);
            if (associatedBlocks.length != 0) {
                distinctNeighbors.clear();
                for (int blockIndex : associatedBlocks) {
                    for (int neighborId : getNeighborEntities(blockIndex, i)) {
                        distinctNeighbors.add(neighborId);
                    }
                }
                comparisonsPerEntity[i] = distinctNeighbors.size();
                if (!cleanCleanER) {
                    comparisonsPerEntity[i]--;
                }
                distinctComparisons += comparisonsPerEntity[i];
            }
        }
        distinctComparisons /= 2;
    }
}
