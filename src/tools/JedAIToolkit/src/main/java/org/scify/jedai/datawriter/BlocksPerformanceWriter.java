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
package org.scify.jedai.datawriter;

import com.esotericsoftware.minlog.Log;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.scify.jedai.blockprocessing.comparisoncleaning.ComparisonPropagation;
import org.scify.jedai.datamodel.*;
import org.scify.jedai.utilities.DBUtils;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.GroundTruthIndex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

/**
 *
 * @author gap2
 */
public class BlocksPerformanceWriter {

    private boolean isCleanCleanER;

    private int noOfD1Entities;
    private int noOfD2Entities;
    private int detectedDuplicates;

    private float aggregateCardinality;
    private float blockAssignments;
    private float d1BlockAssignments;
    private float d2BlockAssignments;
    private float fMeasure;
    private float pc;
    private float pq;

    private final AbstractDuplicatePropagation abstractDP;
    private final List<AbstractBlock> blocks;
    private GroundTruthIndex entityIndex;

    private String dbpassword;
    private String dbtable;
    private String dbuser;
    private boolean ssl;
    private String endpointURL;
    private String endpointGraph;

    public BlocksPerformanceWriter(List<AbstractBlock> bl, AbstractDuplicatePropagation adp) {
        abstractDP = adp;
        abstractDP.resetDuplicates();
        blocks = bl;
    }

    public void setPassword(String password) {
        this.dbpassword = password;
    }

    public void setTable(String table) {
        this.dbtable = table;
    }

    public void setUser(String user) {
        this.dbuser = user;
    }

    public void setSSL(boolean ssl) {
        this.ssl = ssl;
    }

    public void setEndpointURL(String endpointURL) {
        this.endpointURL = endpointURL;
    }

    public void setEndpointGraph(String endpointGraph) {
        this.endpointGraph = endpointGraph;
    }

    private boolean areCooccurring(boolean cleanCleanER, IdDuplicates pairOfDuplicates) {
        final int[] blocks1 = entityIndex.getEntityBlocks(pairOfDuplicates.getEntityId1(), 0);
        if (blocks1 == null) {
            return false;
        }

        final int[] blocks2 = entityIndex.getEntityBlocks(pairOfDuplicates.getEntityId2(), cleanCleanER ? 1 : 0);
        if (blocks2 == null) {
            return false;
        }

        for (int item : blocks1) {
            for (int value : blocks2) {
                if (value < item) {
                    continue;
                }

                if (item < value) {
                    break;
                }

                if (item == value) {
                    return true;
                }
            }
        }

        return false;
    }

    public float getAggregateCardinality() {
        return aggregateCardinality;
    }

    public float getBlockAssignments() {
        return blockAssignments;
    }

    public float getD1BlockAssignments() {
        return d1BlockAssignments;
    }

    public float getD2BlockAssignments() {
        return d2BlockAssignments;
    }

    public float getPc() {
        return pc;
    }

    public float getPq() {
        return pq;
    }

    private void getBilateralBlockingCardinality() {
        d1BlockAssignments = 0;
        d2BlockAssignments = 0;
        for (AbstractBlock block : blocks) {
            BilateralBlock bilBlock = (BilateralBlock) block;
            d1BlockAssignments += bilBlock.getIndex1Entities().length;
            d2BlockAssignments += bilBlock.getIndex2Entities().length;
        }
    }

    private void getDecomposedBlocksEntities() {
        final TIntSet entitiesD1 = new TIntHashSet((int) aggregateCardinality);
        if (isCleanCleanER) {
            final TIntSet entitiesD2 = new TIntHashSet((int) aggregateCardinality);
            for (AbstractBlock block : blocks) {
                final ComparisonIterator iterator = block.getComparisonIterator();
                while (iterator.hasNext()) {
                    Comparison comparison = iterator.next();
                    entitiesD1.add(comparison.getEntityId1());
                    entitiesD2.add(comparison.getEntityId2());
                }
            }
            noOfD1Entities = entitiesD1.size();
            noOfD2Entities = entitiesD2.size();
        } else {
            for (AbstractBlock block : blocks) {
                final ComparisonIterator iterator = block.getComparisonIterator();
                while (iterator.hasNext()) {
                    Comparison comparison = iterator.next();
                    entitiesD1.add(comparison.getEntityId1());
                    entitiesD1.add(comparison.getEntityId2());
                }
            }
            noOfD1Entities = entitiesD1.size();
        }
    }

    public int getDetectedDuplicates() {
        return detectedDuplicates;
    }

    private void getDuplicatesOfDecomposedBlocks() {
        for (AbstractBlock block : blocks) {
            ComparisonIterator iterator = block.getComparisonIterator();
            while (iterator.hasNext()) {
                final Comparison comp = iterator.next();
                abstractDP.isSuperfluous(comp.getEntityId1(), comp.getEntityId2());
            }
        }

        detectedDuplicates = abstractDP.getNoOfDuplicates();
        pc = ((float) abstractDP.getNoOfDuplicates()) / abstractDP.getExistingDuplicates();
        pq = abstractDP.getNoOfDuplicates() / aggregateCardinality;
        if (0 < pc && 0 < pq) {
            fMeasure = 2 * pc * pq / (pc + pq);
        } else {
            fMeasure = 0;
        }
    }

    private void getDuplicatesWithEntityIndex() {
        float noOfDuplicates = 0;
        boolean cleanCleanER = blocks.get(0) instanceof BilateralBlock;
        for (IdDuplicates pairOfDuplicates : abstractDP.getDuplicates()) {
            if (areCooccurring(cleanCleanER, pairOfDuplicates)) {
                noOfDuplicates++;
            }
        }

        detectedDuplicates = (int) noOfDuplicates;
        pc = noOfDuplicates / abstractDP.getExistingDuplicates();
        pq = noOfDuplicates / aggregateCardinality;
        fMeasure = 2 * pc * pq / (pc + pq);
    }

    private void getEntities() {
        if (blocks.get(0) instanceof UnilateralBlock) {
            final TIntSet distinctEntities = new TIntHashSet();
            for (AbstractBlock block : blocks) {
                final UnilateralBlock uBlock = (UnilateralBlock) block;
                for (int entityId : uBlock.getEntities()) {
                    distinctEntities.add(entityId);
                }
            }
            noOfD1Entities = distinctEntities.size();
        } else {
            final TIntSet distinctEntitiesD1 = new TIntHashSet();
            final TIntSet distinctEntitiesD2 = new TIntHashSet();
            for (AbstractBlock block : blocks) {
                final BilateralBlock bBlock = (BilateralBlock) block;
                for (int entityId : bBlock.getIndex1Entities()) {
                    distinctEntitiesD1.add(entityId);
                }
                for (int entityId : bBlock.getIndex2Entities()) {
                    distinctEntitiesD2.add(entityId);
                }
            }
            noOfD1Entities = distinctEntitiesD1.size();
            noOfD2Entities = distinctEntitiesD2.size();
        }
    }

    public void printDetailedResults(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2) {
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return;
        }

        setType();

        List<AbstractBlock> blocksToUse = blocks;
        if (!(blocks.get(0) instanceof DecomposedBlock)) {
            final ComparisonPropagation cp = new ComparisonPropagation();
            blocksToUse = cp.refineBlocks(blocks);
        }

        abstractDP.resetDuplicates();
        for (AbstractBlock block : blocksToUse) {
            final ComparisonIterator iterator = block.getComparisonIterator();
            while (iterator.hasNext()) {
                final Comparison currentComparison = iterator.next();
                final EntityProfile profile1 = profilesD1.get(currentComparison.getEntityId1());
                final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(currentComparison.getEntityId2()) : profilesD1.get(currentComparison.getEntityId2());

                final int originalDuplicates = abstractDP.getNoOfDuplicates();
                abstractDP.isSuperfluous(currentComparison.getEntityId1(), currentComparison.getEntityId2());
                final int newDuplicates = abstractDP.getNoOfDuplicates();

                System.out.print(profile1.getEntityUrl() + ",");
                System.out.print(profile2.getEntityUrl() + ",");
                if (originalDuplicates == newDuplicates) {
                    System.out.print("FP,"); //false positive
                } else { // originalDuplicates < newDuplicates
                    System.out.print("TP,"); // true positive
                }
                System.out.print("Profile 1:[" + profilesD1 + "]");
                System.out.println("Profile 2:[" + profilesD2 + "]");
            }
        }

