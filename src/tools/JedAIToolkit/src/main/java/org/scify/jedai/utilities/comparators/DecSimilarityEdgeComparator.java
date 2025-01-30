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

import org.scify.jedai.datamodel.SimilarityEdge;
import java.util.Comparator;

/**
 *
 * @author G.A.P. II
 */

public class DecSimilarityEdgeComparator implements Comparator<SimilarityEdge> {
    
   /* 
    * This comparator orders edges of the similarity graph in decreasing order,
    * of weight, i.e., from the largest weight to the smallest one.
    * It is used by the clustering algorithms.
    */
    @Override
    public int compare(SimilarityEdge se1,SimilarityEdge se2) {
        float test = se1.getSimilarity()-se2.getSimilarity(); 
        if (test > 0) {
            return -1;
        }

        if (test < 0) {
            return 1;
        }

        return 0;
    }
    
//    public static void main (String[] args) {
//        List<SimilarityEdge> edges = new ArrayList<>();
//        edges.add(new SimilarityEdge(0, 1, 0.50));
//        edges.add(new SimilarityEdge(0, 1, 0.25));
//        edges.add(new SimilarityEdge(0, 1, 0.75));
//        
//        Collections.sort(edges, new DecSimilarityEdgeComparator());
//        
//        for (SimilarityEdge e : edges) {
//            System.out.println(e.toString());
//        }
//    }
}