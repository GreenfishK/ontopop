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

package org.scify.jedai.prioritization;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.util.List;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.prioritization.utilities.PositionIndex;
import org.scify.jedai.prioritization.utilities.SortedEntities;
import org.scify.jedai.utilities.enumerations.ProgressiveWeightingScheme;

/**
 *
 * @author gap2
 */
public abstract class AbstractSimilarityBasedPrioritization extends AbstractPrioritization {

    protected boolean isCleanCleanER;

    protected int datasetLimit;
    protected int emittedComparisons;
    protected int noOfEntities;

    protected int[] counters;
    protected int[] flags;
    protected int[] sortedEntityIds;

    protected PositionIndex positionIndex;
    protected final ProgressiveWeightingScheme pwScheme;
    protected final TIntSet distinctNeighbors;

    public AbstractSimilarityBasedPrioritization(int budget, ProgressiveWeightingScheme pwScheme) {
        super(budget);
        distinctNeighbors = new TIntHashSet();
        this.pwScheme = pwScheme;
    }

    @Override
    public void developBlockBasedSchedule(List<AbstractBlock> blocks) {
    }
    
    @Override
    public void developEntityBasedSchedule(List<EntityProfile> profilesD1) {
        developEntityBasedSchedule(profilesD1, null);
    }
    @Override
    public void developEntityBasedSchedule(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2) {
        emittedComparisons = 0;
        isCleanCleanER = profilesD2 != null;
        noOfEntities = isCleanCleanER ? profilesD1.size() + profilesD2.size(): profilesD1.size();
        datasetLimit = isCleanCleanER ? profilesD1.size() : 0;

        SortedEntities se = new SortedEntities();
        se.getBlocks(profilesD1, profilesD2);
        sortedEntityIds = se.getSortedEntityIds();
        
        counters = new int[noOfEntities];
        flags = new int[noOfEntities];
        positionIndex = new PositionIndex(noOfEntities, sortedEntityIds);
    }
}
