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

import org.scify.jedai.configuration.IConfiguration;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.utilities.IConstants;
import org.scify.jedai.utilities.IDocumentation;

import java.util.Iterator;
import java.util.List;

/**
 *
 * @author gap2
 */
public interface IPrioritization extends IConfiguration, IConstants, IDocumentation, Iterator<Comparison> {
    
    void developBlockBasedSchedule(List<AbstractBlock> blocks);
    
    void developEntityBasedSchedule(List<EntityProfile> entitiesD1);
    
    void developEntityBasedSchedule(List<EntityProfile> entitiesD1, List<EntityProfile> entitiesD2);
}
