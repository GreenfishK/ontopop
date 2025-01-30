package org.scify.jedai.textmodels.embeddings;

import org.scify.jedai.textmodels.AbstractModel;
import org.scify.jedai.textmodels.ITextModel;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;

public abstract class VectorSpaceModel extends AbstractModel {

    float[] aggregateVector;
    static int dimension;

    public VectorSpaceModel(int dId, int n, RepresentationModel md, SimilarityMetric sMetric, String iName) {
        super(dId, n, md, sMetric, iName);
    }

    float[] getVector() {
        return aggregateVector;
    }

    int getDimension() {
        return dimension;
    }

    @Override
    public float getSimilarity(ITextModel oModel) {
        switch (simMetric) {
            case COSINE_SIMILARITY:
                return getCosineSimilarity((VectorSpaceModel) oModel);
            default:
                throw new IllegalStateException(
                    "The given similarity metric is incompatible with the bag representation model.");
        }
    }

    /**
     * Cosine similarity for two arithmetic vectors
     *
     * @param oModel the other VS model
     * @return the cosine similarity value
     */
    public float getCosineSimilarity(VectorSpaceModel oModel) {
        // get vectors
        float[] v1 = getVector();
        float[] v2 = oModel.getVector();
        float norm1 = 0.0f;
        float norm2 = 0.0f;
        float dot = 0.0f;
        for (int i = 0; i < getDimension(); ++i) {
            dot += v1[i] * v2[i];
            norm1 += Math.pow(v1[i], 2);
            norm2 += Math.pow(v2[i], 2);
        }
        return (float) (dot / (Math.sqrt(norm1) * Math.sqrt(norm2)));

    }

}
