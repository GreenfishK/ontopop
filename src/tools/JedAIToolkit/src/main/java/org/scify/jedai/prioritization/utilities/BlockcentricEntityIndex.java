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
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import java.io.Serializable;
import java.util.List;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.BilateralBlock;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.ComparisonIterator;
import org.scify.jedai.datamodel.DecomposedBlock;
import org.scify.jedai.datamodel.UnilateralBlock;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 * @author gap2
 */
public class BlockcentricEntityIndex implements Serializable {

    private static final long serialVersionUID = 13483254243447L;

    private int datasetLimit;
    private int noOfEntities;
    private int validEntities1;
    private int validEntities2;
    private int[][] entityBlocks;

    private float totalBlocks;
    private float validComparisons;
    private float[] comparisonsPerBlock;
    private float[] comparisonsPerEntity;

    protected ChiSquareTest chiSquaredTest;
    private WeightingScheme wScheme;
    
    public BlockcentricEntityIndex(List<AbstractBlock> blocks, WeightingScheme wScheme) {
        if (blocks.isEmpty()) {
            System.err.println("Entity index received an empty block collection as input!");
            return;
        }

        if (blocks.get(0) instanceof DecomposedBlock) {
            System.err.println("The entity index is incompatible with a set of decomposed blocks!");
            System.err.println("Its functionalities can be carried out with same efficiency through a linear search of all comparisons!");
            return;
        }

        this.wScheme = wScheme;
        enumerateBlocks(blocks);
        setNoOfEntities(blocks);
        indexEntities(blocks);
        getStatistics(blocks);
        
        if (wScheme.equals(WeightingScheme.PEARSON_X2)) {
            chiSquaredTest = new ChiSquareTest();
        }
    }

    private void enumerateBlocks(List<AbstractBlock> blocks) {
        int blockIndex = 0;
        if (blocks.get(0).getBlockIndex() < 0) {
            for (AbstractBlock block : blocks) {
                block.setBlockIndex(blockIndex++);
            }
        }
    }

    TIntList getCommonBlockIndices(int blockIndex, Comparison comparison) {
        int[] blocks1 = entityBlocks[comparison.getEntityId1()];
        int[] blocks2 = entityBlocks[comparison.getEntityId2() + datasetLimit];

        boolean firstCommonIndex = false;
        int noOfBlocks1 = blocks1.length;
        int noOfBlocks2 = blocks2.length;
        final TIntList indices = new TIntArrayList();

        int i = 0;
        int j = 0;

        while (i < noOfBlocks1) {
            while (j < noOfBlocks2) {
                if (blocks2[j] < blocks1[i]) {
                    j++;
                } else if (blocks1[i] < blocks2[j]) {
                    break;
                } else { //blocks1[i] == blocks2[j]
                    if (!firstCommonIndex) {
                        firstCommonIndex = true;
                        if (blocks1[i] != blockIndex) {
                            return null;
                        }
                    }
                    indices.add(blocks1[i]);
                    j++;
                }
            }
            i++;
        }

        return indices;
    }

    int getDatasetLimit() {
        return datasetLimit;
    }

    int[] getEntityBlocks(int entityId, int useDLimit) {
        entityId += useDLimit * datasetLimit;
        if (noOfEntities <= entityId) {
            return null;
        }
        return entityBlocks[entityId];
    }

    int getNoOfCommonBlocks(int blockIndex, Comparison comparison) {
        int[] blocks1 = entityBlocks[comparison.getEntityId1()];
        int[] blocks2 = entityBlocks[comparison.getEntityId2() + datasetLimit];

        boolean firstCommonIndex = false;
        int commonBlocks = 0;
        int noOfBlocks1 = blocks1.length;
        int noOfBlocks2 = blocks2.length;

        int i = 0;
        int j = 0;

        while (i < noOfBlocks1) {
            while (j < noOfBlocks2) {
                if (blocks2[j] < blocks1[i]) {
                    j++;
                } else if (blocks1[i] < blocks2[j]) {
                    break;
                } else { //blocks1[i] == blocks2[j]
                    j++;
                    commonBlocks++;
                    if (!firstCommonIndex) {
                        firstCommonIndex = true;
                        if (blocks1[i] != blockIndex) {
                            return -1;
                        }
                    }
                }
            }
            i++;
        }
        return commonBlocks;
    }

