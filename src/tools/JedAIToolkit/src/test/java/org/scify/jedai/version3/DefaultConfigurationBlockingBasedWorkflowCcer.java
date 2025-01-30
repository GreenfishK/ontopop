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
package org.scify.jedai.version3;

import java.util.List;
import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.blockbuilding.IBlockBuilding;
import org.scify.jedai.blockbuilding.StandardBlocking;
import org.scify.jedai.blockprocessing.IBlockProcessing;
import org.scify.jedai.blockprocessing.blockcleaning.BlockFiltering;
import org.scify.jedai.blockprocessing.blockcleaning.ComparisonsBasedBlockPurging;
import org.scify.jedai.blockprocessing.comparisoncleaning.CardinalityNodePruning;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.entityclustering.IEntityClustering;
import org.scify.jedai.entityclustering.UniqueMappingClustering;
import org.scify.jedai.entitymatching.IEntityMatching;
import org.scify.jedai.entitymatching.ProfileMatcher;
import org.scify.jedai.utilities.BlocksPerformance;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;

/**
 *
 * @author gap2
 */
public class DefaultConfigurationBlockingBasedWorkflowCcer {

    public static void main(String[] args) {
        BasicConfigurator.configure();

//        int datasetId = Integer.parseInt(args[0]);
        float clusteringThreshold = 0.1f;
        RepresentationModel repModel = RepresentationModel.CHARACTER_FOURGRAMS_TF_IDF;
        SimilarityMetric simMetric = SimilarityMetric.COSINE_SIMILARITY;

        String mainDir = "/home/gap2/data/JedAIdata/datasets/cleanCleanErDatasets/";
        String[] datasetsD1 = {"restaurant1Profiles", "abtProfiles", "amazonProfiles", "dblpProfiles", "walmartProfiles", "dblpProfiles2", "imdbProfiles"};
        String[] datasetsD2 = {"restaurant2Profiles", "buyProfiles", "gpProfiles", "acmProfiles", "amazonProfiles2", "scholarProfiles", "dbpediaProfiles"};
        String[] groundtruthDirs = {"restaurantsIdDuplicates", "abtBuyIdDuplicates", "amazonGpIdDuplicates", "dblpAcmIdDuplicates", "amazonWalmartIdDuplicates",
            "dblpScholarIdDuplicates", "moviesIdDuplicates"};

        for (int datasetId = 0; datasetId < groundtruthDirs.length; datasetId++) {
            System.out.println("\n\n\nCurrent dataset\t:\t" + datasetsD1[datasetId]);

            IEntityReader eReader1 = new EntitySerializationReader(mainDir + datasetsD1[datasetId]);
            List<EntityProfile> profiles1 = eReader1.getEntityProfiles();
            System.out.println("Input Entity Profiles\t:\t" + profiles1.size());

            IEntityReader eReader2 = new EntitySerializationReader(mainDir + datasetsD2[datasetId]);
            List<EntityProfile> profiles2 = eReader2.getEntityProfiles();
            System.out.println("Input Entity Profiles\t:\t" + profiles2.size());

            IGroundTruthReader gtReader = new GtSerializationReader(mainDir + groundtruthDirs[datasetId]);
            final AbstractDuplicatePropagation duplicatePropagation = new BilateralDuplicatePropagation(gtReader.getDuplicatePairs(null));
            System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

            long time1 = System.currentTimeMillis();

            final IBlockBuilding blockBuildingMethod = new StandardBlocking();
            List<AbstractBlock> blocks = blockBuildingMethod.getBlocks(profiles1, profiles2);

            final IBlockProcessing blockCleaningMethod1 = new ComparisonsBasedBlockPurging(1.00f);
            blocks = blockCleaningMethod1.refineBlocks(blocks);

            final IBlockProcessing blockCleaningMethod2 = new BlockFiltering();
            blocks = blockCleaningMethod2.refineBlocks(blocks);

            final IBlockProcessing comparisonCleaningMethod = new CardinalityNodePruning();
            blocks = comparisonCleaningMethod.refineBlocks(blocks);

            long time2 = System.currentTimeMillis();

            final IEntityMatching em = new ProfileMatcher(profiles1, profiles2, repModel, simMetric);
            final SimilarityPairs sims = em.executeComparisons(blocks);

            final IEntityClustering ec = new UniqueMappingClustering(clusteringThreshold);
            final EquivalenceCluster[] clusters = ec.getDuplicates(sims);

            long time3 = System.currentTimeMillis();

            final BlocksPerformance blStats = new BlocksPerformance(blocks, duplicatePropagation);
            blStats.setStatistics();
            blStats.printStatistics(time2 - time1, "", "");

            final ClustersPerformance clp = new ClustersPerformance(clusters, duplicatePropagation);
            clp.setStatistics();
            clp.printStatistics(time3 - time1, "", "");

            System.out.println("Running time\t:\t" + (time3 - time1));
        }
    }
}
