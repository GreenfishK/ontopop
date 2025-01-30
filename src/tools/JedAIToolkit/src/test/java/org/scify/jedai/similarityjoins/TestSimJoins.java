/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.jedai.similarityjoins;

import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.datamodel.Attribute;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.datawriter.ClustersPerformanceWriter;
import org.scify.jedai.entityclustering.IEntityClustering;
import org.scify.jedai.entityclustering.UniqueMappingClustering;
import org.scify.jedai.similarityjoins.tokenbased.PartEnumJoin;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

//import org.scify.jedai.similarityjoins.characterbased.AllPairs;

/**
 *
 * @author gap2
 */
public class TestSimJoins {

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

        /////////////////////////
        /*String file1 = null, file2 = null, outFile = null, statsFile = null, columnDelimiter = null,
                tokenDelimiter = null;
        int setCol = 0, tokenCol = 1, header = 1, maxLines = -1;
        float simThreshold = 1;

        try (InputStream input = new FileInputStream("C:\\Users\\Manos\\Documents\\UOA\\silkmoth-java\\silkmoth-java.git\\config.properties")) {
            Properties prop = new Properties();
            prop.load(input);

            file1 = prop.getProperty("input1");
            file2 = prop.getProperty("input2");
            outFile = prop.getProperty("output");
            statsFile = prop.getProperty("stats_file");
            setCol = Integer.parseInt(prop.getProperty("col_sets"));
            tokenCol = Integer.parseInt(prop.getProperty("col_elements"));
            columnDelimiter = prop.getProperty("column_delimiter");
            tokenDelimiter = prop.getProperty("token_delimiter");
            header = Integer.parseInt(prop.getProperty("has_header"));
            maxLines = Integer.parseInt(prop.getProperty("max_lines"));
            simThreshold = Double.parseDouble(prop.getProperty("sim_threshold"));
        } catch (Exception e) {
            throw new RuntimeException("ERROR: Wrong input parameters.", e);
        }

        *//* READ THE INPUT FILES *//*
        System.out.println("START READING INPUT...");
        InputReader reader = new InputReader();
        Map<String, List<Set<String>>> input1 = reader.importCollectionFromFile(file1, setCol, tokenCol,
                columnDelimiter, tokenDelimiter, header, maxLines);
        Map<String, List<Set<String>>> input2 = reader.importCollectionFromFile(file2, setCol, tokenCol,
                columnDelimiter, tokenDelimiter, header, maxLines);

//		System.out.println(input1.keySet().toString());
        // --for debugging--
        // Util.printCollection(input1, Math.min(10, input1.size()));
        // Util.printCollection(input2, Math.min(10, input2.size()));

        *//* INVOKE THE JOIN OPERATION *//*
        System.out.println("START JOIN EXECUTION...");
        FuzzySetSimJoin fssj = new FuzzySetSimJoin();
//		List<int[]> matchingPairs = fssj.join(input1, input2, simThreshold);
        HashMap<String, Double> matchingPairs = fssj.join(input1, input2, simThreshold);


        System.out.print("Writing results to file...   ");
        Util.writeMatchingPairs(matchingPairs, outFile, "", statsFile, input1.keySet().toArray(new String[input1.size()]), input2.keySet().toArray(new String[input1.size()]));
        System.out.println("Finished!");*/
        //////////////////////////////
        BasicConfigurator.configure();

        int datasetId = 3;//Integer.parseInt(args[0]);
        float jaccardThreshold = 0.3f;

        String mainDir = "C:\\Users\\Manos\\Documents\\UOA\\JedAIforAssembla\\JedAItk\\data\\cleanCleanErDatasets\\";
        String[] datasetsD1 = {"restaurant1Profiles", "abtProfiles", "amazonProfiles", "dblpProfiles", "walmartProfiles", "dblpProfiles2", "imdbProfiles"};
        String[] datasetsD2 = {"restaurant2Profiles", "buyProfiles", "gpProfiles", "acmProfiles", "amazonProfiles2", "scholarProfiles", "dbpediaProfiles"};
        String[] groundtruthDirs = {"restaurantsIdDuplicates", "abtBuyIdDuplicates", "amazonGpIdDuplicates", "dblpAcmIdDuplicates", "amazonWalmartIdDuplicates",
            "dblpScholarIdDuplicates", "moviesIdDuplicates"};

        List<EntityProfile> profiles1 = getDatasetAggregateValues(mainDir + datasetsD1[datasetId]);
        System.out.println("Input Entity Profiles\t:\t" + profiles1.size());

        List<EntityProfile> profiles2 = getDatasetAggregateValues(mainDir + datasetsD2[datasetId]);
        System.out.println("Input Entity Profiles\t:\t" + profiles2.size());

        IGroundTruthReader gtReader = new GtSerializationReader(mainDir + groundtruthDirs[datasetId]);
        final AbstractDuplicatePropagation duplicatePropagation = new BilateralDuplicatePropagation(gtReader.getDuplicatePairs(null));
        System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

        //AllPairs join = new AllPairs(3,15);
        //AllPairs join = new AllPairs(jaccardThreshold);
        //PassJoin join = new PassJoin(20);
        //PPJoin join = new PPJoin(jaccardThreshold);
        PartEnumJoin join = new PartEnumJoin(jaccardThreshold);
        //FuzzySetJoin join = new FuzzySetJoin(jaccardThreshold);
        //Topk join = new Topk(jaccardThreshold, 3);
        SimilarityPairs simPairs = join.executeFiltering("all", "all", profiles1, profiles2);

        final IEntityClustering ec = new UniqueMappingClustering(jaccardThreshold);

        final EquivalenceCluster[] clusters = ec.getDuplicates(simPairs);

        final ClustersPerformance clp = new ClustersPerformance(clusters, duplicatePropagation);
        clp.setStatistics();
        clp.printStatistics(0, "", "");

        final ClustersPerformanceWriter clpw = new ClustersPerformanceWriter(clusters, duplicatePropagation);
        try {
            clpw.printDetailedResultsToCSV(profiles1,profiles2, "C:\\Users\\Manos\\Desktop\\asd.csv");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
