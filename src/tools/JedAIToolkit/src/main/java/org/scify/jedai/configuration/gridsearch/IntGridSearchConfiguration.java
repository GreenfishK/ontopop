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
package org.scify.jedai.configuration.gridsearch;

/**
 *
 * @author GAP2
 */
public class IntGridSearchConfiguration implements IGridSearchConfiguration {
    private static final long serialVersionUID = -3539335569080429407L;
    private final int maximumValue;
    private final int minimumValue;
    private final int step;
    
    public IntGridSearchConfiguration(int max, int min, int s) {
        step = s;
        maximumValue = max;
        minimumValue = min;
    }
    
    @Override
    public Object getNumberedValue(int iterationNumber) {
        return minimumValue + iterationNumber * step;
    }
    
    @Override
    public int getNumberOfConfigurations() {
        return (maximumValue - minimumValue) / step + 1;
    }
}
