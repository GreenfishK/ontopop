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

import org.scify.jedai.blockprocessing.IBlockProcessing;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.BilateralBlock;
import org.scify.jedai.datamodel.DecomposedBlock;
import org.scify.jedai.datamodel.UnilateralBlock;
import org.scify.jedai.utilities.datastructures.EntityIndex;

import com.esotericsoftware.minlog.Log;

import gnu.trove.TIntCollection;
import gnu.trove.list.TIntList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.List;

/**
 *
 * @author gap2
 */

public abstract class AbstractComparisonCleaning implements IBlockProcessing {
    private static final long serialVersionUID = -215890142305328575L;

    protected boolean cleanCleanER;

    protected int datasetLimit;
    protected int noOfBlocks;
    protected int noOfEntities;

    protected EntityIndex entityIndex;
    protected BilateralBlock[] bBlocks;
    protected final TIntSet validEntities;
    protected UnilateralBlock[] uBlocks;

    public AbstractComparisonCleaning() {
        validEntities = new TIntHashSet();
    }

    protected void addDecomposedBlock(int entityId, TIntList neighbors, TIntList neighborWeights, List<AbstractBlock> newBlocks) {
        if (neighbors.isEmpty()) {
            return;
        }

        final int[] entityIds1 = replicateId(entityId, neighbors.size());
        newBlocks.add(new DecomposedBlock(cleanCleanER, entityIds1, neighbors.toArray(), neighborWeights.toArray()));
    }
    
    protected void addReversedDecomposedBlock(int entityId, TIntCollection neighbors, TIntCollection neighborWeights, List<AbstractBlock> newBlocks) {
        if (neighbors.isEmpty()) {
            return;
        }

        final int[] entityIds2 = replicateId(entityId, neighbors.size());
        newBlocks.add(new DecomposedBlock(cleanCleanER, neighbors.toArray(), entityIds2, neighborWeights.toArray()));
    }
    
    protected abstract List<AbstractBlock> applyMainProcessing();

    @Override
    public List<AbstractBlock> refineBlocks(List<AbstractBlock> blocks) {
        Log.info("Applying " + getMethodName() + " with the following configuration : " + getMethodConfiguration());
        
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return blocks;
        }
        
        entityIndex = new EntityIndex(blocks);
        
        cleanCleanER = entityIndex.isCleanCleanER();
        datasetLimit = entityIndex.getDatasetLimit();
        noOfBlocks = blocks.size();
        noOfEntities = entityIndex.getNoOfEntities();
        bBlocks = entityIndex.getBilateralBlocks();
        uBlocks = entityIndex.getUnilateralBlocks();

        return applyMainProcessing();
    }
    
    protected int[] replicateId(int entityId, int times) {
        int counter = 0;
        final int[] array = new int[times];
        for (int i = 0; i < times; i++) {
            array[counter++] = entityId;
        }
        return array;
    }
}
