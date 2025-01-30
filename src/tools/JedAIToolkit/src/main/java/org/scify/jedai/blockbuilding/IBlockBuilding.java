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
package org.scify.jedai.blockbuilding;

import org.scify.jedai.configuration.IConfiguration;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.AttributeClusters;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.utilities.IConstants;
import org.scify.jedai.utilities.IDocumentation;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author G.A.P. II
 */
public interface IBlockBuilding extends IConfiguration, IConstants, IDocumentation, Serializable {

    String CLUSTER_PREFIX = "#$!cl";
    String CLUSTER_SUFFIX = "#$!en";
    
    List<AbstractBlock> getBlocks(List<EntityProfile> profiles);

    List<AbstractBlock> getBlocks(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2);

    List<AbstractBlock> getBlocks(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, AttributeClusters[] schemaClusters);
    
}
