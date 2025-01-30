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
import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 *
 * @author gap2
 */
public class DetailedDBPediaPerformance {

    public static void main(String[] args) {
        BasicConfigurator.configure();

        float[] clusteringThreshold = {0.45f, 0.1f};
        RepresentationModel[] rModel = {RepresentationModel.TOKEN_UNIGRAMS_TF_IDF, RepresentationModel.CHARACTER_FOURGRAMS_TF_IDF};
        SimilarityMetric[] simMetric = {SimilarityMetric.SIGMA_SIMILARITY, SimilarityMetric.COSINE_SIMILARITY};

        String mainDir = "/home/gpapadakis/data/ccerDatasets/allDatasets/";
//        String mainDir = "/home/gpapadakis/data/datasets/";
//        String datasetsD1 = "newDBPedia1";
//        String datasetsD2 = "newDBPedia2";
        String datasetsD1 = "cleanDBPedia1";
        String datasetsD2 = "cleanDBPedia2";
        String groundtruthDirs = "newDBPediaMatches";

        IEntityReader eReader1 = new EntitySerializationReader(mainDir + datasetsD1);
        List<EntityProfile> profiles1 = eReader1.getEntityProfiles();
        System.out.println("NEW! Input Entity Profiles\t:\t" + profiles1.size());

        IEntityReader eReader2 = new EntitySerializationReader(mainDir + datasetsD2);
        List<EntityProfile> profiles2 = eReader2.getEntityProfiles();
        System.out.println("Input Entity Profiles\t:\t" + profiles2.size());

        IGroundTruthReader gtReader = new GtSerializationReader(mainDir + groundtruthDirs);
        final AbstractDuplicatePropagation duplicatePropagation = new BilateralDuplicatePropagation(gtReader.getDuplicatePairs(null));
        System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

        long time1 = System.currentTimeMillis();
        
        IBlockBuilding blockBuildingMethod = new StandardBlocking();
        List<AbstractBlock> blocks = blockBuildingMethod.getBlocks(profiles1, profiles2);
        System.out.println("Original blocks\t:\t" + blocks.size());

        IBlockProcessing blockCleaningMethod1 = new ComparisonsBasedBlockPurging(1.000f);
        blocks = blockCleaningMethod1.refineBlocks(blocks);

        IBlockProcessing blockCleaningMethod2 = new BlockFiltering();
        blocks = blockCleaningMethod2.refineBlocks(blocks);

        IBlockProcessing comparisonCleaningMethod = new CardinalityNodePruning(WeightingScheme.JS);
        blocks = comparisonCleaningMethod.refineBlocks(blocks);

        BlocksPerformance bp = new BlocksPerformance(blocks, duplicatePropagation);
        bp.setStatistics();
        bp.printStatistics(0, "", "");
        
        long time2 = System.currentTimeMillis();
        System.out.println("Blocking time\t:\t" + (time2-time1));
        
        int i = 0;
//        for (int i = 0; i < clusteringThreshold.length; i++) {
            long time3 = System.currentTimeMillis();
            
            final IEntityMatching em = new ProfileMatcher(profiles1, profiles2, rModel[i], simMetric[i]);
            final SimilarityPairs sims = em.executeComparisons(blocks);

            final IEntityClustering ec = new UniqueMappingClustering(clusteringThreshold[i]);
            final EquivalenceCluster[] clusters = ec.getDuplicates(sims);
            
            long time4 = System.currentTimeMillis();
            System.out.println("Run time " + (i+1) + "\t" + (time4-time3));
            
            final ClustersPerformance clp = new ClustersPerformance(clusters, duplicatePropagation);
            clp.setStatistics();
            clp.printStatistics(time4 - time3 + time2 - time1, "", "");
//        }
    }
}
