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

import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.datamodel.*;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.entityclustering.ConnectedComponentsClustering;
import org.scify.jedai.similarityjoins.tokenbased.PPJoin;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.datastructures.UnilateralDuplicatePropagation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author G_A.Papadakis
 */
public class ScalabilityJoinBasedWorkflow {

    static List<EntityProfile> getFlatProfiles(String filePath) {
        final IEntityReader eReader = new EntitySerializationReader(filePath);//;
        final List<EntityProfile> profiles = eReader.getEntityProfiles();
        System.out.println("\n\nInput Entity Profiles\t:\t" + profiles.size());

        final List<EntityProfile> flatProfiles = new ArrayList<>();
        for (final EntityProfile profile1 : profiles) {
            String totalValue = getAggregateValues(profile1);

            final EntityProfile newProfile = new EntityProfile(profile1.getEntityUrl());
            newProfile.addAttribute("all", totalValue);
            flatProfiles.add(newProfile);
        }

        return flatProfiles;
    }

    static String getAggregateValues(EntityProfile profile) {
        final StringBuilder sb = new StringBuilder();
        for (Attribute attribute : profile.getAttributes()) {
            String[] tokens = attribute.getValue().toLowerCase().split("[\\W_]");
            for (String token : tokens) {
                if (0 < token.trim().length()) {
                    sb.append(token).append(" ");
                }
            }
        }
        return sb.toString().trim();
    }

    public static void main(String[] args) {
        int datasetId = Integer.parseInt(args[0]);
//        String mainDir = "/home/gpapadakis/data/gridRandomConf/";
//        String mainDir = "/home/gap2/data/JedAIdata/datasets/syntheticDatasets/";
        String mainDir = "/home/gman/SparkER/data/Experiments/syntheticData/";
        String[] syntheticDatasets = {"10K", "50K", "100K", "200K", "300K", "1M", "2M"};

//        for (int datasetId = 0; datasetId < syntheticDatasets.length; datasetId++) {
        BasicConfigurator.configure();

        String datasetDir = syntheticDatasets[datasetId] + "profiles";
        String groundtruthDir = syntheticDatasets[datasetId] + "IdDuplicates";

        final List<EntityProfile> profiles = getFlatProfiles(mainDir + datasetDir);//inReader.getEntityProfiles();
        System.out.println("Profiles\t:\t" + profiles.size());

        final IGroundTruthReader gtReader = new GtSerializationReader(mainDir + groundtruthDir);
        final Set<IdDuplicates> duplicatePairs = gtReader.getDuplicatePairs(null);
        System.out.println("Duplicate pairs\t:\t" + duplicatePairs.size());

        long time1 = System.currentTimeMillis();

        float simThreshold = 0.40f;
        PPJoin ppjoin = new PPJoin(simThreshold);
        SimilarityPairs simPairs = ppjoin.executeFiltering("all", profiles);

        ConnectedComponentsClustering cc = new ConnectedComponentsClustering(simThreshold);
        EquivalenceCluster[] entityClusters = cc.getDuplicates(simPairs);

        long time2 = System.currentTimeMillis();

        ClustersPerformance clp = new ClustersPerformance(entityClusters, new UnilateralDuplicatePropagation(duplicatePairs));
        clp.setStatistics();
        clp.printStatistics(time2 - time1, "", "");

        System.out.println("Running time\t:\t" + (time2 - time1));
//        }
    }
}
