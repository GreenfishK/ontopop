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
package org.scify.jedai.similarityjoins.fuzzysets;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

public class FuzzySetSimJoin {

    TObjectIntMap<String> tokenDict;
    float[] elementBounds;

    /**
     * Computes the join between two collections.
     */
    public HashMap<String, Float> join(Map<String, List<Set<String>>> input1, Map<String, List<Set<String>>> input2,
            float simThreshold) {
        /* TRANSFORM THE INPUT COLLECTIONS */

        // Create an empty token dictionary
        tokenDict = new TObjectIntHashMap<>();
        // use instead the following if order by frequency is needed
        // TObjectIntMap<String> tokenDict =
        // it.mapTokensToIntsByFrequency(input2.values());

        // Transform input to integer tokens
        // (dictionary is built on input2)
        int[][][] collection2 = transform(input2, tokenDict);
        int[][][] collection1 = transform(input1, tokenDict);

        /* JOIN THE TRANSFORMED INPUT COLLECTIONS */
        HashMap<String, Float> matchingPairs = join(collection1, collection2, simThreshold);
        return matchingPairs;
    }

    /**
     * Computes the join between two already transformed and indexed collections
     *
     * @param collection1
     * @param collection2
     * @param simThreshold
     * @return
     */
    HashMap<String, Float> join(int[][][] collection1, int[][][] collection2, float simThreshold) {
        final HashMap<String, Float> matchingPairs = new HashMap<>();

        /* CREATE INDEX */
        TIntObjectMap<TIntList>[] idx = buildSetInvertedIndex(collection2, tokenDict.size());

        /* EXECUTE THE JOIN ALGORITHM */
        for (int i = 0; i < collection1.length; i++) {
            TIntFloatHashMap matches = search(collection1[i], collection2, simThreshold, idx);
            for (int j : matches.keys()) {
                matchingPairs.put(i + "_" + j, matches.get(j));
            }
        }

        return matchingPairs;
    }

    /**
     * Find matches for a given set
     */
    private TIntFloatHashMap search(int[][] querySet, int[][][] collection, float simThreshold,
            TIntObjectMap<TIntList>[] idx) {

        /* SIGNATURE GENERATION */
        TIntSet[] unflattenedSignature = computeUnflattenedSignature(querySet, simThreshold, idx);

        /* CANDIDATE SELECTION AND CHECK FILTER */
        TIntObjectMap<TIntFloatMap> checkFilterCandidates = applyCheckFilter(querySet, collection,
                unflattenedSignature, idx, simThreshold);

        /* NEAREST NEIGHBOR FILTER */
        TIntSet nnFilterCandidates = applyNNFilter(querySet, collection, checkFilterCandidates, simThreshold);

        /* VERIFICATION */
        TIntFloatHashMap matches = verifyCandidates(querySet, collection, nnFilterCandidates, simThreshold);

        return matches;
    }

    private TIntSet[] computeUnflattenedSignature(int[][] querySet, float simThreshold,
            TIntObjectMap<TIntList>[] idx) {

        // initialize unflattened signature
        TIntSet[] unflattenedSignature = new TIntHashSet[querySet.length];
        for (int i = 0; i < unflattenedSignature.length; i++) {
            unflattenedSignature[i] = new TIntHashSet();
        }

        // Compute token scores
        float score;
        TIntFloatMap tokenScores = new TIntFloatHashMap();
        // first compute values
        for (int[] ints : querySet) {
            for (int anInt : ints) {
                score = 0;
                if (tokenScores.containsKey(anInt)) {
                    score = tokenScores.get(anInt);
                }
                score += (1.0 / ints.length);
                tokenScores.put(anInt, score);
            }
        }
        // then include costs
        int cost;
        for (int token : tokenScores.keys()) {
            if (token < 0) {
                tokenScores.put(token, 0);
            } else {
                cost = 0;
                for (int s : idx[token].keys()) {
                    cost += idx[token].get(s).size();
                }
                tokenScores.put(token, cost / tokenScores.get(token));
            }
        }

        // set threshold and current bound
        float thres = simThreshold * querySet.length;
        float simUpperBound = querySet.length;

        // construct the signature
        int bestToken = -1;
        float bestScore = Float.MAX_VALUE;
        while (simUpperBound >= thres) {
            // choose the next best token
            for (int token : tokenScores.keys()) {
                score = tokenScores.get(token);
                if (score < bestScore) {
                    bestToken = token;
                    bestScore = score;
                }
            }

            // remove it from the pool of tokens and reset best score
            tokenScores.remove(bestToken);
            bestScore = Float.MAX_VALUE;

            // update the signature
            for (int i = 0; i < querySet.length; i++) {
                for (int j = 0; j < querySet[i].length; j++) {
                    if (querySet[i][j] == bestToken) {
                        unflattenedSignature[i].add(bestToken);
                        simUpperBound -= (1.0f / querySet[i].length);
                    }
                }
            }
        }

        return unflattenedSignature;
    }

