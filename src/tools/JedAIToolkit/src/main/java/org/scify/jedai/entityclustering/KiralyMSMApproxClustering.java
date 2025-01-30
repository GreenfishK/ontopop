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
package org.scify.jedai.entityclustering;

import com.esotericsoftware.minlog.Log;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityEdge;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.utilities.comparators.DecSimilarityEdgeComparator;

import java.util.*;

/**
 * Implements the so-called "New Algorithm" by Zoltan Kiraly 2013, which is a 3/2-approximation to the
 * Maximum Stable Marriage (MSM) problem.
 * @author vefthym
 */
public class KiralyMSMApproxClustering extends AbstractCcerEntityClustering {
    private static final long serialVersionUID = 274454050200034674L;

    class SimilarityEdgeExt extends SimilarityEdge { //extension of similarity edge, just for the needs of this method
        private static final long serialVersionUID = -3489398478601924151L;
        boolean isActive;

        public SimilarityEdgeExt (int pos1, int pos2, float sim) {
            super(pos1, pos2, sim);
            this.isActive = true; //active edge by default
        }

        public void setActive(boolean active) {
            this.isActive = active;
        }

        public boolean isActive() {
            return isActive;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SimilarityEdgeExt that = (SimilarityEdgeExt) o;
            return getModel1Pos() == that.getModel1Pos() && getModel2Pos() == that.getModel2Pos() && Float.compare(that.getSimilarity(), getSimilarity()) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getModel1Pos(), getModel2Pos(), getSimilarity());
        }
    }

    public KiralyMSMApproxClustering() {
        this(0.1f);
    }

    public KiralyMSMApproxClustering(float simTh) {
        super(simTh);
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }
    
    private boolean accepts_proposal(int w, int ws_fiance, int m, List<SimilarityEdgeExt> ws_preferences, boolean[] isUncertain){
        if (ws_fiance == -1) { // w is free, so she accepts m's proposal
            //.println(w + " accepts " + m + "'s proposal, since she is free.");
            return true;
        }
        if (isUncertain[ws_fiance]) { // w is flighty, so she accepts m's proposal
            //System.out.println(w + " accepts " + m + "'s proposal, since she is flighty.");
            return true;
        }
        double m_score = 0;
        double ws_fiance_score = 0;
        for (SimilarityEdge next_pref : ws_preferences) {
            int next_id = next_pref.getModel1Pos();
            if (next_id == m) {
                m_score = next_pref.getSimilarity();
            } else if (next_id == ws_fiance) {
                ws_fiance_score = next_pref.getSimilarity();
            }
        }
        if (m_score > ws_fiance_score) { // w prefers the new proposer to her existing fiance, so she accepts proposal
            //System.out.println(w + " accepts " + m + "'s proposal, since she prefers him to her fiance "+ ws_fiance+".");
            return true;
        }
        //System.out.println(w + " rejects " + m + "'s proposal.");
        return false;
    }

    private boolean isActive(List<SimilarityEdgeExt> prefs) {
        if (prefs == null) {
            return false;
        }
        for (SimilarityEdgeExt cand: prefs) {
            if (cand.isActive()) {
                return true;
            }
        }
        return false;
    }

    private boolean deactivateWomanCandidate(List<SimilarityEdgeExt> prefs, int w) {
        if (prefs == null) {
            return false;
        }
        for (SimilarityEdgeExt cand: prefs) {
            if (cand.getModel2Pos() == 2 && cand.isActive()) {
                cand.setActive(false);
                return true;
            }
        }
        return false;
    }

    @Override
    public EquivalenceCluster[] getDuplicates(SimilarityPairs simPairs) {
        Log.info("Input comparisons\t:\t" + simPairs.getNoOfComparisons());
        
        matchedIds.clear();
        if (simPairs.getNoOfComparisons() == 0) {
            return new EquivalenceCluster[0];
        }

        initializeData(simPairs);
        if (!isCleanCleanER) {
            return null; //the method is only applicable to Clean-Clean ER
        }

        // create as many PQs (initially empty) as the number of entities in collection 1
        List<List<SimilarityEdgeExt>> SEqueuesMen = new ArrayList<>();
        for (int i =0; i < datasetLimit; ++i) {
            SEqueuesMen.add(i, new ArrayList<>());
        }

        // create as many PQs (initially empty) as the number of entities in collection 2
        List<List<SimilarityEdgeExt>> SEqueuesWomen = new ArrayList<>();
        for (int i =0; i < noOfEntities-datasetLimit; ++i) {
            SEqueuesWomen.add(i, new ArrayList<>()); //new DecSimilarityEdgeComparator());
        }

        Set<Integer> men = new HashSet<>();
        Set<Integer> women = new HashSet<>();

        // add all candidates per node to the respective PQ of this node
        final Iterator<Comparison> iterator = simPairs.getPairIterator();
        while (iterator.hasNext()) { // add a similarity edge to the queue, for every pair of entities with a weight higher than the threshold
            Comparison comparison = iterator.next();
            int eid1 = comparison.getEntityId1();
            int eid2 = comparison.getEntityId2();
            if (threshold < comparison.getUtilityMeasure()) {
                SEqueuesMen.get(eid1).add(new SimilarityEdgeExt(eid1, eid2 + datasetLimit, comparison.getUtilityMeasure()));
                SEqueuesWomen.get(eid2).add(new SimilarityEdgeExt(eid1, eid2 + datasetLimit, comparison.getUtilityMeasure()));
            }
            men.add(eid1);
            women.add(eid2);
        }

        //sort all local candidates lists (for men and women) in desc. similarity
        for (List<SimilarityEdgeExt> prefs : SEqueuesMen) {
            Collections.sort(prefs, new DecSimilarityEdgeComparator());
        }
        for (List<SimilarityEdgeExt> prefs : SEqueuesWomen) {
            Collections.sort(prefs, new DecSimilarityEdgeComparator());
        }

        boolean[] isBachelor = new boolean[datasetLimit]; //checks if men are lads (false) or bachelors (true)
        Arrays.fill(isBachelor, Boolean.FALSE);

        //uncertain: an engaged man whose list contains a woman he prefers to his actual fiancee
        boolean[] isUncertain = Arrays.copyOf(isBachelor,datasetLimit); // another boolean array, initially false

        int[] fiances = new int[noOfEntities-datasetLimit]; //the fiance (man id) of each woman (-1 for unengaged)
        Arrays.fill(fiances, -1);

        List<SimilarityEdgeExt> currMatches = new ArrayList<>();

        Queue<Integer> freeMen = new LinkedList<>(men);
        while (!freeMen.isEmpty()) { // while there exists and active man m
            int m = freeMen.poll(); // eid1 (next free man)
            // check if there are any options for m, or if his list of candidates is empty
            if (!isActive(SEqueuesMen.get(m))) {
                //System.out.print(m + " runs out of options. ");
                //first time here for this man? YES: he (a lad) becomes a bachelor, NO: he (a bachelor) becomes an old bachelor
                if (!isBachelor[m]) {  // first time here: m becomes a bachelor and resets his PQ
                    //System.out.println("He now becomes a bachelor.");
                    isBachelor[m] = true; // m becomes a bachelor
                    if (!SEqueuesMen.get(m).isEmpty()) {
                        freeMen.add(m); // m is free (active) again
                    }
                    // reset m's PQ and thus, re-activate m
                    for (SimilarityEdgeExt cand : SEqueuesMen.get(m)) {
                        cand.setActive(true);
                    }
                } else {    // second time here: m, a bachelor, now becomes an old bachelor (free but inactive)
                    //System.out.println("He already was a bachelor. Now he becomes an old bachelor (inactive).");
                    continue; // we already removed m from freeMen (since we used .poll() to retrieve him)
                }
            } else { // m is still active (lad or bachelor) -> m proposes to his favorite woman w
                int w = -1;
                for (SimilarityEdgeExt cand : SEqueuesMen.get(m)) {
                    if (cand.isActive()) {
                        w = cand.getModel2Pos();
                        break;
                    }
                }
                int ws_fiance = fiances[w-datasetLimit]; // w's fiance (-1 if none)
                if (ws_fiance == -1) { // w is free, so she becomes engaged to m
                    fiances[w-datasetLimit] = m; //w's new fiance is m
                    currMatches.add(new SimilarityEdgeExt(m,w,0));
                    //System.out.println(w + " accepts " + m + "'s proposal, since she is free.");
                } else { // w is already engaged to ws_fiance
                    if (accepts_proposal(w, ws_fiance,m,SEqueuesWomen.get(w-datasetLimit),isUncertain)) {
                        currMatches.remove(new SimilarityEdgeExt(ws_fiance,w,0)); //w breaks up with ws_fiance
                        currMatches.add(new SimilarityEdgeExt(m, w, 0)); // m and w get engaged
                        fiances[w-datasetLimit] = m;
                        freeMen.add(ws_fiance); // w's ex-fiance now becomes free again
                        if (!isUncertain[ws_fiance]) { //w is not flighty //TODO: check when isUncertain becomes true
                            deactivateWomanCandidate(SEqueuesMen.get(ws_fiance), w); //w's ex-fiance removes w from his list
                        }
                    } else { // m's proposal is rejected by w
                        deactivateWomanCandidate(SEqueuesMen.get(ws_fiance), w); // m removes w from his list
                    }
                }
            }
            //System.out.println("current matches: ");
//            for (SimilarityEdge currMatch: currMatches) {
//                System.out.println(currMatch);
//            }
        } // end of loop looking for active men

        // store the final results
        for (SimilarityEdge match : currMatches) {
            similarityGraph.addEdge(match.getModel1Pos(), match.getModel2Pos());
        }

        return getConnectedComponents();
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": a 3/2 approximation of the Maximum Stable Marriage problem (suggested by Kiraly)";
    }

    @Override
    public String getMethodName() {
        return "KiralyMSMApprox Clustering";
    }
}
