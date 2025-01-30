/*
* Copyright [2016-2020 [George Papadakis (gpapadis@yahoo.gr)]
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

import org.scify.jedai.blockbuilding.IBlockBuilding;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.blockprocessing.IBlockProcessing;
import org.scify.jedai.utilities.datastructures.UnilateralDuplicatePropagation;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;
import java.util.List;
import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.blockbuilding.StandardBlocking;
import org.scify.jedai.blockprocessing.blockcleaning.BlockFiltering;
import org.scify.jedai.blockprocessing.blockcleaning.ComparisonsBasedBlockPurging;
import org.scify.jedai.blockprocessing.comparisoncleaning.CardinalityNodePruning;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.entityclustering.ConnectedComponentsClustering;
import org.scify.jedai.entitymatching.IEntityMatching;
import org.scify.jedai.entitymatching.ProfileMatcher;
import org.scify.jedai.utilities.BlocksPerformance;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 *
 * @author G.A.P. II
 */
public class ScalabilityBlockingBasedWorkflow {

    public static void main(String[] args) {
        BasicConfigurator.configure();

        String mainDir = "/home/gap2/data/JedAIdata/datasets/syntheticDatasets/";
        String[] datasets = {"10K", "50K", "100K", "200K", "300K", "1M", "2M"};

        int datasetId = Integer.parseInt(args[0]);
        String currentDataset = mainDir + datasets[datasetId] + "profiles";
        String currentGroundtruth = mainDir + datasets[datasetId] + "IdDuplicates";

        System.out.println("Current datase\t:\t" + currentDataset);

        IEntityReader eReader = new EntitySerializationReader(currentDataset);
        List<EntityProfile> profiles = eReader.getEntityProfiles();
        System.out.println("Input Entity Profiles\t:\t" + profiles.size());

        IGroundTruthReader gtReader = new GtSerializationReader(currentGroundtruth);
        final AbstractDuplicatePropagation duplicatePropagation = new UnilateralDuplicatePropagation(gtReader.getDuplicatePairs(eReader.getEntityProfiles()));
        System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

        long time1 = System.currentTimeMillis();

        IBlockBuilding blockBuildingMethod = new StandardBlocking();
        List<AbstractBlock> blocks = blockBuildingMethod.getBlocks(profiles, null);
        System.out.println("Original blocks\t:\t" + blocks.size());

        IBlockProcessing blockCleaningMethod1 = new ComparisonsBasedBlockPurging(false);
        blocks = blockCleaningMethod1.refineBlocks(blocks);

        IBlockProcessing blockCleaningMethod2 = new BlockFiltering();
        blocks = blockCleaningMethod2.refineBlocks(blocks);

        IBlockProcessing comparisonCleaningMethod = new CardinalityNodePruning(WeightingScheme.ARCS);
        blocks = comparisonCleaningMethod.refineBlocks(blocks);

        long time2 = System.currentTimeMillis();

        IEntityMatching pm = new ProfileMatcher(profiles, RepresentationModel.CHARACTER_BIGRAMS, SimilarityMetric.COSINE_SIMILARITY);
        SimilarityPairs simPairs = pm.executeComparisons(blocks);

        ConnectedComponentsClustering ccc = new ConnectedComponentsClustering();
        EquivalenceCluster[] entityClusters = ccc.getDuplicates(simPairs);

        long time3 = System.currentTimeMillis();

        BlocksPerformance blStats = new BlocksPerformance(blocks, duplicatePropagation);
        blStats.setStatistics();
        blStats.printStatistics(time2 - time1, "", "");

        ClustersPerformance clp = new ClustersPerformance(entityClusters, duplicatePropagation);
        clp.setStatistics();
        clp.printStatistics(time3 - time1, "", "");
        
        System.out.println("Running time\t:\t" + (time3-time1));
    }
}