    private TIntObjectMap<TIntFloatMap> applyCheckFilter(int[][] querySet, int[][][] collection,
            TIntSet[] unflattenedSignature, TIntObjectMap<TIntList>[] idx, float simThreshold) {

        TIntObjectMap<TIntFloatMap> checkFilterCandidates = new TIntObjectHashMap<>();

        TIntFloatMap cachedScores;
        float sim;

        // compute bounds for length filter
        float minLength = querySet.length * simThreshold;
        float maxLength = querySet.length / simThreshold;

        // compute the individual element bounds
        elementBounds = new float[querySet.length];
        for (int i = 0; i < querySet.length; i++) {
            elementBounds[i] = (float) (querySet[i].length - unflattenedSignature[i].size())
                    / (float) querySet[i].length;
        }

        // for each element of the query set
        for (int i = 0; i < querySet.length; i++) {

            // for each token, retrieve candidates from index
            for (int token : unflattenedSignature[i].toArray()) {
                if (token < 0) {
                    continue;
                }

                for (int s : idx[token].keys()) {
                    // Apply length filter
                    if (collection[s].length < minLength || collection[s].length > maxLength) {
                        continue;
                    }

                    for (int e : idx[token].get(s).toArray()) {

                        // compute the similarity score
                        sim = jaccard(querySet[i], collection[s][e]);

                        // check the condition
                        if (sim >= elementBounds[i]) {
                            cachedScores = checkFilterCandidates.get(s);
                            if (cachedScores == null) {
                                cachedScores = new TIntFloatHashMap();
                                cachedScores.put(i, sim);
                            } else if (sim > cachedScores.get(i)) {
                                cachedScores.put(i, sim);
                            }
                            checkFilterCandidates.put(s, cachedScores);
                        }
                    }
                }
            }
        }

        return checkFilterCandidates;
    }

    private TIntSet applyNNFilter(int[][] querySet, int[][][] collection,
            TIntObjectMap<TIntFloatMap> checkFilterCandidates, float simThreshold) {

        TIntSet nnFilterCandidates = new TIntHashSet();

        float sim, maxSim, total;
        TIntSet matchedElements;

        candLoop:
        for (int c : checkFilterCandidates.keys()) {
            total = 0;
            matchedElements = new TIntHashSet();

            for (int e : checkFilterCandidates.get(c).keys()) {
                matchedElements.add(e);
                total += checkFilterCandidates.get(c).get(e);
            }

            for (int j = 0; j < querySet.length; j++) {
                if (matchedElements.contains(j)) {
                    continue;
                }
                maxSim = 0;
                for (int k = 0; k < collection[c].length; k++) {
                    sim = jaccard(querySet[j], collection[c][k]);
                    if (sim > maxSim) {
                        maxSim = sim;
                    }
                }
                total += maxSim;
                matchedElements.add(j);
                if (total < simThreshold * matchedElements.size()) {
                    continue candLoop;
                }
            }
            if (total >= simThreshold * querySet.length) {
                nnFilterCandidates.add(c);
            }
        }

        return nnFilterCandidates;
    }

