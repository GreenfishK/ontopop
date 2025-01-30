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

import java.util.Iterator;
import org.scify.jedai.configuration.gridsearch.IntGridSearchConfiguration;
import org.scify.jedai.configuration.randomsearch.IntRandomSearchConfiguration;
import org.scify.jedai.datamodel.Comparison;

/**
 *
 * @author gap2
 */
public abstract class AbstractPrioritization implements IPrioritization {

    protected int comparisonsBudget;

    protected Iterator<Comparison> compIterator;
    
    protected final IntGridSearchConfiguration gridComparisonsBudget;
    protected final IntRandomSearchConfiguration randomComparisonsBudget;
    
    public AbstractPrioritization(int budget) {
        comparisonsBudget = budget;
        
        gridComparisonsBudget = new IntGridSearchConfiguration(1000000, 1000, 1000);
        randomComparisonsBudget = new IntRandomSearchConfiguration(1000000, 1000);
    }
}
