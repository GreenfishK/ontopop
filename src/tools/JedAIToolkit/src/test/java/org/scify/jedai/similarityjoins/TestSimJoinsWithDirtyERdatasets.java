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
package org.scify.jedai.similarityjoins;

import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.datamodel.*;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.datawriter.ClustersPerformanceWriter;
import org.scify.jedai.entityclustering.ConnectedComponentsClustering;
import org.scify.jedai.entityclustering.IEntityClustering;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.UnilateralDuplicatePropagation;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.scify.jedai.similarityjoins.tokenbased.PPJoin;

/**
 *
 * @author G.A.P. II
 */
public class TestSimJoinsWithDirtyERdatasets {

    static List<EntityProfile> getDatasetAggregateValues(String filePath) {
        final IEntityReader eReader = new EntitySerializationReader(filePath);
        final List<EntityProfile> profiles = eReader.getEntityProfiles();
        System.out.println("\n\nInput Entity Profiles\t:\t" + profiles.size());

        final List<EntityProfile> newProfiles = new ArrayList<>();
        for (final EntityProfile oldProfile : profiles) {
            String totalValue = getAggregateValues(oldProfile);

            EntityProfile newProfile = new EntityProfile(oldProfile.getEntityUrl());
            newProfile.addAttribute("all", totalValue);
            newProfiles.add(newProfile);
        }

        return newProfiles;
    }

    static List<EntityProfile> getDatasetAggregateValues(List<EntityProfile> profiles) {
        System.out.println("\n\nInput Entity Profiles\t:\t" + profiles.size());

        final List<EntityProfile> newProfiles = new ArrayList<>();
        for (final EntityProfile oldProfile : profiles) {
            String totalValue = getAggregateValues(oldProfile);

            EntityProfile newProfile = new EntityProfile(oldProfile.getEntityUrl());
            newProfile.addAttribute("all", totalValue);
            newProfiles.add(newProfile);
        }

        return newProfiles;
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
        BasicConfigurator.configure();

        float jaccardThreshold = 0.45f;

        String entitiesFilePath = "data" + File.separator + "dirtyErDatasets" + File.separator + "coraProfiles";
        String groundTruthFilePath = "data" + File.separator + "dirtyErDatasets" + File.separator + "coraIdDuplicates";

//        EntityCSVReader testJedaiEntityReader;// = new EntityCSVReader(mainDirectory + "DBLP2.csv");

//        String mainDirectory = "C:\\Users\\Manos\\Documents\\UOA\\SimilarityJoins\\Similarity-Search-and-Join\\Zinp\\";

//        testJedaiEntityReader = new EntityCSVReader(mainDirectory + "testJedaiACMdplp.txt");
//        testJedaiEntityReader.setAttributeNamesInFirstRow(false);
//        //csvEntityReader.setIdIndex(0);
//        testJedaiEntityReader.setSeparator(';');
//        List<EntityProfile> csvACM = testJedaiEntityReader.getEntityProfiles();

//        List<EntityProfile> profiles = getDatasetAggregateValues(entitiesFilePath);
//        List<EntityProfile> profiles = getDatasetAggregateValues(csvACM);
//        System.out.println("Input Entity Profiles\t:\t" + profiles.size());

        IEntityReader eReader = new EntitySerializationReader(entitiesFilePath);
        List<EntityProfile> profiles = eReader.getEntityProfiles();
        System.out.println("Input Entity Profiles\t:\t" + profiles.size());

        IGroundTruthReader gtReader = new GtSerializationReader(groundTruthFilePath);
        final AbstractDuplicatePropagation duplicatePropagation = new UnilateralDuplicatePropagation(gtReader.getDuplicatePairs(eReader.getEntityProfiles()));
        System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

        PPJoin join = new PPJoin(jaccardThreshold);
//        SilkMoth join = new SilkMoth(jaccardThreshold);
        SimilarityPairs simPairs = join.executeFiltering("all", profiles);

        final IEntityClustering ec = new ConnectedComponentsClustering(jaccardThreshold);
        final EquivalenceCluster[] clusters = ec.getDuplicates(simPairs);

        final ClustersPerformance clp = new ClustersPerformance(clusters, duplicatePropagation);
        clp.setStatistics();
        clp.printStatistics(0, "", "");
        final ClustersPerformanceWriter clpw = new ClustersPerformanceWriter(clusters, duplicatePropagation);
        try {
            clpw.printDetailedResultsToCSV(profiles, null, "C:\\Users\\Manos\\Desktop\\asdBLOCK.csv");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
