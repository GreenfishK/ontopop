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

import java.util.List;
import org.scify.jedai.datamodel.IdDuplicates;
import java.util.Set;
import org.scify.jedai.datamodel.EquivalenceCluster;

/**
 *
 * @author gap2
 */
public abstract class AbstractDuplicatePropagation {

    protected final int existingDuplicates;
    protected final Set<IdDuplicates> duplicates;

    public AbstractDuplicatePropagation(Set<IdDuplicates> matches) {
        duplicates = matches;
        existingDuplicates = duplicates.size();
    }

    public int getExistingDuplicates() {
        return existingDuplicates;
    }

    public abstract Set<IdDuplicates> getFalseNegatives();

    public abstract int getNoOfDuplicates();

    public abstract boolean isSuperfluous(int entityId1, int entityId2);

    public abstract void resetDuplicates();

    public abstract List<EquivalenceCluster> getDetectedEquivalenceClusters();

    public abstract List<EquivalenceCluster> getRealEquivalenceClusters();

    public Set<IdDuplicates> getDuplicates() {
        return duplicates;
    }
}
