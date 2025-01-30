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
import org.scify.jedai.entityclustering.ConnectedComponentsClustering;
import org.scify.jedai.entityclustering.IEntityClustering;
import org.scify.jedai.entitymatching.IEntityMatching;
import org.scify.jedai.entitymatching.ProfileMatcher;
import org.scify.jedai.utilities.BlocksPerformance;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.UnilateralDuplicatePropagation;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 *
 * @author gap2
 */
public class DefaultConfigurationBlockingBasedWorkflowDer {

    public static void main(String[] args) {
        BasicConfigurator.configure();

//        int datasetId = Integer.parseInt(args[0]);
//        int defaultConfiguration = 221;

        float clusteringThreshold = 0.65f;
        RepresentationModel repModel = RepresentationModel.CHARACTER_BIGRAM_GRAPHS;
        SimilarityMetric simMetric = SimilarityMetric.GRAPH_VALUE_SIMILARITY;

        String mainDir = "/home/gap2/data/JedAIdata/datasets/dirtyErDatasets/";
        String[] profilesFile = {"cddbProfiles", "coraProfiles"};
        String[] groundtruthFile = {"cddbIdDuplicates", "coraIdDuplicates"};

        for (int datasetId = 0; datasetId < groundtruthFile.length; datasetId++) {
            System.out.println("\n\n\nCurrent dataset\t:\t" + profilesFile[datasetId]);

            IEntityReader eReader = new EntitySerializationReader(mainDir + profilesFile[datasetId]);
            List<EntityProfile> profiles = eReader.getEntityProfiles();
            System.out.println("Input Entity Profiles\t:\t" + profiles.size());

            IGroundTruthReader gtReader = new GtSerializationReader(mainDir + groundtruthFile[datasetId]);
            final AbstractDuplicatePropagation duplicatePropagation = new UnilateralDuplicatePropagation(gtReader.getDuplicatePairs(null));
            System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

            long time1 = System.currentTimeMillis();

            final IBlockBuilding blockBuildingMethod = new StandardBlocking();
            List<AbstractBlock> blocks = blockBuildingMethod.getBlocks(profiles);

            final IBlockProcessing blockCleaningMethod1 = new ComparisonsBasedBlockPurging(false);
            blocks = blockCleaningMethod1.refineBlocks(blocks);

            final IBlockProcessing blockCleaningMethod2 = new BlockFiltering();
            blocks = blockCleaningMethod2.refineBlocks(blocks);

            final IBlockProcessing comparisonCleaningMethod = new CardinalityNodePruning(WeightingScheme.JS);
            blocks = comparisonCleaningMethod.refineBlocks(blocks);

//        final IEntityClustering ec = new ConnectedComponentsClustering();
//        ec.setNumberedGridConfiguration(defaultConfiguration % ec.getNumberOfGridConfigurations());
//        final IEntityMatching em = new ProfileMatcher(profiles);
//        em.setNumberedGridConfiguration(defaultConfiguration / ec.getNumberOfGridConfigurations());

            long time2 = System.currentTimeMillis();

            final IEntityMatching em = new ProfileMatcher(profiles, repModel, simMetric);
            final SimilarityPairs sims = em.executeComparisons(blocks);

            final IEntityClustering ec = new ConnectedComponentsClustering(clusteringThreshold);
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
