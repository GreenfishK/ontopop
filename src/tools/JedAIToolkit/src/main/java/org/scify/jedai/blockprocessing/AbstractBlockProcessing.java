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

package org.scify.jedai.blockprocessing;

import org.scify.jedai.datamodel.AbstractBlock;

import com.esotericsoftware.minlog.Log;

import java.util.List;

/**
 *
 * @author G.A.P. II
 */

public abstract class AbstractBlockProcessing implements IBlockProcessing {
    private static final long serialVersionUID = 9065475597657477586L;

    protected void printOriginalStatistics(List<AbstractBlock> inputBlocks) {
        long comparisons = inputBlocks.stream().mapToLong(AbstractBlock::getNoOfComparisons).sum();
        
        Log.info("Original blocks\t:\t" + inputBlocks.size());
        Log.info("Original comparisons\t:\t" + comparisons);
    }
}
