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

import org.scify.jedai.datamodel.Comparison;
import java.util.Comparator;

/**
 *
 * @author G.A.P. II
 */

public class DecComparisonWeightComparator implements Comparator<Comparison> {

    /* 
    * This comparator orders comparisons in decreasing order of weight 
    * (utility measure), i.e., from the largest weight to the smallest one.
    * It is useful for Cardinality Node Pruning.
    */
    
    @Override
    public int compare(Comparison o1, Comparison o2) {
        float test = o1.getUtilityMeasure()-o2.getUtilityMeasure(); 
        if (0 < test) {
            return -1;
        }

        if (test < 0) {
            return 1;
        }

        return 0;
    }
    
//    public static void main (String[] args) {
//        Comparison c1 = new Comparison(true, 0, 1);
//        c1.setUtilityMeasure(0.5);
//        Comparison c2 = new Comparison(true, 0, 2);
//        c2.setUtilityMeasure(0.25);
//        Comparison c3 = new Comparison(true, 0, 3);
//        c3.setUtilityMeasure(0.75);
//        
//        List<Comparison> comparisons = new ArrayList<>();
//        comparisons.add(c1);
//        comparisons.add(c2);
//        comparisons.add(c3);
//        
//        Collections.sort(comparisons, new DecComparisonWeightComparator());
//        
//        comparisons.forEach((c) -> {
//            System.out.println(c.toString());
//        });
//    }
}