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
package org.scify.jedai.configuration.version2_1;

import java.io.File;
import java.util.List;
import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.blockbuilding.IBlockBuilding;
import org.scify.jedai.blockbuilding.StandardBlocking;
import org.scify.jedai.blockprocessing.IBlockProcessing;
import org.scify.jedai.blockprocessing.blockcleaning.BlockFiltering;
import org.scify.jedai.blockprocessing.blockcleaning.SizeBasedBlockPurging;
import org.scify.jedai.blockprocessing.comparisoncleaning.CardinalityNodePruning;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.entityclustering.ConnectedComponentsClustering;
import org.scify.jedai.entityclustering.IEntityClustering;
import org.scify.jedai.entitymatching.IEntityMatching;
import org.scify.jedai.entitymatching.ProfileMatcher;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.UnilateralDuplicatePropagation;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 *
 * @author GAP2
 */
public class StepByStepRandomConfigurationDER {

    private final static float[] PURGING_FACTOR = {0.009f, 0.084f};
    private final static float[] FILTERING_RATIO = {0.283f, 0.520f};
    private final static float[] SIM_THRESHOLD = {0.758f, 0.576f};
    private final static WeightingScheme[] WEIGHTING_SCHEME = {WeightingScheme.EJS, WeightingScheme.CBS};
    private final static RepresentationModel[] REP_MODEL = {RepresentationModel.CHARACTER_BIGRAM_GRAPHS, RepresentationModel.CHARACTER_TRIGRAMS};
    private final static SimilarityMetric[] SIM_METRIC = {SimilarityMetric.GRAPH_NORMALIZED_VALUE_SIMILARITY, SimilarityMetric.ENHANCED_JACCARD_SIMILARITY};
    
    public static void main(String[] args) throws Exception {
        int i = Integer.parseInt(args[0]); //datasetIndex
        BasicConfigurator.configure();

        String mainDir = "data" + File.separator + "dirtyErDatasets" + File.separator;
        final String[] datasets = {"cddb", "cora"};

        System.out.println("\n\n\n\nCurrent dataset\t:\t" + mainDir + datasets[i] + "Profiles");

        final EntitySerializationReader inReader = new EntitySerializationReader(mainDir + datasets[i] + "Profiles");
        final List<EntityProfile> profiles = inReader.getEntityProfiles();
        System.out.println("\nNumber of entity profiles\t:\t" + profiles.size());

        final IGroundTruthReader gtReader = new GtSerializationReader(mainDir + datasets[i] + "IdDuplicates");
        final AbstractDuplicatePropagation duplicatePropagation = new UnilateralDuplicatePropagation(gtReader.getDuplicatePairs(profiles));
        System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

        final IBlockBuilding bb = new StandardBlocking();
        final IBlockProcessing bp1 = new SizeBasedBlockPurging(PURGING_FACTOR[i]);
        final IBlockProcessing bp2 = new BlockFiltering(FILTERING_RATIO[i]);
        final IBlockProcessing cc = new CardinalityNodePruning(WEIGHTING_SCHEME[i]);
        final IEntityMatching em = new ProfileMatcher(profiles, REP_MODEL[i], SIM_METRIC[i]);
        final IEntityClustering ec = new ConnectedComponentsClustering(SIM_THRESHOLD[i]);

        final StringBuilder matchingWorkflowName = new StringBuilder();
        matchingWorkflowName.append(bb.getMethodName());
        matchingWorkflowName.append("->").append(bp1.getMethodName());
        matchingWorkflowName.append("->").append(bp2.getMethodName());
        matchingWorkflowName.append("->").append(cc.getMethodName());
        matchingWorkflowName.append("->").append(em.getMethodName());
        matchingWorkflowName.append("->").append(ec.getMethodName());

        final StringBuilder matchingWorkflowConf = new StringBuilder();
        matchingWorkflowConf.append(bb.getMethodConfiguration());
        matchingWorkflowConf.append("\n").append(bp1.getMethodConfiguration());
        matchingWorkflowConf.append("\n").append(bp2.getMethodConfiguration());
        matchingWorkflowConf.append("\n").append(cc.getMethodConfiguration());
        matchingWorkflowConf.append("\n").append(em.getMethodConfiguration());
        matchingWorkflowConf.append("\n").append(ec.getMethodConfiguration());

        long time1 = System.currentTimeMillis();

        final List<AbstractBlock> blocks = bb.getBlocks(profiles);
        final List<AbstractBlock> purgedBlocks = bp1.refineBlocks(blocks);
        final List<AbstractBlock> filteredBlocks = bp2.refineBlocks(purgedBlocks);
        final List<AbstractBlock> finalBlocks = cc.refineBlocks(filteredBlocks);
        final SimilarityPairs sims = em.executeComparisons(finalBlocks);
        final EquivalenceCluster[] clusters = ec.getDuplicates(sims);

        long time2 = System.currentTimeMillis();

        final ClustersPerformance clp = new ClustersPerformance(clusters, duplicatePropagation);
        clp.setStatistics();
        clp.printStatistics(time2 - time1, matchingWorkflowName.toString(), matchingWorkflowConf.toString());

        System.out.println("Run-time\t:\t" + (time2 - time1));
    }
}
