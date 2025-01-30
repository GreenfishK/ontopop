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
package org.scify.jedai.schemaclustering;

import java.io.File;
import java.util.List;
import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.blockbuilding.IBlockBuilding;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.AttributeClusters;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.utilities.BlocksPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation;
import org.scify.jedai.utilities.enumerations.BlockBuildingMethod;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SchemaClusteringMethod;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;

/**
 *
 * @author GAP2
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
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "scholarProfiles"
        };

        String[] groundTruthFilePath = {"data" + File.separator + "cleanCleanErDatasets" + File.separator + "abtBuyIdDuplicates",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "amazonGpIdDuplicates",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "dblpAcmIdDuplicates",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "dblpScholarIdDuplicates",
        };

        for (int i = 0; i < groundTruthFilePath.length; i++) {
            System.out.println("\n\n\n\n\nCurrent dataset\t:\t" + groundTruthFilePath[i]);
            final EntitySerializationReader inReaderD1 = new EntitySerializationReader(entitiesFilePath[i * 2]);
            final List<EntityProfile> profilesD1 = inReaderD1.getEntityProfiles();
            System.out.println("Profiles D1\t:\t" + profilesD1.size());

            final EntitySerializationReader inReaderD2 = new EntitySerializationReader(entitiesFilePath[i * 2 + 1]);
            final List<EntityProfile> profilesD2 = inReaderD2.getEntityProfiles();
            System.out.println("Profiles D2\t:\t" + profilesD2.size());

            IGroundTruthReader gtReader = new GtSerializationReader(groundTruthFilePath[i]);
            final AbstractDuplicatePropagation duplicatePropagation = new BilateralDuplicatePropagation(gtReader.getDuplicatePairs(null));
            System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

            for (SchemaClusteringMethod scm : SchemaClusteringMethod.values()) {
                long time1 = System.currentTimeMillis();

                final ISchemaClustering sc = SchemaClusteringMethod.getModel(RepresentationModel.CHARACTER_TRIGRAMS, SimilarityMetric.ENHANCED_JACCARD_SIMILARITY, scm);
                final AttributeClusters[] clusters = sc.getClusters(profilesD1, profilesD2);

//                for (int j = 0; j < clusters.length; j++) {
//                    System.out.println("\nDataset\t:\t" + (j + 1));
//                    final TObjectIntIterator<String> it = clusters[j].iterator();
//                    while (it.hasNext()) {
//                        it.advance();
//                        System.out.println(it.key() + "\t" + it.value());
//                    }
//                }
                final IBlockBuilding bb = BlockBuildingMethod.getDefaultConfiguration(BlockBuildingMethod.STANDARD_BLOCKING);
                final List<AbstractBlock> blocks = bb.getBlocks(profilesD1, profilesD2, clusters);

                long time2 = System.currentTimeMillis();

                BlocksPerformance blStats = new BlocksPerformance(blocks, duplicatePropagation);
                blStats.setStatistics();
                blStats.printStatistics(time2 - time1, bb.getMethodConfiguration(), bb.getMethodName());
            }
        }
    }
}
