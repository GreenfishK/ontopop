/*
* Copyright [2016-2018] [George Papadakis (gpapadis@yahoo.gr)]
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

package org.scify.jedai.blockprocessing.blockcleaning;

import org.scify.jedai.blockprocessing.AbstractBlockProcessing;
import org.scify.jedai.datamodel.AbstractBlock;

import com.esotericsoftware.minlog.Log;
import java.util.ArrayList;

import java.util.Iterator;
import java.util.List;

/**
 *
 * @author G.A.P. II
 */

public abstract class AbstractBlockPurging extends AbstractBlockProcessing {
    private static final long serialVersionUID = 9199463269624218907L;
    public AbstractBlockPurging() {
    }

    @Override
    public List<AbstractBlock> refineBlocks(List<AbstractBlock> blocks) {
        Log.info("Applying " + getMethodName() + " with the following configuration : " + getMethodConfiguration());
        
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return blocks;
        }
        
        List<AbstractBlock> newBlocks = new ArrayList<>(blocks);
        printOriginalStatistics(newBlocks);
        setThreshold(newBlocks);

        int noOfPurgedBlocks = 0;
        long totalComparisons = 0;
        final Iterator<AbstractBlock> blocksIterator = newBlocks.iterator();
        while (blocksIterator.hasNext()) {
            AbstractBlock aBlock = blocksIterator.next();
            if (!satisfiesThreshold(aBlock)) {
                noOfPurgedBlocks++;
                blocksIterator.remove();
            } else {
                totalComparisons += aBlock.getNoOfComparisons();
            }
        }
        
        Log.info("Purged blocks\t:\t" + noOfPurgedBlocks);
        Log.info("Retained blocks\t:\t" + blocks.size());
        Log.info("Retained comparisons\t:\t" + totalComparisons);

        return newBlocks;
    }
    
    protected abstract boolean satisfiesThreshold(AbstractBlock block);
    protected abstract void setThreshold(List<AbstractBlock> blocks);
}
