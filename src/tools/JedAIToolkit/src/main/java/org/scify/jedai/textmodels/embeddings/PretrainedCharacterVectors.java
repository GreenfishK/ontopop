package org.scify.jedai.textmodels.embeddings;

import com.esotericsoftware.minlog.Log;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;

public class PretrainedCharacterVectors extends PretrainedVectors {

    /**
     * Constructor
     *
     * @param dId
     * @param n
     * @param md
     * @param sMetric
     * @param iName
     */
    public PretrainedCharacterVectors(int dId, int n, RepresentationModel md, SimilarityMetric sMetric, String iName) {
        super(dId, n, md, sMetric, iName);
        // Log.set(Log.LEVEL_DEBUG);
    }

    void addCharacterVector(float[] cvec, float[] aggrVec) {
        for (int i = 0; i < dimension; ++i) {
            aggrVec[i] += cvec[i];
        }
    }

    float[] createCharacterWordVector(String token) {
        float[] char_word_vector = getZeroVector();
        int num_token_chars = 0;
        int char_idx = 0;
        for (char c : token.toCharArray()) {
            char_idx++;
            if (elementMap.containsKey(Character.toString(c))) {
                Log.debug(String.format("Adding char %d/%d : %c from token %s to vector.", char_idx, token.length(), c, token));
                num_token_chars++;
                addCharacterVector(elementMap.get(Character.toString(c)), char_word_vector);
            } else {
                handleUnknown(token);
            }
        }
        if (num_token_chars == 0) {
            return null;
        }
        // normalize
        Log.debug(String.format("Averaging character embedding from %d characters", num_token_chars));
        for (int i = 0; i < dimension; ++i) {
            char_word_vector[i] /= num_token_chars;
        }
        return char_word_vector;

    }

    @Override
    public float getEntropy(boolean normalized) {
        return 0;
    }

    /**
     * Updates the model with the input text. Tokenizes, maps each token to the
     * aggregate vector.
     *
     * @param text
     */
    @Override
    public void updateModel(String text) {
        int localUpdates = 0;
        final String[] tokens = text.toLowerCase().split("[\\W_]");
        for (String token : tokens) {
            float[] wordVector = createCharacterWordVector(token);
            if (wordVector != null) {
                addVector(wordVector);
            }
            localUpdates++;
        }
        Log.debug("Used " + localUpdates + " element(s) from [text]: [" + text + "]");
    }

}
