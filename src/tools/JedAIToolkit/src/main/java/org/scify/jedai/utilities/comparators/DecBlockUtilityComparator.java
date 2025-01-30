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
public class DecBlockUtilityComparator implements Comparator<AbstractBlock> {

   /* 
    * This comparator orders blocks in decreasing order of weight 
    * (utility measure), i.e., from the largest weight to the smallest one.
    * It is useful for Block Cleaning techniques.
    */
    
    @Override
    public int compare(AbstractBlock block1, AbstractBlock block2) {
        return Float.compare(block2.getUtilityMeasure(), block1.getUtilityMeasure());
    }

//    public static void main(String[] args) {
//        List<AbstractBlock> blocks = new ArrayList<>();
//        blocks.add(new UnilateralBlock(new int[]{1, 2, 3}));
//        blocks.add(new UnilateralBlock(new int[]{1, 2}));
//        blocks.add(new UnilateralBlock(new int[]{1, 2, 3, 4}));
//
//        int counter = 0;
//        for (AbstractBlock ab : blocks) {
//            ab.setUtilityMeasure();
//            ab.setBlockIndex(counter++);
//        }
//        Collections.sort(blocks, new DecBlockUtilityComparator());
//
//        for (AbstractBlock b : blocks) {
//            System.out.println(b.toString());
//        }
//    }
}