    int getNoOfEntities() {
        return noOfEntities;
    }

    int getNoOfEntityBlocks(int entityId, int useDLimit) {
        entityId += useDLimit * datasetLimit;
        if (entityBlocks[entityId] == null) {
            return -1;
        }

        return entityBlocks[entityId].length;
    }

    private void getStatistics(List<AbstractBlock> blocks) {
        totalBlocks = blocks.size();
        comparisonsPerBlock = new float[(int) (totalBlocks + 1)];
        for (AbstractBlock block : blocks) {
            comparisonsPerBlock[block.getBlockIndex()] = block.getNoOfComparisons();
        }

        if (wScheme.equals(WeightingScheme.EJS)) {
            validComparisons = 0;
            comparisonsPerEntity = new float[noOfEntities];
            for (AbstractBlock block : blocks) {
                final ComparisonIterator iterator = block.getComparisonIterator();
                while (iterator.hasNext()) {
                    Comparison comparison = iterator.next();
                    int entityId2 = comparison.getEntityId2() + datasetLimit;

                    if (!isRepeated(block.getBlockIndex(), comparison)) {
                        validComparisons++;
                        comparisonsPerEntity[comparison.getEntityId1()]++;
                        comparisonsPerEntity[entityId2]++;
                    }
                }
            }
        }
    }

    TIntList getTotalCommonIndices(Comparison comparison) {
        final TIntList indices = new TIntArrayList();

        int[] blocks1 = entityBlocks[comparison.getEntityId1()];
        int[] blocks2 = entityBlocks[comparison.getEntityId2() + datasetLimit];
        if (blocks1.length == 0 || blocks2.length == 0) {
            return indices;
        }

        int noOfBlocks1 = blocks1.length;
        int noOfBlocks2 = blocks2.length;

        int i = 0;
        int j = 0;

        while (i < noOfBlocks1) {
            while (j < noOfBlocks2) {
                if (blocks2[j] < blocks1[i]) {
                    j++;
                } else if (blocks1[i] < blocks2[j]) {
                    break;
                } else { //blocks1[i] == blocks2[j]
                    i++;
                    indices.add(blocks1[i]);
                }
            }
            i++;
        }

        return indices;
    }

    int getTotalNoOfCommonBlocks(Comparison comparison) {
        int[] blocks1 = entityBlocks[comparison.getEntityId1()];
        int[] blocks2 = entityBlocks[comparison.getEntityId2() + datasetLimit];
        if (blocks1.length == 0 || blocks2.length == 0) {
            return 0;
        }

        int commonBlocks = 0;
        int noOfBlocks1 = blocks1.length;
        int noOfBlocks2 = blocks2.length;

        int i = 0;
        int j = 0;

        while (i < noOfBlocks1) {
            while (j < noOfBlocks2) {
                if (blocks2[j] < blocks1[i]) {
                    j++;
                } else if (blocks1[i] < blocks2[j]) {
                    break;
                } else { //blocks1[i] == blocks2[j]
                    j++;
                    commonBlocks++;
                }
            }
            i++;
        }

        return commonBlocks;
    }

    int getValidEntities1() {
        return validEntities1;
    }

    int getValidEntities2() {
        return validEntities2;
    }

