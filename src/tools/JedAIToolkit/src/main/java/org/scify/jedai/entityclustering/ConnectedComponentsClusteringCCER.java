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

import java.util.ArrayList;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityPairs;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author G.A.P. II
 */
public class ConnectedComponentsClusteringCCER extends AbstractEntityClustering {
    private static final long serialVersionUID = -2358377764782428809L;

    public ConnectedComponentsClusteringCCER() {
        this(0.5f);
    }
    
    public ConnectedComponentsClusteringCCER(float simTh) {
        super(simTh);
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }
    
    @Override
    public EquivalenceCluster[] getDuplicates(SimilarityPairs simPairs) {
        if (simPairs.getNoOfComparisons() == 0) {
            return new EquivalenceCluster[0];
        }
        
        initializeData(simPairs);
        
        // add an edge for every pair of entities with a weight higher than the threshold
        final Iterator<Comparison> iterator = simPairs.getPairIterator();
        while (iterator.hasNext()) {
            final Comparison comparison = iterator.next();
            if (threshold < comparison.getUtilityMeasure()) {
                similarityGraph.addEdge(comparison.getEntityId1(), comparison.getEntityId2() + datasetLimit);
            }
        }
        
        final EquivalenceCluster[] initialClusters = getConnectedComponents();
        final List<EquivalenceCluster> validClusters = new ArrayList<>();
        for (EquivalenceCluster cluster : initialClusters) {
            if (cluster.getEntityIdsD1().size() == 1 && cluster.getEntityIdsD2().size() == 1) {
                validClusters.add(cluster);
            }
        }
        
        return validClusters.toArray(new EquivalenceCluster[validClusters.size()]);
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it gets equivalence clsuters from the transitive closure of the similarity graph.";
    }

    @Override
    public String getMethodName() {
        return "Connected Components Clustering";
    }
}
