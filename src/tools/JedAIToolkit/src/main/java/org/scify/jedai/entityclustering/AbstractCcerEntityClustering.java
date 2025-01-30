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

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 *
 * @author gap2
 */
public abstract class AbstractCcerEntityClustering extends AbstractEntityClustering {
    private static final long serialVersionUID = -3807607496823228662L;
    protected final TIntSet matchedIds; //the ids of entities that have been already matched
    
    public AbstractCcerEntityClustering(float simTh) {
        super(simTh);
        matchedIds = new TIntHashSet();
    }
    
    @Override
    public void setNextRandomConfiguration() {
        matchedIds.clear();
        super.setNextRandomConfiguration();
    }

    @Override
    public void setNumberedGridConfiguration(int iterationNumber) {
        matchedIds.clear();
        super.setNumberedGridConfiguration(iterationNumber);
    }

    @Override
    public void setNumberedRandomConfiguration(int iterationNumber) {
        matchedIds.clear();
        super.setNumberedRandomConfiguration(iterationNumber);
    }
}
