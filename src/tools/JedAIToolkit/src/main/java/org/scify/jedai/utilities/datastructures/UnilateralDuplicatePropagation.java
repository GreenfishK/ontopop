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
package org.scify.jedai.utilities.datastructures;

import java.util.ArrayList;
import org.scify.jedai.datamodel.IdDuplicates;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.utilities.graph.ConnectedComponents;
import org.scify.jedai.utilities.graph.UndirectedGraph;

/**
 *
 * @author gap2
 */
public class UnilateralDuplicatePropagation extends AbstractDuplicatePropagation {

    private final Set<IdDuplicates> detectedDuplicates;

    public UnilateralDuplicatePropagation(Set<IdDuplicates> matches) {
        super(matches);
        detectedDuplicates = new HashSet<>(2 * matches.size());
    }

    private List<EquivalenceCluster> getClusters(UndirectedGraph similarityGraph) {
        final ConnectedComponents cc = new ConnectedComponents(similarityGraph);
        final EquivalenceCluster[] clustersArray = new EquivalenceCluster[cc.count()];
        for (int i = 0; i < similarityGraph.V(); i++) {
            if (cc.size(i) < 2) { // sigleton entity
                continue;
            }

            int clusterId = cc.id(i);
            if (clustersArray[clusterId] == null) {
                clustersArray[clusterId] = new EquivalenceCluster();
            }
            clustersArray[clusterId].addEntityIdD1(i);
        }

        final List<EquivalenceCluster> clustersList = new ArrayList<>();
        for (EquivalenceCluster equivalenceCluster : clustersArray) {
            if (equivalenceCluster != null) {
                clustersList.add(equivalenceCluster);
            }
        }
        return clustersList;
    }
    
    @Override
    public List<EquivalenceCluster> getDetectedEquivalenceClusters() {
        int noOfEntities = 0;
        for (IdDuplicates duplicatePair : detectedDuplicates) {
            noOfEntities = Math.max(noOfEntities, duplicatePair.getEntityId1());
            noOfEntities = Math.max(noOfEntities, duplicatePair.getEntityId2());
        }

        final UndirectedGraph similarityGraph = new UndirectedGraph(noOfEntities + 1);
        detectedDuplicates.forEach((duplicatePair) -> {
            similarityGraph.addEdge(duplicatePair.getEntityId1(), duplicatePair.getEntityId2());
        });

        return getClusters(similarityGraph);
    }

    @Override
    public Set<IdDuplicates> getFalseNegatives() {
        final Set<IdDuplicates> falseNegatives = new HashSet<>(duplicates);
        falseNegatives.removeAll(detectedDuplicates);
        for (IdDuplicates pair : detectedDuplicates) {
            falseNegatives.remove(new IdDuplicates(pair.getEntityId2(), pair.getEntityId1()));
        }
        return falseNegatives;
    }

    @Override
    public int getNoOfDuplicates() {
        return detectedDuplicates.size();
    }

    @Override
    public List<EquivalenceCluster> getRealEquivalenceClusters() {
        int noOfEntities = 0;
        for (IdDuplicates duplicatePair : duplicates) {
            noOfEntities = Math.max(noOfEntities, duplicatePair.getEntityId1());
            noOfEntities = Math.max(noOfEntities, duplicatePair.getEntityId2());
        }

        final UndirectedGraph similarityGraph = new UndirectedGraph(noOfEntities + 1);
        duplicates.forEach((duplicatePair) -> {
            similarityGraph.addEdge(duplicatePair.getEntityId1(), duplicatePair.getEntityId2());
        });

        return getClusters(similarityGraph);
    }

    @Override

    public boolean isSuperfluous(int entityId1, int entityId2) {
        final IdDuplicates duplicatePair1 = new IdDuplicates(entityId1, entityId2);
        final IdDuplicates duplicatePair2 = new IdDuplicates(entityId2, entityId1);
        
        if (detectedDuplicates.contains(duplicatePair1)
                || detectedDuplicates.contains(duplicatePair2)) {
            return true;
        }
        
        if (duplicates.contains(duplicatePair1)
                || duplicates.contains(duplicatePair2)) {
            if (entityId1 < entityId2) {
                detectedDuplicates.add(duplicatePair1);
            } else {
                detectedDuplicates.add(duplicatePair2);
            }
        }

        return false;
    }

    @Override
    public void resetDuplicates() {
        detectedDuplicates.clear();
    }
}
