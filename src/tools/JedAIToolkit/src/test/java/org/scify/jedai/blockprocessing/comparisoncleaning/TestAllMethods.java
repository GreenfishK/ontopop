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

package org.scify.jedai.blockprocessing.comparisoncleaning;

import java.io.File;
import org.scify.jedai.blockbuilding.IBlockBuilding;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.blockprocessing.IBlockProcessing;
import org.scify.jedai.utilities.datastructures.UnilateralDuplicatePropagation;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.utilities.enumerations.BlockBuildingMethod;
import org.scify.jedai.utilities.BlocksPerformance;
import java.util.List;
import org.apache.log4j.BasicConfigurator;

/**
 *
 * @author G.A.P. II
 */

public class TestAllMethods {
    public static void main(String[] args) {
        BasicConfigurator.configure();
        
        String entitiesFilePath = "data" + File.separator + "dirtyErDatasets" + File.separator + "cddbProfiles";
        String groundTruthFilePath = "data" + File.separator + "dirtyErDatasets" + File.separator + "cddbIdDuplicates";
        
        IEntityReader eReader = new EntitySerializationReader(entitiesFilePath);
        List<EntityProfile> profiles = eReader.getEntityProfiles();
        System.out.println("Input Entity Profiles\t:\t" + profiles.size());
        
        IGroundTruthReader gtReader = new GtSerializationReader(groundTruthFilePath);
        final AbstractDuplicatePropagation duplicatePropagation = new UnilateralDuplicatePropagation(gtReader.getDuplicatePairs(eReader.getEntityProfiles()));
        System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());
        
        for (BlockBuildingMethod blbuMethod : BlockBuildingMethod.values()) {
            long time1 = System.currentTimeMillis();
            
            StringBuilder workflowConf = new StringBuilder();
            StringBuilder workflowName = new StringBuilder();
            
            System.out.println("\n\nCurrent blocking metohd\t:\t" + blbuMethod);
            
            IBlockBuilding blockBuildingMethod = BlockBuildingMethod.getDefaultConfiguration(blbuMethod);
            List<AbstractBlock> blocks = blockBuildingMethod.getBlocks(profiles, null);
            
            workflowConf.append(blockBuildingMethod.getMethodConfiguration());
            workflowName.append(blockBuildingMethod.getMethodName());
            System.out.println("Original blocks\t:\t" + blocks.size());
            
            IBlockProcessing blockCleaningMethod = BlockBuildingMethod.getDefaultBlockCleaning(blbuMethod);
            if (blockCleaningMethod != null) {
                blocks = blockCleaningMethod.refineBlocks(blocks);
                workflowConf.append("\n").append(blockCleaningMethod.getMethodConfiguration());
                workflowName.append("->").append(blockCleaningMethod.getMethodName());
            }
            
            IBlockProcessing comparisonCleaningMethod = BlockBuildingMethod.getDefaultComparisonCleaning(blbuMethod);
            if (comparisonCleaningMethod != null) {
                blocks = comparisonCleaningMethod.refineBlocks(blocks);
                workflowConf.append("\n").append(comparisonCleaningMethod.getMethodConfiguration());
                workflowName.append("->").append(comparisonCleaningMethod.getMethodName());
            }
            
            long time2 = System.currentTimeMillis();
            
            BlocksPerformance blStats = new BlocksPerformance(blocks, duplicatePropagation);
            blStats.setStatistics();
            blStats.printStatistics(time2-time1, workflowConf.toString(), workflowName.toString());
        }
    }
}
