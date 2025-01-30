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

import org.scify.jedai.datamodel.IdDuplicates;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.scify.jedai.datamodel.EquivalenceCluster;

/**
 *
 * @author gap2
 */
public class BilateralDuplicatePropagation extends AbstractDuplicatePropagation {

    private final TIntSet entities1;
    private final TIntSet entities2;

    public BilateralDuplicatePropagation(Set<IdDuplicates> matches) {
        super(matches);
        entities1 = new TIntHashSet(2 * existingDuplicates);
        entities2 = new TIntHashSet(2 * existingDuplicates);
    }

    @Override
    public List<EquivalenceCluster> getDetectedEquivalenceClusters() {
        final List<EquivalenceCluster> eClusters = new ArrayList<>();
        duplicates.stream().filter((duplicatePair) -> (entities1.contains(duplicatePair.getEntityId1()))).map((duplicatePair) -> {
            final EquivalenceCluster cluster = new EquivalenceCluster();
            cluster.getEntityIdsD1().add(duplicatePair.getEntityId1());
            cluster.getEntityIdsD2().add(duplicatePair.getEntityId2());
            return cluster;
        }).forEachOrdered((cluster) -> {
            eClusters.add(cluster);
        });
        return eClusters;
    }

    @Override
    public Set<IdDuplicates> getFalseNegatives() {
        final Set<IdDuplicates> falseNegatives = new HashSet<>();
        duplicates.stream().filter((duplicatePair) -> (!entities1.contains(duplicatePair.getEntityId1()))).forEachOrdered((duplicatePair) -> {
            falseNegatives.add(duplicatePair);
        });
        return falseNegatives;
    }

    @Override
    public int getNoOfDuplicates() {
        return entities1.size();
    }

    @Override
    public List<EquivalenceCluster> getRealEquivalenceClusters() {
        final List<EquivalenceCluster> eClusters = new ArrayList<>();
        duplicates.stream().map((duplicatePair) -> {
            final EquivalenceCluster cluster = new EquivalenceCluster();
            cluster.getEntityIdsD1().add(duplicatePair.getEntityId1());
            cluster.getEntityIdsD2().add(duplicatePair.getEntityId2());
            return cluster;
        }).forEachOrdered((cluster) -> {
            eClusters.add(cluster);
        });
        return eClusters;
    }

    @Override
    public boolean isSuperfluous(int entityId1, int entityId2) {
        if (entities1.contains(entityId1) || entities2.contains(entityId2)) {
            return true;
        }

        final IdDuplicates tempDuplicates = new IdDuplicates(entityId1, entityId2);
        if (duplicates.contains(tempDuplicates)) {
            entities1.add(entityId1);
            entities2.add(entityId2);
        }
        return false;
    }

    @Override
    public void resetDuplicates() {
        entities1.clear();
        entities2.clear();
    }
}
