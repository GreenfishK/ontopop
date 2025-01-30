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
package org.scify.jedai.utilities.enumerations;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author G.A.P. II
 */
public enum SimilarityMetric {
    ARCS_SIMILARITY,
    COSINE_SIMILARITY,
    ENHANCED_JACCARD_SIMILARITY,
    GENERALIZED_JACCARD_SIMILARITY,
    GRAPH_CONTAINMENT_SIMILARITY,
    GRAPH_NORMALIZED_VALUE_SIMILARITY,
    GRAPH_VALUE_SIMILARITY,
    GRAPH_OVERALL_SIMILARITY,
    JACCARD_SIMILARITY,
    SIGMA_SIMILARITY,
    WEIGHTED_JACCARD_SIMILARITY;

    public static List<SimilarityMetric> getModelCompatibleSimMetrics(RepresentationModel model) {
        final List<SimilarityMetric> simMetrics = new ArrayList<>();
        switch (model) {
            case CHARACTER_BIGRAMS:
            case CHARACTER_FOURGRAMS:
            case CHARACTER_TRIGRAMS:
            case TOKEN_BIGRAMS:
            case TOKEN_TRIGRAMS:
            case TOKEN_UNIGRAMS:
                simMetrics.add(COSINE_SIMILARITY);
                simMetrics.add(ENHANCED_JACCARD_SIMILARITY);
                simMetrics.add(GENERALIZED_JACCARD_SIMILARITY);
                simMetrics.add(JACCARD_SIMILARITY);
                break;
            case CHARACTER_BIGRAMS_TF_IDF:
            case CHARACTER_FOURGRAMS_TF_IDF:
            case CHARACTER_TRIGRAMS_TF_IDF:
            case TOKEN_BIGRAMS_TF_IDF:
            case TOKEN_TRIGRAMS_TF_IDF:
            case TOKEN_UNIGRAMS_TF_IDF:
                simMetrics.add(ARCS_SIMILARITY);
                simMetrics.add(COSINE_SIMILARITY);
                simMetrics.add(GENERALIZED_JACCARD_SIMILARITY);
                simMetrics.add(SIGMA_SIMILARITY);
                break;
            case CHARACTER_BIGRAM_GRAPHS:
            case CHARACTER_FOURGRAM_GRAPHS:
            case CHARACTER_TRIGRAM_GRAPHS:
            case TOKEN_BIGRAM_GRAPHS:
            case TOKEN_TRIGRAM_GRAPHS:
            case TOKEN_UNIGRAM_GRAPHS:
                simMetrics.add(GRAPH_CONTAINMENT_SIMILARITY);
                simMetrics.add(GRAPH_NORMALIZED_VALUE_SIMILARITY);
                simMetrics.add(GRAPH_OVERALL_SIMILARITY);
                simMetrics.add(GRAPH_VALUE_SIMILARITY);
                break;
//            case PRETRAINED_WORD_VECTORS:
//            case PRETRAINED_CHARACTER_VECTORS:
//                simMetrics.add(COSINE_SIMILARITY);
//                try euclidean distance, too!
//                break;
            default:
                return null;
        }
        return simMetrics;
    }

    public static SimilarityMetric getModelDefaultSimMetric(RepresentationModel model) {
        switch (model) {
            case CHARACTER_BIGRAMS:
                return JACCARD_SIMILARITY;
            case CHARACTER_BIGRAM_GRAPHS:
                return GRAPH_VALUE_SIMILARITY;
            case CHARACTER_BIGRAMS_TF_IDF:
                return COSINE_SIMILARITY;
            case CHARACTER_FOURGRAMS:
                return JACCARD_SIMILARITY;
            case CHARACTER_FOURGRAM_GRAPHS:
                return GRAPH_VALUE_SIMILARITY;
            case CHARACTER_FOURGRAMS_TF_IDF:
                return COSINE_SIMILARITY;
            case CHARACTER_TRIGRAMS:
                return JACCARD_SIMILARITY;
            case CHARACTER_TRIGRAM_GRAPHS:
                return GRAPH_VALUE_SIMILARITY;
            case CHARACTER_TRIGRAMS_TF_IDF:
                return COSINE_SIMILARITY;
            case TOKEN_BIGRAMS:
                return COSINE_SIMILARITY;
            case TOKEN_BIGRAMS_TF_IDF:
                return COSINE_SIMILARITY;
            case TOKEN_BIGRAM_GRAPHS:
                return GRAPH_VALUE_SIMILARITY;
            case TOKEN_TRIGRAMS:
                return COSINE_SIMILARITY;
            case TOKEN_TRIGRAMS_TF_IDF:
                return COSINE_SIMILARITY;
            case TOKEN_TRIGRAM_GRAPHS:
                return GRAPH_VALUE_SIMILARITY;
            case TOKEN_UNIGRAMS:
                return COSINE_SIMILARITY;
            case TOKEN_UNIGRAMS_TF_IDF:
                return COSINE_SIMILARITY;
            case TOKEN_UNIGRAM_GRAPHS:
                return GRAPH_VALUE_SIMILARITY;
//            case PRETRAINED_WORD_VECTORS:
//                return COSINE_SIMILARITY;
//            case PRETRAINED_CHARACTER_VECTORS:
//                return COSINE_SIMILARITY;
//                try euclidean distance, too!

            default:
                return null;
        }
    }
}
