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

import com.esotericsoftware.minlog.Log;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityEdge;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.utilities.comparators.DecSimilarityEdgeComparator;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Implements an adapted, simplified version of the BEST algorithm, introduced
 * in "Similarity Flooding: A Versatile Graph Matching Algorithm and Its
 * Application to Schema Matching", also used in "BIGMAT: A Distributed
 * Affinity-Preserving Random Walk Strategy for Instance Matching on Knowledge
 * Graphs"
 *
 * @author vefthym
 */
public class BestMatchClustering extends AbstractCcerEntityClustering {
    private static final long serialVersionUID = -7108295963579068885L;
    private boolean leftDataset;

    public BestMatchClustering() {
        this(0.1f);
    }

    public BestMatchClustering(float simTh) {
        super(simTh);
        leftDataset = true;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }
    
    @Override
    public EquivalenceCluster[] getDuplicates(SimilarityPairs simPairs) {
        Log.info("Input comparisons\t:\t" + simPairs.getNoOfComparisons());

        matchedIds.clear();
        if (simPairs.getNoOfComparisons() == 0) {
            return new EquivalenceCluster[0];
        }

        initializeData(simPairs);
        if (!isCleanCleanER) {
            return null; //the method is only applicable to Clean-Clean ER
        }

        if (leftDataset) {
            // create as many PQs (initially empty) as the number of entities in collection 1
            Queue<SimilarityEdge>[] SEqueues = new PriorityQueue[datasetLimit];
            for (int i = 0; i < SEqueues.length; ++i) {
                SEqueues[i] = new PriorityQueue<>(new DecSimilarityEdgeComparator());
            }

            // add all candidates per node to the respective PQ of this node
            final Iterator<Comparison> iterator = simPairs.getPairIterator();
            while (iterator.hasNext()) { // add a similarity edge to the queue, for every pair of entities with a weight higher than the threshold
                Comparison comparison = iterator.next();
                int eid1 = comparison.getEntityId1();
                if (threshold < comparison.getUtilityMeasure()) {
                    SEqueues[eid1].add(new SimilarityEdge(eid1, comparison.getEntityId2() + datasetLimit, comparison.getUtilityMeasure()));
                }
            }

            //retrieve the top-1 candidate for every entity of the first collection
            for (int e1 = 0; e1 < datasetLimit; ++e1) {
                while (!SEqueues[e1].isEmpty()) {
                    final SimilarityEdge se = SEqueues[e1].remove();
                    int e2 = se.getModel2Pos();

                    //skip already matched entities (unique mapping constraint for clean-clean ER)
                    if (matchedIds.contains(e2)) {
                        continue;
                    }

                    similarityGraph.addEdge(e1, e2);
                    matchedIds.add(e1);
                    matchedIds.add(e2);
                    break; // go to next entity from collection 1
                }
            }
        } else {
            // create as many PQs (initially empty) as the number of entities in collection 2
            Queue<SimilarityEdge>[] SEqueues = new PriorityQueue[noOfEntities - datasetLimit];
            for (int i = 0; i < SEqueues.length; ++i) {
                SEqueues[i] = new PriorityQueue<>(new DecSimilarityEdgeComparator());
            }

            // add all candidates per node to the respective PQ of this node
            final Iterator<Comparison> iterator = simPairs.getPairIterator();
            while (iterator.hasNext()) { // add a similarity edge to the queue, for every pair of entities with a weight higher than the threshold
                Comparison comparison = iterator.next();
                int eid2 = comparison.getEntityId2();
                if (threshold < comparison.getUtilityMeasure()) {
                    SEqueues[eid2].add(new SimilarityEdge(comparison.getEntityId1(), eid2 + datasetLimit, comparison.getUtilityMeasure()));
                }
            }

            //retrieve the top-1 candidate for every entity of the second (right) collection
            for (int e2 = 0; e2 < noOfEntities - datasetLimit; ++e2) {
                while (!SEqueues[e2].isEmpty()) {
                    final SimilarityEdge se = SEqueues[e2].remove();
                    int e1 = se.getModel1Pos();

                    //skip already matched entities (unique mapping constraint for clean-clean ER)
                    if (matchedIds.contains(e1)) {
                        continue;
                    }

                    similarityGraph.addEdge(e1, e2 + datasetLimit);
                    matchedIds.add(e1);
                    matchedIds.add(e2 + datasetLimit);
                    break; // go to next entity from collection 2
                }
            }
        }
        
        return getConnectedComponents();
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it creates a cluster for each entity of collection 1 with its best match from collection 2";
    }

    @Override
    public String getMethodName() {
        return "BestMatch Clustering";
    }
    
    public void setDataset(boolean leftDataset) {
        this.leftDataset = leftDataset;
    }
}
