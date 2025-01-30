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
 * Implements an adapted, simplified version of the Exact THRESHOLD algorithm,
 * introduced in "Similarity Flooding: A Versatile Graph Matching Algorithm and Its Application to Schema Matching",
 * also referred in "BIGMAT: A Distributed Affinity-Preserving Random Walk Strategy for Instance Matching on Knowledge Graphs".
 * In essence, it keeps the top-1 candidate per entity, as long as the candidate also considers this node as its top candidate.
 * @author vefthym
 */
public class ExactClustering extends AbstractCcerEntityClustering {
    private static final long serialVersionUID = 315372204805970171L;

    public ExactClustering() {
        this(0.1f);
    }

    public ExactClustering(float simTh) {
        super(simTh);
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

        // create as many PQs (initially empty) as the number of entities in collection 1
        Queue<SimilarityEdge>[] SEqueues1 = new PriorityQueue[datasetLimit];
        for (int i =0; i < SEqueues1.length; ++i) {
            SEqueues1[i] = new PriorityQueue<>(new DecSimilarityEdgeComparator());
        }

        // create as many PQs (initially empty) as the number of entities in collection 2
        Queue<SimilarityEdge>[] SEqueues2 = new PriorityQueue[noOfEntities-datasetLimit];
        for (int i =0; i < SEqueues2.length; ++i) {
            SEqueues2[i] = new PriorityQueue<>(new DecSimilarityEdgeComparator());
        }

        // add all candidates per node to the respective PQ of this node
        final Iterator<Comparison> iterator = simPairs.getPairIterator();
        while (iterator.hasNext()) { // add a similarity edge to the queue, for every pair of entities with a weight higher than the threshold
            Comparison comparison = iterator.next();
            int eid1 = comparison.getEntityId1();
            int eid2 = comparison.getEntityId2();
            if (threshold < comparison.getUtilityMeasure()) {
                //SimilarityEdge se = new SimilarityEdge(eid1, eid2 + datasetLimit, comparison.getUtilityMeasure());
                SEqueues1[eid1].add(new SimilarityEdge(eid1, eid2 + datasetLimit, comparison.getUtilityMeasure()));
                SEqueues2[eid2].add(new SimilarityEdge(eid1, eid2 + datasetLimit, comparison.getUtilityMeasure()));
            }
        }

        //retrieve the top-1 candidate for every entity of the first collection, if this edge is reciprocal top-1
        for (int e1 = 0; e1 < datasetLimit; ++e1) {
            if (SEqueues1[e1].isEmpty()) {
                continue;
            }
            final SimilarityEdge se1 = SEqueues1[e1].remove();

            int e2 = se1.getModel2Pos();

            final SimilarityEdge se2 = SEqueues2[e2-datasetLimit].remove();

            //skip non-reciprocal top-1 edges (only keep edges that are top-1 for both nodes)
            if (se2.getModel1Pos() != e1) {
                continue;
            }

            similarityGraph.addEdge(e1, e2);
            matchedIds.add(e1);
            matchedIds.add(e2);
        }

        return getConnectedComponents();
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it creates a cluster for each entity of collection 1 with its best match from collection 2, " +
                "only when both entities consider each other their best match.";
    }

    @Override
    public String getMethodName() {
        return "SymmetricBestMatch Clustering";
    }
}
