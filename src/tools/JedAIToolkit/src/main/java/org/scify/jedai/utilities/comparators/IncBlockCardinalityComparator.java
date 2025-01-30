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
package org.scify.jedai.utilities.comparators;

import org.scify.jedai.datamodel.AbstractBlock;
import java.util.Comparator;

/**
 *
 * @author gap2
 */
public class IncBlockCardinalityComparator implements Comparator<AbstractBlock> {

    /* 
    * This comparator orders blocks in increasing order of cardinality, i.e.,
    * from the smallest number of comparisons to the largest one.
    * It is useful for Block Cleaning techniques.
    * Blocks with the same number of comparisons are ordered in decreasing aggregate comparison weight.
     */
    @Override
    public int compare(AbstractBlock block1, AbstractBlock block2) {
        return Float.compare(block1.getNoOfComparisons(), block2.getNoOfComparisons());
    }

//    public static void main(String[] args) {
//        List<AbstractBlock> blocks = new ArrayList<>();
//        blocks.add(new BilateralBlock(new int[]{1, 2, 3}, new int[]{10, 11}));
//        blocks.add(new BilateralBlock(new int[]{1, 2}, new int[]{10, 12}));
//        blocks.add(new BilateralBlock(new int[]{1, 2, 3, 4}, new int[]{10}));
//
//        int counter = 0;
//        for (AbstractBlock ab : blocks) {
//            ab.setUtilityMeasure();
//            ab.setBlockIndex(counter++);
//        }
//        Collections.sort(blocks, new IncBlockCardinalityComparator());
//
//        for (AbstractBlock b : blocks) {
//            System.out.println(b.toString());
//        }
//    }
}