        for (IdDuplicates duplicatesPair : abstractDP.getFalseNegatives()) {
            final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(duplicatesPair.getEntityId2()) : profilesD1.get(duplicatesPair.getEntityId2());

            System.out.print(profile1.getEntityUrl() + ",");
            System.out.print(profile2.getEntityUrl() + ",");
            System.out.print("FN,"); // false negative
            System.out.print("Profile 1:[" + profile1 + "]");
            System.out.println("Profile 2:[" + profile2 + "]");
        }

        detectedDuplicates = abstractDP.getNoOfDuplicates();
        pc = ((float) abstractDP.getNoOfDuplicates()) / abstractDP.getExistingDuplicates();
        pq = abstractDP.getNoOfDuplicates() / aggregateCardinality;
        if (0 < pc && 0 < pq) {
            fMeasure = 2 * pc * pq / (pc + pq);
        } else {
            fMeasure = 0;
        }

        System.out.println("Pairs Quality (Precision)\t:\t" + pq);
        System.out.println("Pairs Completentess (Recall)\t:\t" + pc);
        System.out.println("F-Measure\t:\t" + fMeasure);
    }

    public void printDetailedResultsToCSV(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, String outputFile) throws FileNotFoundException {
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return;
        }

        setType();

        List<AbstractBlock> blocksToUse = blocks;
        if (!(blocks.get(0) instanceof DecomposedBlock)) {
            final ComparisonPropagation cp = new ComparisonPropagation();
            blocksToUse = cp.refineBlocks(blocks);
        }

        StringBuilder sb = new StringBuilder();
        final PrintWriter printWriter = new PrintWriter(new File(outputFile));

        abstractDP.resetDuplicates();
        for (AbstractBlock block : blocksToUse) {
            final ComparisonIterator iterator = block.getComparisonIterator();
            while (iterator.hasNext()) {
                final Comparison currentComparison = iterator.next();
                final EntityProfile profile1 = profilesD1.get(currentComparison.getEntityId1());
                final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(currentComparison.getEntityId2()) : profilesD1.get(currentComparison.getEntityId2());

                final int originalDuplicates = abstractDP.getNoOfDuplicates();
                abstractDP.isSuperfluous(currentComparison.getEntityId1(), currentComparison.getEntityId2());
                final int newDuplicates = abstractDP.getNoOfDuplicates();

                sb.append(profile1.getEntityUrl()).append(",");
                sb.append(profile2.getEntityUrl()).append(",");
                if (originalDuplicates == newDuplicates) {
                    sb.append("FP,"); //false positive
                } else { // originalDuplicates < newDuplicates
                    sb.append("TP,"); // true positive
                }
                sb.append("Profile 1:[").append(profile1).append("]");
                sb.append("Profile 2:[").append(profile2).append("]").append("\n");
            }
        }

        for (IdDuplicates duplicatesPair : abstractDP.getFalseNegatives()) {
            final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(duplicatesPair.getEntityId2()) : profilesD1.get(duplicatesPair.getEntityId2());

            sb.append(profile1.getEntityUrl()).append(",");
            sb.append(profile2.getEntityUrl()).append(",");
            sb.append("FN,"); // false negative
            sb.append("Profile 1:[").append(profile1).append("]");
            sb.append("Profile 2:[").append(profile2).append("]").append("\n");
        }

        detectedDuplicates = abstractDP.getNoOfDuplicates();
        pc = ((float) abstractDP.getNoOfDuplicates()) / abstractDP.getExistingDuplicates();
        pq = abstractDP.getNoOfDuplicates() / aggregateCardinality;
        if (0 < pc && 0 < pq) {
            fMeasure = 2 * pc * pq / (pc + pq);
        } else {
            fMeasure = 0;
        }

        printWriter.println("Pairs Quality (Precision)\t:\t" + pq);
        printWriter.println("Pairs Completentess (Recall)\t:\t" + pc);
        printWriter.println("F-Measure\t:\t" + fMeasure);
        printWriter.write(sb.toString());
        printWriter.close();
    }

    public void printDetailedResultsToRDFXML(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, String outputFile) throws FileNotFoundException {
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return;
        }

        setType();

        List<AbstractBlock> blocksToUse = blocks;
        if (!(blocks.get(0) instanceof DecomposedBlock)) {
            final ComparisonPropagation cp = new ComparisonPropagation();
            blocksToUse = cp.refineBlocks(blocks);
        }

        final PrintWriter printWriter = new PrintWriter(new File(outputFile));
        printWriter.println("<?xml version=\"1.0\"?>");
        printWriter.println();
        printWriter.println("<rdf:RDF");
        printWriter.println("xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
        printWriter.println("xmlns:obj=\"https://www.w3schools.com/rdf/\">");

        abstractDP.resetDuplicates();
        for (AbstractBlock block : blocksToUse) {
            final ComparisonIterator iterator = block.getComparisonIterator();
            while (iterator.hasNext()) {
                final Comparison currentComparison = iterator.next();
                final EntityProfile profile1 = profilesD1.get(currentComparison.getEntityId1());
                final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(currentComparison.getEntityId2()) : profilesD1.get(currentComparison.getEntityId2());

                final int originalDuplicates = abstractDP.getNoOfDuplicates();
                abstractDP.isSuperfluous(currentComparison.getEntityId1(), currentComparison.getEntityId2());
                final int newDuplicates = abstractDP.getNoOfDuplicates();

                printWriter.println();

                printWriter.println("<rdf:Description rdf:about=\"" + block.toString() + "\">");

                printWriter.print("<obj:" + "url1" + ">");
                printWriter.print(profile1.getEntityUrl().replace("&", "") + "");
                printWriter.println("</obj:" + "url1>");

                printWriter.print("<obj:" + "url2" + ">");
                printWriter.print(profile2.getEntityUrl().replace("&", "") + "");
                printWriter.println("</obj:" + "url2>");

                printWriter.print("<obj:" + "pairType" + ">");
                if (originalDuplicates == newDuplicates) {
                    printWriter.print("FP"); //false positive
                } else { // originalDuplicates < newDuplicates
                    printWriter.print("TP"); // true positive
                }
                printWriter.println("</obj:" + "pairType>");

                printWriter.print("<obj:" + "Profile1" + ">");
                printWriter.print((profile1 + "").replace("&", ""));
                printWriter.println("</obj:" + "Profile1>");

                printWriter.print("<obj:" + "Profile2" + ">");
                printWriter.print((profile2 + "").replace("&", ""));
                printWriter.println("</obj:" + "Profile2>");

                printWriter.println("</rdf:Description>");
            }
        }

        for (IdDuplicates duplicatesPair : abstractDP.getFalseNegatives()) {
            final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(duplicatesPair.getEntityId2()) : profilesD1.get(duplicatesPair.getEntityId2());

            printWriter.println();

            printWriter.println("<rdf:Description rdf:about=\"" + duplicatesPair.toString() + "\">");

            printWriter.print("<obj:" + "url1" + ">");
            printWriter.print(profile1.getEntityUrl().replace("&", "") + "");
            printWriter.println("</obj:" + "url1>");

            printWriter.print("<obj:" + "url2" + ">");
            printWriter.print(profile2.getEntityUrl().replace("&", "") + "");
            printWriter.println("</obj:" + "url2>");

            printWriter.print("<obj:" + "pairType" + ">");
            printWriter.print("FN"); // false negative
            printWriter.println("</obj:" + "pairType>");

            printWriter.print("<obj:" + "Profile1" + ">");
            printWriter.print((profile1 + "").replace("&", ""));
            printWriter.println("</obj:" + "Profile1>");

            printWriter.print("<obj:" + "Profile2" + ">");
            printWriter.print((profile2 + "").replace("&", ""));
            printWriter.println("</obj:" + "Profile2>");

            printWriter.println("</rdf:Description>");
        }

        detectedDuplicates = abstractDP.getNoOfDuplicates();
        pc = ((float) abstractDP.getNoOfDuplicates()) / abstractDP.getExistingDuplicates();
        pq = abstractDP.getNoOfDuplicates() / aggregateCardinality;
        if (0 < pc && 0 < pq) {
            fMeasure = 2 * pc * pq / (pc + pq);
        } else {
            fMeasure = 0;
        }

        printWriter.println();

        printWriter.println("<rdf:Description rdf:about=\"" + "STATS" + "\">");

        printWriter.print("<obj:" + "PairsQuality" + ">");
        printWriter.print(pq + "");
        printWriter.println("</obj:" + "PairsQuality>");

        printWriter.print("<obj:" + "PairsCompletentess" + ">");
        printWriter.print(pc + "");
        printWriter.println("</obj:" + "PairsCompletentess>");

        printWriter.print("<obj:" + "F-Measure" + ">");
        printWriter.print(fMeasure + "");
        printWriter.println("</obj:" + "F-Measure>");

        printWriter.println("</rdf:Description>");

        printWriter.println("</rdf:RDF>");
        printWriter.close();

    }

    public void printDetailedResultsToJSONrdf(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, String outputFile) throws FileNotFoundException {
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return;
        }

        setType();

        List<AbstractBlock> blocksToUse = blocks;
        if (!(blocks.get(0) instanceof DecomposedBlock)) {
            final ComparisonPropagation cp = new ComparisonPropagation();
            blocksToUse = cp.refineBlocks(blocks);
        }

        final PrintWriter printWriter = new PrintWriter(new File(outputFile));

        printWriter.println("{\"triples\":\n");
        printWriter.println("[");

        String subString = "{Subject: \"";
        String predString = "\", Predicate: \"";
        String objString = "\", Object: \"";

        abstractDP.resetDuplicates();
        for (AbstractBlock block : blocksToUse) {
            final ComparisonIterator iterator = block.getComparisonIterator();
            while (iterator.hasNext()) {
                final Comparison currentComparison = iterator.next();
                final EntityProfile profile1 = profilesD1.get(currentComparison.getEntityId1());
                final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(currentComparison.getEntityId2()) : profilesD1.get(currentComparison.getEntityId2());

                final int originalDuplicates = abstractDP.getNoOfDuplicates();
                abstractDP.isSuperfluous(currentComparison.getEntityId1(), currentComparison.getEntityId2());
                final int newDuplicates = abstractDP.getNoOfDuplicates();

                printWriter.print(subString + block.toString());
                printWriter.print(predString + "url1");
                printWriter.print(objString + profile1.getEntityUrl().replace("&", "").replace("\\", "") + "");
                printWriter.println("\"},");

                printWriter.print(subString + block.toString());
                printWriter.print(predString + "url2");
                printWriter.print(objString + profile2.getEntityUrl().replace("&", "").replace("\\", "") + "");
                printWriter.println("\"},");

                printWriter.print(subString + block.toString());
                printWriter.print(predString + "pairType");
                printWriter.print(objString);
                if (originalDuplicates == newDuplicates) {
                    printWriter.print("FP"); //false positive
                } else { // originalDuplicates < newDuplicates
                    printWriter.print("TP"); // true positive
                }
                printWriter.println("\"},");

                printWriter.print(subString + block.toString());
                printWriter.print(predString + "Profile1");
                printWriter.print(objString);
                printWriter.print((profile1 + "").replace("&", "").replace("\\", ""));
                printWriter.println("\"},");

                printWriter.print(subString + block.toString());
                printWriter.print(predString + "Profile2");
                printWriter.print(objString);
                printWriter.print((profile2 + "").replace("&", "").replace("\\", ""));
                printWriter.println("\"},");
            }
        }

        for (IdDuplicates duplicatesPair : abstractDP.getFalseNegatives()) {
            final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(duplicatesPair.getEntityId2()) : profilesD1.get(duplicatesPair.getEntityId2());

            printWriter.print(subString + duplicatesPair.toString());
            printWriter.print(predString + "url1");
            printWriter.print(objString + profile1.getEntityUrl().replace("&", "").replace("\\", "") + "");
            printWriter.println("\"},");

            printWriter.print(subString + duplicatesPair.toString());
            printWriter.print(predString + "url2");
            printWriter.print(objString + profile2.getEntityUrl().replace("&", "").replace("\\", "") + "");
            printWriter.println("\"},");

            printWriter.print(subString + duplicatesPair.toString());
            printWriter.print(predString + "pairType");
            printWriter.print(objString);
            printWriter.print("FN"); //false negative
            printWriter.println("\"},");

            printWriter.print(subString + duplicatesPair.toString());
            printWriter.print(predString + "Profile1");
            printWriter.print(objString);
            printWriter.print((profile1 + "").replace("&", "").replace("\\", ""));
            printWriter.println("\"},");

            printWriter.print(subString + duplicatesPair.toString());
            printWriter.print(predString + "Profile2");
            printWriter.print(objString);
            printWriter.print((profile2 + "").replace("&", "").replace("\\", ""));
            printWriter.println("\"},");
        }

        detectedDuplicates = abstractDP.getNoOfDuplicates();
        pc = ((float) abstractDP.getNoOfDuplicates()) / abstractDP.getExistingDuplicates();
        pq = abstractDP.getNoOfDuplicates() / aggregateCardinality;
        if (0 < pc && 0 < pq) {
            fMeasure = 2 * pc * pq / (pc + pq);
        } else {
            fMeasure = 0;
        }

        printWriter.print(subString + "STATS");
        printWriter.print(predString + "PairsQuality");
        printWriter.print(objString);
        printWriter.print(pq + "");
        printWriter.println("\"},");

        printWriter.print(subString + "STATS");
        printWriter.print(predString + "PairsCompletentess");
        printWriter.print(objString);
        printWriter.print(pc + "");
        printWriter.println("\"},");

        printWriter.print(subString + "STATS");
        printWriter.print(predString + "F-Measure");
        printWriter.print(objString);
        printWriter.print(fMeasure + "");
        printWriter.println("\"}");

        printWriter.println("]");
        printWriter.println("}");
        printWriter.close();

    }

    public void printDetailedResultsToRDFNT(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, String outputFile) throws FileNotFoundException {
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return;
        }

        setType();

        List<AbstractBlock> blocksToUse = blocks;
        if (!(blocks.get(0) instanceof DecomposedBlock)) {
            final ComparisonPropagation cp = new ComparisonPropagation();
            blocksToUse = cp.refineBlocks(blocks);
        }

        final PrintWriter printWriter = new PrintWriter(new File(outputFile));

        String xmlnsrdf = "http://www.w3.org/1999/02/22/";
        String xmlnsobj = "https://www.w3schools.com/rdf/";

        abstractDP.resetDuplicates();
        for (AbstractBlock block : blocksToUse) {
            final ComparisonIterator iterator = block.getComparisonIterator();
            while (iterator.hasNext()) {
                final Comparison currentComparison = iterator.next();
                final EntityProfile profile1 = profilesD1.get(currentComparison.getEntityId1());
                final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(currentComparison.getEntityId2()) : profilesD1.get(currentComparison.getEntityId2());

                final int originalDuplicates = abstractDP.getNoOfDuplicates();
                abstractDP.isSuperfluous(currentComparison.getEntityId1(), currentComparison.getEntityId2());
                final int newDuplicates = abstractDP.getNoOfDuplicates();

                printWriter.print("<" + xmlnsrdf + block.toString() + "> ");
                printWriter.print("<" + xmlnsobj + "url1" + "> \"");
                printWriter.print(profile1.getEntityUrl().replace("&", "").replace("\\", "") + "");
                printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");

                printWriter.print("<" + xmlnsrdf + block.toString() + "> ");
                printWriter.print("<" + xmlnsobj + "url2" + "> \"");
                printWriter.print(profile2.getEntityUrl().replace("&", "").replace("\\", "") + "");
                printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");

                printWriter.print("<" + xmlnsrdf + block.toString() + "> ");
                printWriter.print("<" + xmlnsobj + "pairType" + "> \"");
                if (originalDuplicates == newDuplicates) {
                    printWriter.print("FP"); //false positive
                } else { // originalDuplicates < newDuplicates
                    printWriter.print("TP"); // true positive
                }
                printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");

                printWriter.print("<" + xmlnsrdf + block.toString() + "> ");
                printWriter.print("<" + xmlnsobj + "Profile1" + "> \"");
                printWriter.print((profile1 + "").replace("&", "").replace("\\", ""));
                printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");

                printWriter.print("<" + xmlnsrdf + block.toString() + "> ");
                printWriter.print("<" + xmlnsobj + "Profile2" + "> \"");
                printWriter.print((profile2 + "").replace("&", "").replace("\\", ""));
                printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");
            }
        }

        for (IdDuplicates duplicatesPair : abstractDP.getFalseNegatives()) {
            final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(duplicatesPair.getEntityId2()) : profilesD1.get(duplicatesPair.getEntityId2());

            printWriter.print("<" + xmlnsrdf + duplicatesPair.toString() + "> ");
            printWriter.print("<" + xmlnsobj + "url1" + "> \"");
            printWriter.print(profile1.getEntityUrl().replace("&", "").replace("\\", "") + "");
            printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");

            printWriter.print("<" + xmlnsrdf + duplicatesPair.toString() + "> ");
            printWriter.print("<" + xmlnsobj + "url2" + "> \"");
            printWriter.print(profile2.getEntityUrl().replace("&", "").replace("\\", "") + "");
            printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");

            printWriter.print("<" + xmlnsrdf + duplicatesPair.toString() + "> ");
            printWriter.print("<" + xmlnsobj + "pairType" + "> \"");
            printWriter.print("FN"); //false positive

            printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");

            printWriter.print("<" + xmlnsrdf + duplicatesPair.toString() + "> ");
            printWriter.print("<" + xmlnsobj + "Profile1" + "> \"");
            printWriter.print((profile1 + "").replace("&", "").replace("\\", ""));
            printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");

            printWriter.print("<" + xmlnsrdf + duplicatesPair.toString() + "> ");
            printWriter.print("<" + xmlnsobj + "Profile2" + "> \"");
            printWriter.print((profile2 + "").replace("&", "").replace("\\", ""));
            printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");
        }

        detectedDuplicates = abstractDP.getNoOfDuplicates();
        pc = ((float) abstractDP.getNoOfDuplicates()) / abstractDP.getExistingDuplicates();
        pq = abstractDP.getNoOfDuplicates() / aggregateCardinality;
        if (0 < pc && 0 < pq) {
            fMeasure = 2 * pc * pq / (pc + pq);
        } else {
            fMeasure = 0;
        }

        printWriter.print("<" + xmlnsrdf + "STATS" + "> ");
        printWriter.print("<" + xmlnsobj + "PairsQuality" + "> \"");
        printWriter.print(pq + "");
        printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");

        printWriter.print("<" + xmlnsrdf + "STATS" + "> ");
        printWriter.print("<" + xmlnsobj + "PairsCompletentess" + "> \"");
        printWriter.print(pc + "");
        printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");

        printWriter.print("<" + xmlnsrdf + "STATS" + "> ");
        printWriter.print("<" + xmlnsobj + "F-Measure" + "> \"");
        printWriter.print(fMeasure + "");
        printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");

        printWriter.close();

    }

    public void printDetailedResultsToHDTrdf(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, String outputFile) throws IOException, ParserException {

        this.printDetailedResultsToRDFNT(profilesD1, profilesD2, outputFile + "help.nt");
        String baseURI = "http://www.w3.org/";
        //String rdfInput = "data//minExample.nt";
        String rdfInput = outputFile + "help.nt";
        String inputType = "ntriples";
        String hdtOutput = outputFile;

        // Create HDT from RDF file
        HDT hdt = HDTManager.generateHDT(
                rdfInput, // Input RDF File
                baseURI, // Base URI
                RDFNotation.parse(inputType), // Input Type
                new HDTSpecification(), // HDT Options
                null // Progress Listener
        );

        // OPTIONAL: Add additional domain-specific properties to the header:
        //Header header = hdt.getHeader();
        //header.insert("myResource1", "property" , "value");
        // Save generated HDT to a file
        hdt.saveToHDT(hdtOutput, null);

        // IMPORTANT: Close hdt when no longer needed
        hdt.close();
    }

    public void printDetailedResultsToXML(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, String outputFile) throws FileNotFoundException {
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return;
        }

        setType();

        List<AbstractBlock> blocksToUse = blocks;
        if (!(blocks.get(0) instanceof DecomposedBlock)) {
            final ComparisonPropagation cp = new ComparisonPropagation();
            blocksToUse = cp.refineBlocks(blocks);
        }

        final PrintWriter printWriter = new PrintWriter(new File(outputFile));
        printWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        printWriter.println();

        printWriter.println("<general>");

        abstractDP.resetDuplicates();
        for (AbstractBlock block : blocksToUse) {
            final ComparisonIterator iterator = block.getComparisonIterator();
            while (iterator.hasNext()) {
                final Comparison currentComparison = iterator.next();
                final EntityProfile profile1 = profilesD1.get(currentComparison.getEntityId1());
                final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(currentComparison.getEntityId2()) : profilesD1.get(currentComparison.getEntityId2());

                final int originalDuplicates = abstractDP.getNoOfDuplicates();
                abstractDP.isSuperfluous(currentComparison.getEntityId1(), currentComparison.getEntityId2());
                final int newDuplicates = abstractDP.getNoOfDuplicates();

                printWriter.println();

                printWriter.println("<entity id=\"" + block.toString() + "\">");

                printWriter.print("<url1" + ">");
                printWriter.print(profile1.getEntityUrl().replace("&", "") + "");
                printWriter.println("</url1>");

                printWriter.print("<url2" + ">");
                printWriter.print(profile2.getEntityUrl().replace("&", "") + "");
                printWriter.println("</url2>");

                printWriter.print("<pairType" + ">");
                if (originalDuplicates == newDuplicates) {
                    printWriter.print("FP"); //false positive
                } else { // originalDuplicates < newDuplicates
                    printWriter.print("TP"); // true positive
                }
                printWriter.println("</pairType>");

                printWriter.print("<Profile1" + ">");
                printWriter.print((profile1 + "").replace("&", ""));
                printWriter.println("</Profile1>");

                printWriter.print("<Profile2" + ">");
                printWriter.print((profile2 + "").replace("&", ""));
                printWriter.println("</Profile2>");

                printWriter.println("</entity>");
            }
        }

        for (IdDuplicates duplicatesPair : abstractDP.getFalseNegatives()) {
            final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(duplicatesPair.getEntityId2()) : profilesD1.get(duplicatesPair.getEntityId2());

            printWriter.println();

            printWriter.println("<entity id=\"" + duplicatesPair.toString() + "\">");

            printWriter.print("<url1" + ">");
            printWriter.print(profile1.getEntityUrl().replace("&", "") + "");
            printWriter.println("</url1>");

            printWriter.print("<url2" + ">");
            printWriter.print(profile2.getEntityUrl().replace("&", "") + "");
            printWriter.println("</url2>");

            printWriter.print("<pairType" + ">");
            printWriter.print("FN"); // false negative
            printWriter.println("</pairType>");

            printWriter.print("<Profile1" + ">");
            printWriter.print((profile1 + "").replace("&", ""));
            printWriter.println("</Profile1>");

            printWriter.print("<Profile2" + ">");
            printWriter.print((profile2 + "").replace("&", ""));
            printWriter.println("</Profile2>");

            printWriter.println("</entity>");
        }

        detectedDuplicates = abstractDP.getNoOfDuplicates();
        pc = ((float) abstractDP.getNoOfDuplicates()) / abstractDP.getExistingDuplicates();
        pq = abstractDP.getNoOfDuplicates() / aggregateCardinality;
        if (0 < pc && 0 < pq) {
            fMeasure = 2 * pc * pq / (pc + pq);
        } else {
            fMeasure = 0;
        }

        printWriter.println();

        printWriter.println("<stats>");

        printWriter.print("<PairsQuality" + ">");
        printWriter.print(pq + "");
        printWriter.println("</PairsQuality>");

        printWriter.print("<PairsCompletentess" + ">");
        printWriter.print(pc + "");
        printWriter.println("</PairsCompletentess>");

        printWriter.print("<F-Measure" + ">");
        printWriter.print(fMeasure + "");
        printWriter.println("</F-Measure>");

        printWriter.println("</stats>");

        printWriter.println();

        printWriter.println("</general>");

        printWriter.close();

    }

    public void printDetailedResultsToSPARQL(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, String endpointURL, String GraphName) {
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return;
        }

        setType();

        List<AbstractBlock> blocksToUse = blocks;
        if (!(blocks.get(0) instanceof DecomposedBlock)) {
            final ComparisonPropagation cp = new ComparisonPropagation();
            blocksToUse = cp.refineBlocks(blocks);
        }
        StringBuilder sb = new StringBuilder();

        String sparqlQueryString1 = "INSERT DATA { "
                + "GRAPH " + GraphName + " { ";
        sb.append(sparqlQueryString1);

        abstractDP.resetDuplicates();
        int counter0 = 0;
        for (AbstractBlock block : blocksToUse) {
            final ComparisonIterator iterator = block.getComparisonIterator();
            while (iterator.hasNext()) {
                final Comparison currentComparison = iterator.next();
                final EntityProfile profile1 = profilesD1.get(currentComparison.getEntityId1());
                final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(currentComparison.getEntityId2()) : profilesD1.get(currentComparison.getEntityId2());

                final int originalDuplicates = abstractDP.getNoOfDuplicates();
                abstractDP.isSuperfluous(currentComparison.getEntityId1(), currentComparison.getEntityId2());
                final int newDuplicates = abstractDP.getNoOfDuplicates();

                counter0++;

                sb.append("<obj/" + "record/").append(block.toString()).append("> ");
                sb.append("<url1> ");
                sb.append("\"").append(profile1.getEntityUrl().replace("&", "")).append("\".\n");

                sb.append("<obj/" + "record/").append(block.toString()).append("> ");
                sb.append("<url2> ");
                sb.append("\"").append(profile2.getEntityUrl().replace("&", "")).append("\".\n");

                sb.append("<obj/" + "record/").append(block.toString()).append("> ");
                sb.append("<pairType> ");
                if (originalDuplicates == newDuplicates) {
                    sb.append("\"" + "FP" + "\".\n");//false positive
                } else { // originalDuplicates < newDuplicates
                    sb.append("\"" + "TP" + "\".\n"); // true positive
                }

                sb.append("<obj/" + "record/").append(block.toString()).append("> ");
                sb.append("<Profile1> ");
                sb.append("\"").append((profile1 + "").replace("&", "")).append("\".\n");

                sb.append("<obj/" + "record/").append(block.toString()).append("> ");
                sb.append("<Profile2> ");
                sb.append("\"").append((profile2 + "").replace("&", "")).append("\".\n");

                //execute query every 1000 steps
                if (counter0 % 1000 == 0) {
                    sb.append("}\n }");
                    String sparqlQueryString = sb.toString();

                    //System.out.println(sparqlQueryString);
                    UpdateRequest update = UpdateFactory.create(sparqlQueryString);
                    UpdateProcessor qexec = UpdateExecutionFactory.createRemote(update, endpointURL);
                    qexec.execute();
                    sb.setLength(0);
                    sb.append(sparqlQueryString1);
                }

            }
        }
        if (counter0 % 1000 != 0) {
            sb.append("}\n }");
            String sparqlQueryString = sb.toString();

            //System.out.println(sparqlQueryString);
            UpdateRequest update = UpdateFactory.create(sparqlQueryString);
            UpdateProcessor qexec = UpdateExecutionFactory.createRemote(update, endpointURL);
            qexec.execute();
            sb.setLength(0);
            sb.append(sparqlQueryString1);
        }

        int counter1 = 0;
        for (IdDuplicates duplicatesPair : abstractDP.getFalseNegatives()) {
            final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(duplicatesPair.getEntityId2()) : profilesD1.get(duplicatesPair.getEntityId2());

            counter1++;

            sb.append("<obj/" + "record/").append(duplicatesPair.toString()).append("> ");
            sb.append("<url1> ");
            sb.append("\"").append(profile1.getEntityUrl().replace("&", "")).append("\".\n");

            sb.append("<obj/" + "record/").append(duplicatesPair.toString()).append("> ");
            sb.append("<url2> ");
            sb.append("\"").append(profile2.getEntityUrl().replace("&", "")).append("\".\n");

            sb.append("<obj/" + "record/").append(duplicatesPair.toString()).append("> ");
            sb.append("<pairType> ");
            sb.append("\"" + "FN" + "\".\n"); // false negative

            sb.append("<obj/" + "record/").append(duplicatesPair.toString()).append("> ");
            sb.append("<Profile1> ");
            sb.append("\"").append((profile1 + "").replace("&", "")).append("\".\n");

            sb.append("<obj/" + "record/").append(duplicatesPair.toString()).append("> ");
            sb.append("<Profile2> ");
            sb.append("\"").append((profile2 + "").replace("&", "")).append("\".\n");

            //execute query every 1000 steps
            if (counter1 % 1000 == 0) {
                sb.append("}\n }");
                String sparqlQueryString = sb.toString();

                //System.out.println(sparqlQueryString);
                UpdateRequest update = UpdateFactory.create(sparqlQueryString);
                UpdateProcessor qexec = UpdateExecutionFactory.createRemote(update, endpointURL);
                qexec.execute();
                sb.setLength(0);
                sb.append(sparqlQueryString1);
            }
        }

        if (counter1 % 1000 != 0) {
            sb.append("}\n }");
            String sparqlQueryString = sb.toString();

            //System.out.println(sparqlQueryString);
            UpdateRequest update = UpdateFactory.create(sparqlQueryString);
            UpdateProcessor qexec = UpdateExecutionFactory.createRemote(update, endpointURL);
            qexec.execute();
            sb.setLength(0);
            sb.append(sparqlQueryString1);
        }

        detectedDuplicates = abstractDP.getNoOfDuplicates();
        pc = ((float) abstractDP.getNoOfDuplicates()) / abstractDP.getExistingDuplicates();
        pq = abstractDP.getNoOfDuplicates() / aggregateCardinality;
        if (0 < pc && 0 < pq) {
            fMeasure = 2 * pc * pq / (pc + pq);
        } else {
            fMeasure = 0;
        }

        sb.append("<obj/" + "record/" + "STATS" + "> ");
        sb.append("<PairsQuality> ");
        sb.append("\"").append(pq).append("\".\n");

        sb.append("<obj/" + "record/" + "STATS" + "> ");
        sb.append("<PairsCompletentess> ");
        sb.append("\"").append(pc).append("\".\n");

        sb.append("<obj/" + "record/" + "STATS" + "> ");
        sb.append("<F-Measure> ");
        sb.append("\"").append(fMeasure).append("\".\n");

        sb.append("}\n }");
        String sparqlQueryString = sb.toString();

        //System.out.println(sparqlQueryString);
        UpdateRequest update = UpdateFactory.create(sparqlQueryString);
        UpdateProcessor qexec = UpdateExecutionFactory.createRemote(update, endpointURL);
        qexec.execute();

    }

    public void printDetailedResultsToDB(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, String dbURL) {
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return;
        }

        setType();

        List<AbstractBlock> blocksToUse = blocks;
        if (!(blocks.get(0) instanceof DecomposedBlock)) {
            final ComparisonPropagation cp = new ComparisonPropagation();
            blocksToUse = cp.refineBlocks(blocks);
        }

        StringBuilder sb = new StringBuilder();
        String dbquery1 = "INSERT INTO " + dbtable + " (url1, url2, pairtype, Profile1, Profile2) VALUES ";
        sb.append(dbquery1);
        abstractDP.resetDuplicates();
        for (AbstractBlock block : blocksToUse) {
            final ComparisonIterator iterator = block.getComparisonIterator();
            while (iterator.hasNext()) {
                final Comparison currentComparison = iterator.next();
                final EntityProfile profile1 = profilesD1.get(currentComparison.getEntityId1());
                final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(currentComparison.getEntityId2()) : profilesD1.get(currentComparison.getEntityId2());

                final int originalDuplicates = abstractDP.getNoOfDuplicates();
                abstractDP.isSuperfluous(currentComparison.getEntityId1(), currentComparison.getEntityId2());
                final int newDuplicates = abstractDP.getNoOfDuplicates();

                sb.append("('").append(profile1.getEntityUrl()).append("', ");
                sb.append("'").append(profile2.getEntityUrl()).append("', ");

                if (originalDuplicates == newDuplicates) {
                    sb.append("'" + "FP" + "', "); //false positive
                } else { // originalDuplicates < newDuplicates
                    sb.append("'" + "TP" + "', "); //true positive
                }
                sb.append("'").append(profile1).append("', ");
                sb.append("'").append(profile2).append("'), ");
            }
        }

        for (IdDuplicates duplicatesPair : abstractDP.getFalseNegatives()) {
            final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(duplicatesPair.getEntityId2()) : profilesD1.get(duplicatesPair.getEntityId2());

            sb.append("('").append(profile1.getEntityUrl()).append("', ");
            sb.append("'").append(profile2.getEntityUrl()).append("', ");
            sb.append("'" + "FN" + "', "); // false negative
            sb.append("'").append(profile1).append("', ");
            sb.append("'").append(profile2).append("'), ");
        }

        detectedDuplicates = abstractDP.getNoOfDuplicates();
        pc = ((float) abstractDP.getNoOfDuplicates()) / abstractDP.getExistingDuplicates();
        pq = abstractDP.getNoOfDuplicates() / aggregateCardinality;
        if (0 < pc && 0 < pq) {
            fMeasure = 2 * pc * pq / (pc + pq);
        } else {
            fMeasure = 0;
        }

        sb.append("('").append(pq).append("', ");
        sb.append("'").append(pc).append("', ");
        sb.append("'").append(fMeasure).append("', ");
        sb.append("'" + "NULL" + "', ");
        sb.append("'" + "NULL" + "'); ");

        String dbquery = sb.toString();

        if (dbuser == null) {
            Log.error("Database user has not been set!");
        }
        if (dbpassword == null) {
            Log.error("Database password has not been set!");
        }
        if (dbtable == null) {
            Log.error("Database table has not been set!");
        }

        try (Connection conn = DBUtils.getDBConnection(dbURL, dbuser, dbpassword, ssl);
            Statement stmt = conn.createStatement();) {
            
          stmt.execute(dbquery);//retrieve the appropriate table
        } catch (Exception ex) {
            Log.error("Error in db writing!", ex);
        }
    }

    public void debugToCSV(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, String outputFile) throws FileNotFoundException {
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return;
        }

        setType(); // Clean-Clean or Dirty ER?
        final PrintWriter pw = new PrintWriter(new File(outputFile));
        StringBuilder sb = new StringBuilder();

        List<AbstractBlock> blocksToUse = blocks;
        if (!(blocks.get(0) instanceof DecomposedBlock)) {
            final ComparisonPropagation cp = new ComparisonPropagation();
            blocksToUse = cp.refineBlocks(blocks);
        }

        abstractDP.resetDuplicates();
        for (AbstractBlock block : blocksToUse) {
            final ComparisonIterator iterator = block.getComparisonIterator();
            while (iterator.hasNext()) {
                final Comparison comp = iterator.next();
                abstractDP.isSuperfluous(comp.getEntityId1(), comp.getEntityId2());
            }
        }

        for (IdDuplicates duplicatesPair : abstractDP.getFalseNegatives()) {
            final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(duplicatesPair.getEntityId2()) : profilesD1.get(duplicatesPair.getEntityId2());

            sb.append(profile1.getEntityUrl()).append(",");
            sb.append(profile2.getEntityUrl()).append(",");
            sb.append("FN,"); // false negative
            sb.append("Profile 1:[").append(profile1).append("]");
            sb.append("Profile 2:[").append(profile2).append("]");
        }

        pw.write(sb.toString());
        pw.close();
    }

    public void debugToDB(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, String dbURL) {
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return;
        }

        setType(); // Clean-Clean or Dirty ER?
        StringBuilder sb = new StringBuilder();
        String dbquery1 = "INSERT INTO " + dbtable + " (url1, url2, pairtype, Profile1, Profile2) VALUES ";
        sb.append(dbquery1);

        List<AbstractBlock> blocksToUse = blocks;
        if (!(blocks.get(0) instanceof DecomposedBlock)) {
            final ComparisonPropagation cp = new ComparisonPropagation();
            blocksToUse = cp.refineBlocks(blocks);
        }

        abstractDP.resetDuplicates();
        for (AbstractBlock block : blocksToUse) {
            final ComparisonIterator iterator = block.getComparisonIterator();
            while (iterator.hasNext()) {
                final Comparison comp = iterator.next();
                abstractDP.isSuperfluous(comp.getEntityId1(), comp.getEntityId2());
            }
        }

        for (IdDuplicates duplicatesPair : abstractDP.getFalseNegatives()) {
            final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(duplicatesPair.getEntityId2()) : profilesD1.get(duplicatesPair.getEntityId2());

            sb.append("('").append(profile1.getEntityUrl()).append("', ");
            sb.append("'").append(profile2.getEntityUrl()).append("', ");
            sb.append("'" + "FN" + "', "); // false negative
            sb.append("'").append(profile1).append("', ");
            sb.append("'").append(profile2).append("'), ");
        }

        String dbquery = sb.toString();

        if (dbuser == null) {
            Log.error("Database user has not been set!");
        }
        if (dbpassword == null) {
            Log.error("Database password has not been set!");
        }
        if (dbtable == null) {
            Log.error("Database table has not been set!");
        }

        try (Connection conn = DBUtils.getDBConnection(dbURL, dbuser, dbpassword, ssl);
            Statement stmt = conn.createStatement();) {
            stmt.execute(dbquery);//retrieve the appropriate table
        } catch (Exception ex) {
            Log.error("Error in db writing!", ex);
        }
    }

    public void debugToRDFXML(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, String outputFile) throws FileNotFoundException {
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return;
        }

        setType(); // Clean-Clean or Dirty ER?
        final PrintWriter printWriter = new PrintWriter(new File(outputFile));
        printWriter.println("<?xml version=\"1.0\"?>");
        printWriter.println();
        printWriter.println("<rdf:RDF");
        printWriter.println("xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
        printWriter.println("xmlns:obj=\"https://www.w3schools.com/rdf/\">");
        List<AbstractBlock> blocksToUse = blocks;
        if (!(blocks.get(0) instanceof DecomposedBlock)) {
            final ComparisonPropagation cp = new ComparisonPropagation();
            blocksToUse = cp.refineBlocks(blocks);
        }

        abstractDP.resetDuplicates();
        for (AbstractBlock block : blocksToUse) {
            final ComparisonIterator iterator = block.getComparisonIterator();
            while (iterator.hasNext()) {
                final Comparison comp = iterator.next();
                abstractDP.isSuperfluous(comp.getEntityId1(), comp.getEntityId2());
            }
        }

        for (IdDuplicates duplicatesPair : abstractDP.getFalseNegatives()) {
            final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(duplicatesPair.getEntityId2()) : profilesD1.get(duplicatesPair.getEntityId2());

            printWriter.println();

            printWriter.println("<rdf:Description rdf:about=\"" + duplicatesPair.toString() + "\">");

            printWriter.print("<obj:" + "url1" + ">");
            printWriter.print(profile1.getEntityUrl().replace("&", "") + "");
            printWriter.println("</obj:" + "url1>");

            printWriter.print("<obj:" + "url2" + ">");
            printWriter.print(profile2.getEntityUrl().replace("&", "") + "");
            printWriter.println("</obj:" + "url2>");

            printWriter.print("<obj:" + "pairType" + ">");
            printWriter.print("FN"); // false negative
            printWriter.println("</obj:" + "pairType>");

            printWriter.print("<obj:" + "Profile1" + ">");
            printWriter.print((profile1 + "").replace("&", ""));
            printWriter.println("</obj:" + "Profile1>");

            printWriter.print("<obj:" + "Profile2" + ">");
            printWriter.print((profile2 + "").replace("&", ""));
            printWriter.println("</obj:" + "Profile2>");

            printWriter.println("</rdf:Description>");
        }

        printWriter.println("</rdf:RDF>");
        printWriter.close();
    }

    public void debugToJSONrdf(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, String outputFile) throws FileNotFoundException {
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return;
        }

        setType(); // Clean-Clean or Dirty ER?
        final PrintWriter printWriter = new PrintWriter(new File(outputFile));

        printWriter.println("{\"triples\":\n");
        printWriter.println("[");

        String subString = "{Subject: \"";
        String predString = "\", Predicate: \"";
        String objString = "\", Object: \"";

        List<AbstractBlock> blocksToUse = blocks;
        if (!(blocks.get(0) instanceof DecomposedBlock)) {
            final ComparisonPropagation cp = new ComparisonPropagation();
            blocksToUse = cp.refineBlocks(blocks);
        }

        abstractDP.resetDuplicates();
        for (AbstractBlock block : blocksToUse) {
            final ComparisonIterator iterator = block.getComparisonIterator();
            while (iterator.hasNext()) {
                final Comparison comp = iterator.next();
                abstractDP.isSuperfluous(comp.getEntityId1(), comp.getEntityId2());
            }
        }

        int cntr = 0;
        for (IdDuplicates duplicatesPair : abstractDP.getFalseNegatives()) {
            cntr++;
            final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(duplicatesPair.getEntityId2()) : profilesD1.get(duplicatesPair.getEntityId2());

            printWriter.print(subString + duplicatesPair.toString());
            printWriter.print(predString + "url1");
            printWriter.print(objString + profile1.getEntityUrl().replace("&", "").replace("\\", "") + "");
            printWriter.println("\"},");

            printWriter.print(subString + duplicatesPair.toString());
            printWriter.print(predString + "url2");
            printWriter.print(objString + profile2.getEntityUrl().replace("&", "").replace("\\", "") + "");
            printWriter.println("\"},");

            printWriter.print(subString + duplicatesPair.toString());
            printWriter.print(predString + "pairType");
            printWriter.print(objString);
            printWriter.print("FN"); //false negative
            printWriter.println("\"},");

            printWriter.print(subString + duplicatesPair.toString());
            printWriter.print(predString + "Profile1");
            printWriter.print(objString);
            printWriter.print((profile1 + "").replace("&", "").replace("\\", ""));
            printWriter.println("\"},");

            printWriter.print(subString + duplicatesPair.toString());
            printWriter.print(predString + "Profile2");
            printWriter.print(objString);
            printWriter.print((profile2 + "").replace("&", "").replace("\\", ""));
            printWriter.println("\"}");
            if (cntr != abstractDP.getFalseNegatives().size()) {
                printWriter.println(",");
            }
        }

        printWriter.println("]");
        printWriter.println("}");
        printWriter.close();
    }

    public void debugToRDFNT(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, String outputFile) throws FileNotFoundException {
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return;
        }

        setType(); // Clean-Clean or Dirty ER?
        final PrintWriter printWriter = new PrintWriter(new File(outputFile));
        String xmlnsrdf = "http://www.w3.org/1999/02/22/";
        String xmlnsobj = "https://www.w3schools.com/rdf/";

        List<AbstractBlock> blocksToUse = blocks;
        if (!(blocks.get(0) instanceof DecomposedBlock)) {
            final ComparisonPropagation cp = new ComparisonPropagation();
            blocksToUse = cp.refineBlocks(blocks);
        }

        abstractDP.resetDuplicates();
        for (AbstractBlock block : blocksToUse) {
            final ComparisonIterator iterator = block.getComparisonIterator();
            while (iterator.hasNext()) {
                final Comparison comp = iterator.next();
                abstractDP.isSuperfluous(comp.getEntityId1(), comp.getEntityId2());
            }
        }

        for (IdDuplicates duplicatesPair : abstractDP.getFalseNegatives()) {
            final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(duplicatesPair.getEntityId2()) : profilesD1.get(duplicatesPair.getEntityId2());

            printWriter.print("<" + xmlnsrdf + duplicatesPair.toString() + "> ");
            printWriter.print("<" + xmlnsobj + "url1" + "> \"");
            printWriter.print(profile1.getEntityUrl().replace("&", "").replace("\\", "") + "");
            printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");

            printWriter.print("<" + xmlnsrdf + duplicatesPair.toString() + "> ");
            printWriter.print("<" + xmlnsobj + "url2" + "> \"");
            printWriter.print(profile2.getEntityUrl().replace("&", "").replace("\\", "") + "");
            printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");

            printWriter.print("<" + xmlnsrdf + duplicatesPair.toString() + "> ");
            printWriter.print("<" + xmlnsobj + "pairType" + "> \"");
            printWriter.print("FN"); //false positive

            printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");

            printWriter.print("<" + xmlnsrdf + duplicatesPair.toString() + "> ");
            printWriter.print("<" + xmlnsobj + "Profile1" + "> \"");
            printWriter.print((profile1 + "").replace("&", "").replace("\\", ""));
            printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");

            printWriter.print("<" + xmlnsrdf + duplicatesPair.toString() + "> ");
            printWriter.print("<" + xmlnsobj + "Profile2" + "> \"");
            printWriter.print((profile2 + "").replace("&", "").replace("\\", ""));
            printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");
        }
        printWriter.close();
    }

    public void debugToHDTrdf(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, String outputFile) throws IOException, ParserException {

        this.debugToRDFNT(profilesD1, profilesD2, outputFile + "help.nt");
        String baseURI = "http://www.w3.org/";
        //String rdfInput = "data//minExample.nt";
        String rdfInput = outputFile + "help.nt";
        String inputType = "ntriples";
        String hdtOutput = outputFile;

        // Create HDT from RDF file
        HDT hdt = HDTManager.generateHDT(
                rdfInput, // Input RDF File
                baseURI, // Base URI
                RDFNotation.parse(inputType), // Input Type
                new HDTSpecification(), // HDT Options
                null // Progress Listener
        );

        // OPTIONAL: Add additional domain-specific properties to the header:
        //Header header = hdt.getHeader();
        //header.insert("myResource1", "property" , "value");
        // Save generated HDT to a file
        hdt.saveToHDT(hdtOutput, null);

        // IMPORTANT: Close hdt when no longer needed
        hdt.close();
    }

    public void debugToXML(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, String outputFile) throws FileNotFoundException {
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return;
        }

        setType(); // Clean-Clean or Dirty ER?
        final PrintWriter printWriter = new PrintWriter(new File(outputFile));
        printWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        printWriter.println();

        printWriter.println("<general>");
        List<AbstractBlock> blocksToUse = blocks;
        if (!(blocks.get(0) instanceof DecomposedBlock)) {
            final ComparisonPropagation cp = new ComparisonPropagation();
            blocksToUse = cp.refineBlocks(blocks);
        }

        abstractDP.resetDuplicates();
        for (AbstractBlock block : blocksToUse) {
            final ComparisonIterator iterator = block.getComparisonIterator();
            while (iterator.hasNext()) {
                final Comparison comp = iterator.next();
                abstractDP.isSuperfluous(comp.getEntityId1(), comp.getEntityId2());
            }
        }

        for (IdDuplicates duplicatesPair : abstractDP.getFalseNegatives()) {
            final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(duplicatesPair.getEntityId2()) : profilesD1.get(duplicatesPair.getEntityId2());

            printWriter.println();

            printWriter.println("<entity id=\"" + duplicatesPair.toString() + "\">");

            printWriter.print("<url1" + ">");
            printWriter.print(profile1.getEntityUrl().replace("&", "") + "");
            printWriter.println("</url1>");

            printWriter.print("<url2" + ">");
            printWriter.print(profile2.getEntityUrl().replace("&", "") + "");
            printWriter.println("</url2>");

            printWriter.print("<pairType" + ">");
            printWriter.print("FN"); // false negative
            printWriter.println("</pairType>");

            printWriter.print("<Profile1" + ">");
            printWriter.print((profile1 + "").replace("&", ""));
            printWriter.println("</Profile1>");

            printWriter.print("<Profile2" + ">");
            printWriter.print((profile2 + "").replace("&", ""));
            printWriter.println("</Profile2>");

            printWriter.println("</entity>");
        }

        printWriter.println("</general>");

        printWriter.close();
    }

    public void debugToSPARQL(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, String endpointURL, String GraphName) {
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return;
        }

        setType(); // Clean-Clean or Dirty ER?
        StringBuilder sb = new StringBuilder();

        String sparqlQueryString1 = "INSERT DATA { "
                + "GRAPH " + GraphName + " { ";
        sb.append(sparqlQueryString1);

        List<AbstractBlock> blocksToUse = blocks;
        if (!(blocks.get(0) instanceof DecomposedBlock)) {
            final ComparisonPropagation cp = new ComparisonPropagation();
            blocksToUse = cp.refineBlocks(blocks);
        }

        abstractDP.resetDuplicates();
        for (AbstractBlock block : blocksToUse) {
            final ComparisonIterator iterator = block.getComparisonIterator();
            while (iterator.hasNext()) {
                final Comparison comp = iterator.next();
                abstractDP.isSuperfluous(comp.getEntityId1(), comp.getEntityId2());
            }
        }

        int counter = 0;
        for (IdDuplicates duplicatesPair : abstractDP.getFalseNegatives()) {
            final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(duplicatesPair.getEntityId2()) : profilesD1.get(duplicatesPair.getEntityId2());

            sb.append("<obj/" + "record/").append(duplicatesPair.toString()).append("> ");
            sb.append("<url1> ");
            sb.append("\"").append(profile1.getEntityUrl().replace("&", "")).append("\".\n");

            sb.append("<obj/" + "record/").append(duplicatesPair.toString()).append("> ");
            sb.append("<url2> ");
            sb.append("\"").append(profile2.getEntityUrl().replace("&", "")).append("\".\n");

            sb.append("<obj/" + "record/").append(duplicatesPair.toString()).append("> ");
            sb.append("<pairType> ");
            sb.append("\"" + "FN" + "\".\n"); // false negative

            sb.append("<obj/" + "record/").append(duplicatesPair.toString()).append("> ");
            sb.append("<Profile1> ");
            sb.append("\"").append((profile1 + "").replace("&", "")).append("\".\n");

            sb.append("<obj/" + "record/").append(duplicatesPair.toString()).append("> ");
            sb.append("<Profile2> ");
            sb.append("\"").append((profile2 + "").replace("&", "")).append("\".\n");

            //execute query every 1000 steps
            if (counter % 1000 == 0) {
                sb.append("}\n }");
                String sparqlQueryString = sb.toString();

                //System.out.println(sparqlQueryString);
                UpdateRequest update = UpdateFactory.create(sparqlQueryString);
                UpdateProcessor qexec = UpdateExecutionFactory.createRemote(update, endpointURL);
                qexec.execute();
                sb.setLength(0);
                sb.append(sparqlQueryString1);
            }
        }

        if (counter % 1000 != 0) {
            sb.append("}\n }");
            String sparqlQueryString = sb.toString();

            //System.out.println(sparqlQueryString);
            UpdateRequest update = UpdateFactory.create(sparqlQueryString);
            UpdateProcessor qexec = UpdateExecutionFactory.createRemote(update, endpointURL);
            qexec.execute();
        }
    }

    public void printStatistics(float overheadTime, String methodConfiguration, String methodName) {
        if (blocks.isEmpty()) {
            return;
        }

        System.out.println("\n\n\n**************************************************");
        System.out.println("Performance of : " + methodName);
        System.out.println("Configuration : " + methodConfiguration);
        System.out.println("**************************************************");
        System.out.println("No of blocks\t:\t" + blocks.size());
        System.out.println("Aggregate cardinality\t:\t" + aggregateCardinality);
        System.out.println("CC\t:\t" + (blockAssignments / aggregateCardinality));
        if (blocks.get(0) instanceof BilateralBlock) {
            System.out.println("Total entities D1\t:\t" + entityIndex.getDatasetLimit());
            System.out.println("Singleton entities D1\t:\t" + (entityIndex.getDatasetLimit() - noOfD1Entities));
            System.out.println("Total entities D2\t:\t" + (entityIndex.getNoOfEntities() - entityIndex.getDatasetLimit()));
            System.out.println("Singleton entities D2\t:\t" + (entityIndex.getNoOfEntities() - entityIndex.getDatasetLimit() - noOfD2Entities));
            System.out.println("Entities in blocks\t:\t" + (noOfD1Entities + noOfD2Entities));
            System.out.println("Average block\t:\t" + d1BlockAssignments / blocks.size() + "-" + d2BlockAssignments / blocks.size());
            System.out.println("iBC_1\t:\t" + d1BlockAssignments / noOfD1Entities);
            System.out.println("iBC_2\t:\t" + d2BlockAssignments / noOfD2Entities);
            System.out.println("oBC\t:\t" + ((d1BlockAssignments + d2BlockAssignments) / (noOfD1Entities + noOfD2Entities)));
        } else if (blocks.get(0) instanceof DecomposedBlock) {
            if (isCleanCleanER) {
                System.out.println("Entities in blocks\t:\t" + (noOfD1Entities + noOfD2Entities));
            } else {
                System.out.println("Entities in blocks\t:\t" + noOfD1Entities);
            }
        } else if (blocks.get(0) instanceof UnilateralBlock) {
            System.out.println("Total entities\t:\t" + entityIndex.getNoOfEntities());
            System.out.println("Entities in blocks\t:\t" + noOfD1Entities);
            System.out.println("Singleton entities\t:\t" + (entityIndex.getNoOfEntities() - noOfD1Entities));
            System.out.println("Average block\t:\t" + blockAssignments / blocks.size());
            System.out.println("BC\t:\t" + blockAssignments / noOfD1Entities);
        }
        System.out.println("Detected duplicates\t:\t" + detectedDuplicates);
        System.out.println("PC\t:\t" + pc);
        System.out.println("PQ\t:\t" + pq);
        System.out.println("F-Measure\t:\t" + fMeasure);
        System.out.println("Overhead time\t:\t" + overheadTime);
    }

    private void setComparisonsCardinality() {
        aggregateCardinality = 0;
        blockAssignments = 0;
        for (AbstractBlock block : blocks) {
            aggregateCardinality += block.getNoOfComparisons();
            blockAssignments += block.getTotalBlockAssignments();
        }
    }

    public void setStatistics() {
        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return;
        }

        setType();
        setComparisonsCardinality();
        if (blocks.get(0) instanceof DecomposedBlock) {
            getDecomposedBlocksEntities();
        } else {
            entityIndex = new GroundTruthIndex(blocks, abstractDP.getDuplicates());
            getEntities();
        }
        if (blocks.get(0) instanceof BilateralBlock) {
            getBilateralBlockingCardinality();
        }
        if (blocks.get(0) instanceof DecomposedBlock) {
            getDuplicatesOfDecomposedBlocks();
        } else {
            getDuplicatesWithEntityIndex();
        }
    }

    private void setType() {
        if (blocks.get(0) instanceof BilateralBlock) {
            isCleanCleanER = true;
        } else if (blocks.get(0) instanceof DecomposedBlock) {
            DecomposedBlock deBlock = (DecomposedBlock) blocks.get(0);
            isCleanCleanER = deBlock.isCleanCleanER();
        } else if (blocks.get(0) instanceof UnilateralBlock) {
            isCleanCleanER = false;
        }
    }
}
