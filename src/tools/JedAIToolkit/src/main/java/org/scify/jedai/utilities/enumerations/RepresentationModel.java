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

import org.scify.jedai.textmodels.CharacterNGramGraphs;
import org.scify.jedai.textmodels.CharacterNGrams;
import org.scify.jedai.textmodels.CharacterNGramsWithGlobalWeights;
import org.scify.jedai.textmodels.ITextModel;
import org.scify.jedai.textmodels.TokenNGramGraphs;
import org.scify.jedai.textmodels.TokenNGrams;
import org.scify.jedai.textmodels.TokenNGramsWithGlobalWeights;
import org.scify.jedai.textmodels.embeddings.PretrainedCharacterVectors;
import org.scify.jedai.textmodels.embeddings.PretrainedVectors;
import org.scify.jedai.textmodels.embeddings.PretrainedWordVectors;

/**
 *
 * @author G.A.P. II
 */
public enum RepresentationModel {
    CHARACTER_BIGRAMS,
    CHARACTER_BIGRAMS_TF_IDF,
    CHARACTER_BIGRAM_GRAPHS,
    CHARACTER_TRIGRAMS,
    CHARACTER_TRIGRAMS_TF_IDF,
    CHARACTER_TRIGRAM_GRAPHS,
    CHARACTER_FOURGRAMS,
    CHARACTER_FOURGRAMS_TF_IDF,
    CHARACTER_FOURGRAM_GRAPHS,
    TOKEN_UNIGRAMS,
    TOKEN_UNIGRAMS_TF_IDF,
    TOKEN_UNIGRAM_GRAPHS,
    TOKEN_BIGRAMS,
    TOKEN_BIGRAMS_TF_IDF,
    TOKEN_BIGRAM_GRAPHS,
    TOKEN_TRIGRAMS,
    TOKEN_TRIGRAMS_TF_IDF,
    TOKEN_TRIGRAM_GRAPHS,
    PRETRAINED_WORD_VECTORS,
    PRETRAINED_CHARACTER_VECTORS;

    public static ITextModel getModel(int dId, RepresentationModel model, SimilarityMetric simMetric, String instanceName) {
        switch (model) {
            case CHARACTER_BIGRAMS:
                return new CharacterNGrams(dId, 2, model, simMetric, instanceName);
            case CHARACTER_BIGRAMS_TF_IDF:
                return new CharacterNGramsWithGlobalWeights(dId, 2, model, simMetric, instanceName);
            case CHARACTER_BIGRAM_GRAPHS:
                return new CharacterNGramGraphs(dId, 2, model, simMetric, instanceName);
            case CHARACTER_FOURGRAMS:
                return new CharacterNGrams(dId, 4, model, simMetric, instanceName);
            case CHARACTER_FOURGRAMS_TF_IDF:
                return new CharacterNGramsWithGlobalWeights(dId, 4, model, simMetric, instanceName);
            case CHARACTER_FOURGRAM_GRAPHS:
                return new CharacterNGramGraphs(dId, 4, model, simMetric, instanceName);
            case CHARACTER_TRIGRAMS:
                return new CharacterNGrams(dId, 3, model, simMetric, instanceName);
            case CHARACTER_TRIGRAMS_TF_IDF:
                return new CharacterNGramsWithGlobalWeights(dId, 3, model, simMetric, instanceName);
            case CHARACTER_TRIGRAM_GRAPHS:
                return new CharacterNGramGraphs(dId, 3, model, simMetric, instanceName);
            case TOKEN_BIGRAMS:
                return new TokenNGrams(dId, 2, model, simMetric, instanceName);
            case TOKEN_BIGRAMS_TF_IDF:
                return new TokenNGramsWithGlobalWeights(dId, 2, model, simMetric, instanceName);
            case TOKEN_BIGRAM_GRAPHS:
                return new TokenNGramGraphs(dId, 2, model, simMetric, instanceName);
            case TOKEN_TRIGRAMS:
                return new TokenNGrams(dId, 3, model, simMetric, instanceName);
            case TOKEN_TRIGRAMS_TF_IDF:
                return new TokenNGramsWithGlobalWeights(dId, 3, model, simMetric, instanceName);
            case TOKEN_TRIGRAM_GRAPHS:
                return new TokenNGramGraphs(dId, 3, model, simMetric, instanceName);
            case TOKEN_UNIGRAMS:
                return new TokenNGrams(dId, 1, model, simMetric, instanceName);
            case TOKEN_UNIGRAMS_TF_IDF:
                return new TokenNGramsWithGlobalWeights(dId, 1, model, simMetric, instanceName);
            case TOKEN_UNIGRAM_GRAPHS:
                return new TokenNGramGraphs(dId, 1, model, simMetric, instanceName);
            case PRETRAINED_WORD_VECTORS:
                return new PretrainedWordVectors(dId, 1, model, simMetric, instanceName);
            case PRETRAINED_CHARACTER_VECTORS:
                return new PretrainedCharacterVectors(dId, 1, model, simMetric, instanceName);
            default:
                return null;
        }
    }

    public static void resetGlobalValues(int datasetId, RepresentationModel model) {
        switch (model) {
            case CHARACTER_BIGRAMS_TF_IDF:
            case CHARACTER_FOURGRAMS_TF_IDF:
            case CHARACTER_TRIGRAMS_TF_IDF:
                CharacterNGramsWithGlobalWeights.resetGlobalValues(datasetId);
                break;
            case TOKEN_BIGRAMS_TF_IDF:
            case TOKEN_TRIGRAMS_TF_IDF:
            case TOKEN_UNIGRAMS_TF_IDF:
                TokenNGramsWithGlobalWeights.resetGlobalValues(datasetId);
                break;
            case CHARACTER_BIGRAMS:
            case CHARACTER_TRIGRAMS:
            case CHARACTER_FOURGRAMS:
                CharacterNGrams.resetGlobalValues(datasetId);
                break;
            case CHARACTER_BIGRAM_GRAPHS:
            case CHARACTER_TRIGRAM_GRAPHS:
            case CHARACTER_FOURGRAM_GRAPHS:
                CharacterNGramGraphs.resetGlobalValues(datasetId);
                break;
            case TOKEN_BIGRAMS:
            case TOKEN_TRIGRAMS:
            case TOKEN_UNIGRAMS:
                TokenNGrams.resetGlobalValues(datasetId);
                break;
            case TOKEN_BIGRAM_GRAPHS:
            case TOKEN_TRIGRAM_GRAPHS:
            case TOKEN_UNIGRAM_GRAPHS:
                TokenNGramGraphs.resetGlobalValues(datasetId);
                break;
            case PRETRAINED_WORD_VECTORS:
            case PRETRAINED_CHARACTER_VECTORS:
                PretrainedVectors.resetGlobalValues(datasetId);
                break;
            default:
        }
    }
}
