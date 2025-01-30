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
package org.scify.jedai.entityclustering;

import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityEdge;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.utilities.comparators.DecSimilarityEdgeComparator;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 *
 * @author G.A.P. II
 */
public class CenterClustering extends AbstractEntityClustering {
    private static final long serialVersionUID = 9098608992716054547L;

    public CenterClustering() {
        this(0.5f);
    }
    
    public CenterClustering(float simTh) {
        super(simTh);
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }
    
    @Override
    public EquivalenceCluster[] getDuplicates(SimilarityPairs simPairs) {
        initializeData(simPairs);
        
        final float[] edgesWeight = new float[noOfEntities];
        final float[] edgesAttached = new float[noOfEntities];
        final Queue<SimilarityEdge> SEqueue = new PriorityQueue<>(simPairs.getNoOfComparisons(), new DecSimilarityEdgeComparator());

        final Iterator<Comparison> iterator = simPairs.getPairIterator();
        // add a similarity edge to the queue for every pair of entities with a weight higher than the threshold
        while (iterator.hasNext()) { 
            final Comparison comparison = iterator.next();
            if (threshold < comparison.getUtilityMeasure()) {
                SEqueue.add(new SimilarityEdge(comparison.getEntityId1(), comparison.getEntityId2() + datasetLimit, comparison.getUtilityMeasure()));

                edgesWeight[comparison.getEntityId1()] += comparison.getUtilityMeasure();
                edgesWeight[comparison.getEntityId2() + datasetLimit] += comparison.getUtilityMeasure();

                edgesAttached[comparison.getEntityId1()]++;
                edgesAttached[comparison.getEntityId2() + datasetLimit]++;
            }
        }

        final TIntSet Center = new TIntHashSet();
        final TIntSet NonCenter = new TIntHashSet();
        while (!SEqueue.isEmpty()) {
            final SimilarityEdge se = SEqueue.remove();
            int v1 = se.getModel1Pos();
            int v2 = se.getModel2Pos();
            
            boolean v1IsCenter = Center.contains(v1);
            boolean v2IsCenter = Center.contains(v2);
            boolean v1IsNonCenter = NonCenter.contains(v1);
            boolean v2IsNonCenter = NonCenter.contains(v2);

            if (!(v1IsCenter || v2IsCenter || v1IsNonCenter || v2IsNonCenter)) {
                float w1 = edgesWeight[v1] / edgesAttached[v1];
                float w2 = edgesWeight[v2] / edgesAttached[v2];
                if (w1 > w2) {
                    Center.add(v1);
                    NonCenter.add(v2);
                } else {
                    Center.add(v2);
                    NonCenter.add(v1);
                }

                similarityGraph.addEdge(v1, v2);
            } else if ((v1IsCenter && v2IsCenter) || (v1IsNonCenter && v2IsNonCenter)) {
                continue;
            } else if (v1IsCenter && !v2IsNonCenter) {
                NonCenter.add(v2);
                similarityGraph.addEdge(v1, v2);
            } else if (v2IsCenter && !v1IsNonCenter) {
                NonCenter.add(v1);
                similarityGraph.addEdge(v1, v2);
            }
        }

        return getConnectedComponents();
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it implements the CENTER algorithm such that a vertex is set as a cluster center if it has highest average edge weight from its adjacent nodes.";
    }
    
    @Override
    public String getMethodName() {
        return "Center Clustering";
    }
}
