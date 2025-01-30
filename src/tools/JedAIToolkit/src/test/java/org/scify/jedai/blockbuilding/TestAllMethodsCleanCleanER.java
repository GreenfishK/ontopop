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

import java.io.File;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.utilities.enumerations.BlockBuildingMethod;
import org.scify.jedai.utilities.BlocksPerformance;
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation;
import java.util.List;
import org.apache.log4j.BasicConfigurator;

/**
 *
 * @author G.A.P. II
 */
public class TestAllMethodsCleanCleanER {

    public static void main(String[] args) {
        BasicConfigurator.configure();
        
        String[] entitiesFilePath = {"data" + File.separator + "cleanCleanErDatasets" + File.separator + "abtProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "buyProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "amazonProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "gpProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "dblpProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "acmProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "dblpProfiles2",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "scholarProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "imdbProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "dbpediaProfiles"
        };
        String[] groundTruthFilePath = {"data" + File.separator + "cleanCleanErDatasets" + File.separator + "abtBuyIdDuplicates",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "amazonGpIdDuplicates",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "dblpAcmIdDuplicates",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "dblpScholarIdDuplicates",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "moviesIdDuplicates"
        };

        for (int i = 0; i < groundTruthFilePath.length; i++) {
            IEntityReader eReader1 = new EntitySerializationReader(entitiesFilePath[i*2]);
            List<EntityProfile> profiles1 = eReader1.getEntityProfiles();
            System.out.println("Input Entity Profiles\t:\t" + profiles1.size());
            
            IEntityReader eReader2 = new EntitySerializationReader(entitiesFilePath[i*2+1]);
            List<EntityProfile> profiles2 = eReader2.getEntityProfiles();
            System.out.println("Input Entity Profiles\t:\t" + profiles2.size());

            IGroundTruthReader gtReader = new GtSerializationReader(groundTruthFilePath[i]);
            final AbstractDuplicatePropagation duplicatePropagation = new BilateralDuplicatePropagation(gtReader.getDuplicatePairs(null));
            System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

            for (BlockBuildingMethod blbuMethod : BlockBuildingMethod.values()) {
                long time1 = System.currentTimeMillis();

                System.out.println("\n\nCurrent blocking metohd\t:\t" + blbuMethod);
                IBlockBuilding blockBuildingMethod = BlockBuildingMethod.getDefaultConfiguration(blbuMethod);

                System.out.println("Block Building...");
                List<AbstractBlock> blocks = blockBuildingMethod.getBlocks(profiles1, profiles2);
                long time2 = System.currentTimeMillis();

                BlocksPerformance blStats = new BlocksPerformance(blocks, duplicatePropagation);
                blStats.setStatistics();
                blStats.printStatistics(time2 - time1, blockBuildingMethod.getMethodConfiguration(), blockBuildingMethod.getMethodName());
            }
        }
    }
}
