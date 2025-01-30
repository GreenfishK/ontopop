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
package org.scify.jedai.configuration.randomsearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author GAP2
 */
public abstract class AbstractRandomSearchConfiguration implements IRandomSearchConfiguration {
    private static final long serialVersionUID = -4086517295274043419L;

    protected final static Random RANDOM_GEN = new Random();
    
    protected final List<Object> selectedRandomValues;
    
    public AbstractRandomSearchConfiguration() {
        selectedRandomValues = new ArrayList<>();
    }
    
    @Override
    public Object getNextRandomValue() {
        Object nextRandomValue = randomValueGeneration();
        selectedRandomValues.add(nextRandomValue);
        return nextRandomValue;
    }
    
    @Override
    public Object getNumberedRandom(int iterationNumber) {
        if (0 <= iterationNumber && iterationNumber < selectedRandomValues.size()) {
            return selectedRandomValues.get(iterationNumber);
        }
        
        return null;
    }
   
    protected abstract Object randomValueGeneration();
}