    public float getWeight(int blockIndex, Comparison comparison) {
        switch (wScheme) {
            case ARCS:
                final TIntList commonIndices = getCommonBlockIndices(blockIndex, comparison);
                if (commonIndices == null) {
                    return -1;
                }

                float totalWeight = 0;
                for (TIntIterator tIterator = commonIndices.iterator(); tIterator.hasNext();) {
                    totalWeight += 1.0 / comparisonsPerBlock[tIterator.next()];
                }
                return totalWeight;
            case CBS:
                return getNoOfCommonBlocks(blockIndex, comparison);
            case ECBS:
                int commonBlocks = getNoOfCommonBlocks(blockIndex, comparison);
                if (commonBlocks < 0) {
                    return commonBlocks;
                }
                return (float)(commonBlocks * Math.log10(totalBlocks / getNoOfEntityBlocks(comparison.getEntityId1(), 0)) * Math.log10(totalBlocks / getNoOfEntityBlocks(comparison.getEntityId2(), comparison.isCleanCleanER() ? 1 : 0)));
            case JS:
                float commonBlocksJS = getNoOfCommonBlocks(blockIndex, comparison);
                if (commonBlocksJS < 0) {
                    return commonBlocksJS;
                }
                return commonBlocksJS / (getNoOfEntityBlocks(comparison.getEntityId1(), 0) + getNoOfEntityBlocks(comparison.getEntityId2(), comparison.isCleanCleanER() ? 1 : 0) - commonBlocksJS);
            case EJS:
                float commonBlocksEJS = getNoOfCommonBlocks(blockIndex, comparison);
                if (commonBlocksEJS < 0) {
                    return commonBlocksEJS;
                }

                float probability = commonBlocksEJS / (getNoOfEntityBlocks(comparison.getEntityId1(), 0) + getNoOfEntityBlocks(comparison.getEntityId2(), comparison.isCleanCleanER() ? 1 : 0) - commonBlocksEJS);
                return (float) (probability * Math.log10(validComparisons / comparisonsPerEntity[comparison.getEntityId1()]) * Math.log10(validComparisons / comparisonsPerEntity[comparison.isCleanCleanER() ? comparison.getEntityId2() + datasetLimit : comparison.getEntityId2()]));
            case PEARSON_X2:
                int commonBlocksPX = getNoOfCommonBlocks(blockIndex, comparison);
                if (commonBlocksPX < 0) {
                    return commonBlocksPX;
                }
                long[] v = new long[2];
                v[0] = commonBlocksPX;
                v[1] = getNoOfEntityBlocks(comparison.getEntityId1(), 0) - v[0];

                long[] v_ = new long[2];
                v_[0] = getNoOfEntityBlocks(comparison.getEntityId2(), comparison.isCleanCleanER() ? 1 : 0) - v[0];
                v_[1] = (int) (totalBlocks - (v[0] + v[1] + v_[0]));

                return (float) chiSquaredTest.chiSquare(new long[][]{v, v_});
        }

        return -1;
    }

    private void indexEntities(List<AbstractBlock> blocks) {
        if (blocks.get(0) instanceof BilateralBlock) {
            indexBilateralEntities(blocks);
        } else if (blocks.get(0) instanceof UnilateralBlock) {
            indexUnilateralEntities(blocks);
        }
    }

    private void indexBilateralEntities(List<AbstractBlock> blocks) {
        //count valid entities & blocks per entity
        validEntities1 = 0;
        validEntities2 = 0;
        int[] counters = new int[noOfEntities];
        for (AbstractBlock block : blocks) {
            BilateralBlock bilBlock = (BilateralBlock) block;
            for (int id1 : bilBlock.getIndex1Entities()) {
                if (counters[id1] == 0) {
                    validEntities1++;
                }
                counters[id1]++;
            }

            for (int id2 : bilBlock.getIndex2Entities()) {
                int entityId = datasetLimit + id2;
                if (counters[entityId] == 0) {
                    validEntities2++;
                }
                counters[entityId]++;
            }
        }

        //initialize inverted index
        entityBlocks = new int[noOfEntities][];
        for (int i = 0; i < noOfEntities; i++) {
            entityBlocks[i] = new int[counters[i]];
            counters[i] = 0;
        }

        //build inverted index
        for (AbstractBlock block : blocks) {
            BilateralBlock bilBlock = (BilateralBlock) block;
            for (int id1 : bilBlock.getIndex1Entities()) {
                entityBlocks[id1][counters[id1]] = block.getBlockIndex();
                counters[id1]++;
            }

            for (int id2 : bilBlock.getIndex2Entities()) {
                int entityId = datasetLimit + id2;
                entityBlocks[entityId][counters[entityId]] = block.getBlockIndex();
                counters[entityId]++;
            }
        }
    }

