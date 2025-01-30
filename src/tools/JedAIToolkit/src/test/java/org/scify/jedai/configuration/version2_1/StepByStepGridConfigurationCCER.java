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
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.entityclustering.IEntityClustering;
import org.scify.jedai.entityclustering.UniqueMappingClustering;
import org.scify.jedai.entitymatching.IEntityMatching;
import org.scify.jedai.entitymatching.ProfileMatcher;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 *
 * @author GAP2
 */
public class StepByStepGridConfigurationCCER {

    private final static float[] PURGING_FACTOR = {0.016f, 0.021f, 0.006f, 0.011f};
    private final static float[] FILTERING_RATIO = {0.325f, 0.500f, 0.250f, 0.425f};
    private final static float[] SIM_THRESHOLD = {0.300f, 0.050f, 0.300f, 0.450f};
    private final static WeightingScheme[] WEIGHTING_SCHEME = {WeightingScheme.EJS, WeightingScheme.PEARSON_X2,
        WeightingScheme.EJS, WeightingScheme.EJS};
    private final static RepresentationModel[] REP_MODEL = {RepresentationModel.CHARACTER_TRIGRAMS_TF_IDF,
        RepresentationModel.TOKEN_BIGRAMS_TF_IDF, RepresentationModel.TOKEN_UNIGRAMS,
        RepresentationModel.CHARACTER_FOURGRAMS_TF_IDF};
    private final static SimilarityMetric[] SIM_METRIC = {SimilarityMetric.COSINE_SIMILARITY,
        SimilarityMetric.COSINE_SIMILARITY, SimilarityMetric.JACCARD_SIMILARITY,
        SimilarityMetric.SIGMA_SIMILARITY};

    public static void main(String[] args) throws Exception {
        int i = Integer.parseInt(args[0]); //datasetIndex
        BasicConfigurator.configure();

        String mainDirectory = "data";
        String[] entitiesFilePath = {mainDirectory + File.separator + "cleanCleanErDatasets" + File.separator + "abtProfiles",
            mainDirectory + File.separator + "cleanCleanErDatasets" + File.separator + "buyProfiles",
            mainDirectory + File.separator + "cleanCleanErDatasets" + File.separator + "amazonProfiles",
            mainDirectory + File.separator + "cleanCleanErDatasets" + File.separator + "gpProfiles",
            mainDirectory + File.separator + "cleanCleanErDatasets" + File.separator + "dblpProfiles",
            mainDirectory + File.separator + "cleanCleanErDatasets" + File.separator + "acmProfiles",
            mainDirectory + File.separator + "cleanCleanErDatasets" + File.separator + "dblpProfiles2",
            mainDirectory + File.separator + "cleanCleanErDatasets" + File.separator + "scholarProfiles",};
        String[] groundTruthFilePath = {mainDirectory + File.separator + "cleanCleanErDatasets" + File.separator + "abtBuyIdDuplicates",
            mainDirectory + File.separator + "cleanCleanErDatasets" + File.separator + "amazonGpIdDuplicates",
            mainDirectory + File.separator + "cleanCleanErDatasets" + File.separator + "dblpAcmIdDuplicates",
            mainDirectory + File.separator + "cleanCleanErDatasets" + File.separator + "dblpScholarIdDuplicates"
        };

        System.out.println("\n\n\n\nCurrent dataset\t:\t" + groundTruthFilePath[i]);

        IEntityReader eReader1 = new EntitySerializationReader(entitiesFilePath[i * 2]);
        List<EntityProfile> profiles1 = eReader1.getEntityProfiles();
        System.out.println("Input Entity Profiles\t:\t" + profiles1.size());

        IEntityReader eReader2 = new EntitySerializationReader(entitiesFilePath[i * 2 + 1]);
        List<EntityProfile> profiles2 = eReader2.getEntityProfiles();
        System.out.println("Input Entity Profiles\t:\t" + profiles2.size());

        IGroundTruthReader gtReader = new GtSerializationReader(groundTruthFilePath[i]);
        final AbstractDuplicatePropagation duplicatePropagation = new BilateralDuplicatePropagation(gtReader.getDuplicatePairs(null));
        System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

        final IBlockBuilding bb = new StandardBlocking();
        final IBlockProcessing bp1 = new SizeBasedBlockPurging(PURGING_FACTOR[i]);
        final IBlockProcessing bp2 = new BlockFiltering(FILTERING_RATIO[i]);
        final IBlockProcessing cc = new CardinalityNodePruning(WEIGHTING_SCHEME[i]);
        final IEntityMatching em = new ProfileMatcher(profiles1, profiles2, REP_MODEL[i], SIM_METRIC[i]);
        final IEntityClustering ec = new UniqueMappingClustering(SIM_THRESHOLD[i]);

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

        final List<AbstractBlock> blocks = bb.getBlocks(profiles1, profiles2);
        final List<AbstractBlock> purgedBlocks = bp1.refineBlocks(blocks);
        final List<AbstractBlock> filteredBlocks = bp2.refineBlocks(purgedBlocks);
        final List<AbstractBlock> finalBlocks = cc.refineBlocks(filteredBlocks);
        final SimilarityPairs sims = em.executeComparisons(finalBlocks);
        final EquivalenceCluster[] clusters = ec.getDuplicates(sims);

        long time2 = System.currentTimeMillis();

        System.out.println(clusters.length);
        final ClustersPerformance clp = new ClustersPerformance(clusters, duplicatePropagation);
        clp.setStatistics();
        clp.printStatistics(time2 - time1, matchingWorkflowName.toString(), matchingWorkflowConf.toString());

        System.out.println("Run-time\t:\t" + (time2 - time1));
    }
}