    private TIntFloatHashMap verifyCandidates(int[][] querySet, int[][][] collection, TIntSet nnFilterCandidates,
            float simThreshold) {

        TIntSet matches = new TIntHashSet();
        TIntFloatHashMap matches2 = new TIntFloatHashMap();
        TIntIterator sit = nnFilterCandidates.iterator();

        while (sit.hasNext()) {
            int id_s = sit.next();

            SimpleWeightedGraph<String, DefaultWeightedEdge> g = new SimpleWeightedGraph<>(
                    DefaultWeightedEdge.class);
            HashSet<String> r_partition = new HashSet<>();
            HashSet<String> s_partition = new HashSet<>();

            for (int id_r = 0; id_r < querySet.length; id_r++) {
                String r_v = "r_" + id_r;
                g.addVertex(r_v);
                r_partition.add(r_v);
                for (int id_ss = 0; id_ss < collection[id_s].length; id_ss++) {
                    String s_v = "s_" + id_ss;
                    float sim = jaccard(querySet[id_r], collection[id_s][id_ss]);
                    g.addVertex(s_v);
                    s_partition.add(s_v);
                    DefaultWeightedEdge e = g.addEdge(r_v, s_v);
                    g.setEdgeWeight(e, sim);
                }
            }

            float match = 0.0f;
            MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> matching = new MaximumWeightBipartiteMatching<>(
                    g, r_partition, s_partition);
            for (DefaultWeightedEdge ed : matching.getMatching().getEdges()) {
                match += g.getEdgeWeight(ed);
            }

            float sim = match / (querySet.length + collection[id_s].length - match);
            if (sim >= simThreshold) {
                matches.add(id_s);
                matches2.put(id_s, sim);
            }
        }

        return matches2;

    }

    private static float jaccard(int[] r, int[] s) {
        TIntSet nr = new TIntHashSet(r);
        TIntSet ns = new TIntHashSet(s);
        TIntSet intersection = new TIntHashSet(nr);
        intersection.retainAll(ns);
        TIntSet union = new TIntHashSet(nr);
        union.addAll(ns);
        return ((float) intersection.size()) / ((float) union.size());
    }
    
    TIntObjectMap<TIntList>[] buildSetInvertedIndex(int[][][] collection, int numTokens) {
        // initialize the index
        @SuppressWarnings("unchecked")
        TIntObjectMap<TIntList>[] idx = new TIntObjectHashMap[numTokens];
        for (int i = 0; i < idx.length; i++) {
            idx[i] = new TIntObjectHashMap<>();
        }

        // populate the index
        TIntList invList;
        int token;
        for (int i = 0; i < collection.length; i++) {
            for (int j = 0; j < collection[i].length; j++) {
                for (int k = 0; k < collection[i][j].length; k++) {
                    token = collection[i][j][k];
                    if (idx[token].containsKey(i)) {
                        invList = idx[token].get(i);
                    } else {
                        invList = new TIntArrayList();
                    }
                    invList.add(j);
                    idx[token].put(i, invList);
                }
            }
        }

        return idx;
    }
    
    int[][][] transform(Map<String, List<Set<String>>> input, TObjectIntMap<String> tokenDictionary) {
        int[][][] collection = new int[input.size()][][];

        boolean existingDictionary = tokenDictionary.size() > 0;
        int unknownTokenCounter = 0;

        int i = 0, j, k;
        List<Set<String>> elements;
        for (String set : input.keySet()) {
            elements = input.get(set);
            collection[i] = new int[elements.size()][];
            j = 0;
            for (Set<String> element : elements) {
                collection[i][j] = new int[element.size()];
                k = 0;
                for (String token : element) {
                    if (!tokenDictionary.containsKey(token)) {
                        if (existingDictionary) {
                            unknownTokenCounter--;
                            tokenDictionary.put(token, unknownTokenCounter);
                        } else {
                            tokenDictionary.put(token, tokenDictionary.size());
                        }
                    }
                    collection[i][j][k] = tokenDictionary.get(token);
                    k++;
                }
                j++;
            }
            i++;
        }

        return collection;
    }
}
