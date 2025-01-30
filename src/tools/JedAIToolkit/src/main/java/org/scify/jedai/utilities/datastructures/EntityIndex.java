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
package org.scify.jedai.utilities.datastructures;

import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.BilateralBlock;
import org.scify.jedai.datamodel.DecomposedBlock;
import org.scify.jedai.datamodel.UnilateralBlock;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author gap2
 */
public class EntityIndex implements Serializable {

    private static final long serialVersionUID = 13483254243447L;

    private boolean cleanCleanER;
    private int datasetLimit;
    private int noOfBlocks;
    private int noOfEntities;

    private float totalAssignments;
    private float totalComparisons;
    private float[] entityComparisons;
    private int[][] entityBlocks;

    private BilateralBlock[] bBlocks;
    private UnilateralBlock[] uBlocks;

    public EntityIndex(List<AbstractBlock> blocks) {
        if (blocks.isEmpty()) {
            System.err.println("Entity index received an empty block collection as input!");
            return;
        }

        if (blocks.get(0) instanceof DecomposedBlock) {
            System.err.println("The entity index is incompatible with a set of decomposed blocks!");
            System.err.println("Its functionalities can be carried out with same efficiency through a linear search of all comparisons!");
            return;
        }

        firstPass(blocks);
        if (cleanCleanER) {
            indexBilateralEntities();
        } else {
            indexUnilateralEntities();
        }
    }

    private void firstPass(List<AbstractBlock> blocks) {
        int counter = 0;
        totalAssignments = 0;
        totalComparisons = 0;
        noOfBlocks = blocks.size();
        cleanCleanER = blocks.get(0) instanceof BilateralBlock;
        if (cleanCleanER) {
            noOfEntities = Integer.MIN_VALUE;
            datasetLimit = Integer.MIN_VALUE;
            bBlocks = new BilateralBlock[noOfBlocks];
            for (AbstractBlock block : blocks) {
                bBlocks[counter] = (BilateralBlock) block;
                for (int id1 : bBlocks[counter].getIndex1Entities()) {
                    if (noOfEntities < id1 + 1) {
                        noOfEntities = id1 + 1;
                    }
                }

                for (int id2 : bBlocks[counter].getIndex2Entities()) {
                    if (datasetLimit < id2 + 1) {
                        datasetLimit = id2 + 1;
                    }
                }
                counter++;
                totalComparisons += block.getNoOfComparisons();
                totalAssignments += block.getTotalBlockAssignments();
            }

            int temp = noOfEntities;
            noOfEntities += datasetLimit;
            datasetLimit = temp;
        } else {
            noOfEntities = Integer.MIN_VALUE;
            datasetLimit = 0;
            uBlocks = new UnilateralBlock[noOfBlocks];
            for (AbstractBlock block : blocks) {
                uBlocks[counter] = (UnilateralBlock) block;
                for (int id : uBlocks[counter].getEntities()) {
                    if (noOfEntities < id + 1) {
                        noOfEntities = id + 1;
                    }
                }
                counter++;
                totalComparisons += block.getNoOfComparisons();
                totalAssignments += block.getTotalBlockAssignments();
            }
        }
    }

    public BilateralBlock[] getBilateralBlocks() {
        return bBlocks;
    }

    public int getDatasetLimit() {
        return datasetLimit;
    }

    public int[] getEntityBlocks(int entityId, int useDLimit) {
        entityId += useDLimit * datasetLimit;
        if (noOfEntities <= entityId) {
            return null;
        }
        return entityBlocks[entityId];
    }

    public float[] getEntityComparisons() {
        return entityComparisons;
    }
    
    public float getEntityComparisons(int entityId, int useDLimit) {
        entityId += useDLimit * datasetLimit;
        return entityComparisons[entityId];
    }

    public int getNoOfEntities() {
        return noOfEntities;
    }

    public int getNoOfEntityBlocks(int entityId, int useDLimit) {
        entityId += useDLimit * datasetLimit;
        if (entityBlocks[entityId] == null) {
            return 0;
        }

        return entityBlocks[entityId].length;
    }

    public UnilateralBlock[] getUnilateralBlocks() {
        return uBlocks;
    }
    
    public float getTotalAssignments() {
        return totalAssignments;
    }
    
    public float getTotalComparisons() {
        return totalComparisons;
    }

    public int[][] getWholeIndex() {
        return entityBlocks;
    }

    private void indexBilateralEntities() {
        final int[] counters = new int[noOfEntities];
        entityComparisons = new float[noOfEntities];
        for (BilateralBlock block : bBlocks) {
            int innerSize1 = block.getIndex1Entities().length;
            int innerSize2 = block.getIndex2Entities().length;
            for (int id1 : block.getIndex1Entities()) {
                counters[id1]++;
                entityComparisons[id1] += innerSize2;
            }

            for (int id2 : block.getIndex2Entities()) {
                int entityId = datasetLimit + id2;
                counters[entityId]++;
                entityComparisons[entityId] += innerSize1;
            }
        }

        //initialize inverted index
        entityBlocks = new int[noOfEntities][];
        for (int i = 0; i < noOfEntities; i++) {
            entityBlocks[i] = new int[counters[i]];
            counters[i] = 0;
        }

        //build inverted index
        int counter = 0;
        for (BilateralBlock block : bBlocks) {
            for (int id1 : block.getIndex1Entities()) {
                entityBlocks[id1][counters[id1]] = counter;
                counters[id1]++;
            }

            for (int id2 : block.getIndex2Entities()) {
                int entityId = datasetLimit + id2;
                entityBlocks[entityId][counters[entityId]] = counter;
                counters[entityId]++;
            }
            counter++;
        }
    }

    private void indexUnilateralEntities() {
        //count valid entities & blocks per entity
        final int[] counters = new int[noOfEntities];
        entityComparisons = new float[noOfEntities];
        for (UnilateralBlock block : uBlocks) {
            int blockSize = block.getEntities().length;
            for (int id : block.getEntities()) {
                counters[id]++;
                entityComparisons[id] += blockSize - 1;
            }
        }

        //initialize inverted index
        entityBlocks = new int[noOfEntities][];
        for (int i = 0; i < noOfEntities; i++) {
            entityBlocks[i] = new int[counters[i]];
            counters[i] = 0;
        }

        //build inverted index
        int counter = 0;
        for (UnilateralBlock block : uBlocks) {
            for (int id : block.getEntities()) {
                entityBlocks[id][counters[id]] = counter;
                counters[id]++;
            }
            counter++;
        }
    }

    public boolean isCleanCleanER() {
        return cleanCleanER;
    }
}
