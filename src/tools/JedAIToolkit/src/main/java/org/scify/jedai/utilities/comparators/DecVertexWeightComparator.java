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

import org.scify.jedai.datamodel.VertexWeight;

import java.util.Comparator;

/**
 *
 * @author G.A.P. II
 */
public class DecVertexWeightComparator implements Comparator<VertexWeight> {

    /* 
    * This comparator sorts vertices in decreasing average weight per edge
    * i.e., from the largerst av. weight to the smallest one.
    * It is used by Ricochet SR Clustering.
     */
    @Override
    public int compare(VertexWeight vw1, VertexWeight vw2) {
        float w1 = 0;
        if (0 < vw1.getNoOfAdj()) {
            w1 = vw1.getWeight() / vw1.getNoOfAdj();
        }
        float w2 = 0;
        if (0 < vw2.getNoOfAdj()) {
            w2 = vw2.getWeight() / vw2.getNoOfAdj();
        }
        float test = w1 - w2;
        if (test > 0) {
            return -1;
        }

        if (test < 0) {
            return 1;
        }
        return 0;
    }

//    public static void main (String[] args) {
//        List<VertexWeight> vertices = new ArrayList<>();
//        vertices.add(new VertexWeight(0, 1.0, 5, null));
//        vertices.add(new VertexWeight(1, 1.0, 2, null));
//        vertices.add(new VertexWeight(2, 1.0, 4, null));
//        
//        Collections.sort(vertices, new DecVertexWeightComparator());
//        
//        for (VertexWeight vw : vertices) {
//            System.out.println(vw.toString());
//        }
//    }
}