    private void indexUnilateralEntities(List<AbstractBlock> blocks) {
        //count valid entities & blocks per entity
        validEntities1 = 0;
        int[] counters = new int[noOfEntities];
        for (AbstractBlock block : blocks) {
            UnilateralBlock uniBlock = (UnilateralBlock) block;
            for (int id : uniBlock.getEntities()) {
                if (counters[id] == 0) {
                    validEntities1++;
                }
                counters[id]++;
            }
        }

        //initialize inverted index
        entityBlocks = new int[noOfEntities][];
        for (int i = 0; i < noOfEntities; i++) {
            entityBlocks[i] = new int[counters[i]];
            counters[i] = 0;
        }

        //build inverted index
        for (AbstractBlock block : blocks) {
            UnilateralBlock uniBlock = (UnilateralBlock) block;
            for (int id : uniBlock.getEntities()) {
                entityBlocks[id][counters[id]] = block.getBlockIndex();
                counters[id]++;
            }
        }
    }

    // So the linear time solution should be somthing like this:
    boolean isRepeated(int blockIndex, Comparison comparison) {
        int[] blocks1 = entityBlocks[comparison.getEntityId1()];
        int[] blocks2 = entityBlocks[comparison.getEntityId2() + datasetLimit];

        int noOfBlocks1 = blocks1.length;
        int noOfBlocks2 = blocks2.length;

        int i = 0;
        int j = 0;

        while (i < noOfBlocks1) {
            while (j < noOfBlocks2) {
                if (blocks2[j] < blocks1[i]) {
                    j++;
                } else if (blocks1[i] < blocks2[j]) {
                    break;
                } else if (blocks1[i] == blocks2[j]) {
                    return blocks1[i] != blockIndex;
                }
            }
            i++;
        }
        return false;
    }

    private void setNoOfEntities(List<AbstractBlock> blocks) {
        if (blocks.get(0) instanceof BilateralBlock) {
            setNoOfBilateralEntities(blocks);
        } else if (blocks.get(0) instanceof UnilateralBlock) {
            setNoOfUnilateralEntities(blocks);
        }
    }

    private void setNoOfBilateralEntities(List<AbstractBlock> blocks) {
        noOfEntities = Integer.MIN_VALUE;
        datasetLimit = Integer.MIN_VALUE;
        for (AbstractBlock block : blocks) {
            BilateralBlock bilBlock = (BilateralBlock) block;
            for (int id1 : bilBlock.getIndex1Entities()) {
                if (noOfEntities < id1 + 1) {
                    noOfEntities = id1 + 1;
                }
            }

            for (int id2 : bilBlock.getIndex2Entities()) {
                if (datasetLimit < id2 + 1) {
                    datasetLimit = id2 + 1;
                }
            }
        }

        int temp = noOfEntities;
        noOfEntities += datasetLimit;
        datasetLimit = temp;
    }

    private void setNoOfUnilateralEntities(List<AbstractBlock> blocks) {
        noOfEntities = Integer.MIN_VALUE;
        datasetLimit = 0;
        for (AbstractBlock block : blocks) {
            UnilateralBlock bilBlock = (UnilateralBlock) block;
            for (int id : bilBlock.getEntities()) {
                if (noOfEntities < id + 1) {
                    noOfEntities = id + 1;
                }
            }
        }
    }
}
