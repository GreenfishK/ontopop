import pandas as pd
import re
import json
import logging
import os
import math
import numpy as np
from datetime import datetime

from sklearn.metrics.pairwise import cosine_similarity
from langchain_huggingface import HuggingFaceEmbeddings
from transformers import AutoTokenizer

##########################################################################################
# Paths, Endpoints, Tokens and Environment Variables
##########################################################################################
log_dir = f"/home/ontopop/logs/evaluate"
datasets_dir = f"/home/ontopop/data/generate"
output_dir = f"/home/ontopop/data/evaluate"

##########################################################################################
# Logging
##########################################################################################
# Read environment variables
llm_id=os.environ["LLM"]
llm_short=os.environ["LLM"].split("/")[1]
pdf_parser=os.environ["PDF_PARSER"]
shots=os.environ["SHOTS"]

# Logging configuration
log_file_path = f"{log_dir}/evaluate_{pdf_parser}_{llm_short}_{shots}.txt"

# Logging configuration
with open(log_file_path, "w") as log_file:
    log_file.write("")

logging.basicConfig(
    handlers=[logging.FileHandler(filename=log_file_path, encoding='utf-8', mode='a+')],
    format="%(asctime)s %(filename)s:%(levelname)s:%(message)s",
    datefmt="%F %A %T",
    level=logging.INFO
    )


##########################################################################################
# Functions
##########################################################################################
def compute_metrics(row, embeddings, total_rows):
    """
    Compute similarity between `propertyValue` and `propertyValuePrediction` for each row.
    - For each value in `propertyValue`, take max similarity with each value in `propertyValuePrediction`.
    - Average these max similarities.
    """
    paperORKGID = row.name[0].split("/")[-1]
    contributionORKGID  = row.name[1].split("/")[-1]
    propertyORKGID = row.name[2].split("/")[-1]
    row_index = row["rowIndex"]

    logging.info(f"{row_index}/{total_rows}: Computing semantic similarity for row: Paper ORKG ID: '{paperORKGID}' with Contribution ORKG ID: {contributionORKGID} and property ORKG ID: {propertyORKGID}.")

    # Get values, handling NaN or empty predictions
    reference_values = str(row["propertyValues"]).split("|") 
    gen_values = str(row["propertyValuePrediction"]).split("|") if len(str(row["errorType"])) != 0 else []

    if not gen_values:
        row["avgMaxSemanticSimilarity"] = 0
        return row
    
    try:
        # Embed reference and predicted property values
        true_embeddings = embeddings.embed_documents(reference_values) 
        pred_embeddings = embeddings.embed_documents(gen_values) 

        # Compute cosine similarity matrix
        similarities = cosine_similarity(true_embeddings, pred_embeddings)
        max_similarities = similarities.max(axis=1)
        row["avgMaxSemanticSimilarity"] = np.mean(max_similarities)
    except RuntimeError as e:
        row["errorType"] = "DeviceSideAssertionError_PropertyValueEmbeddings"
        row["avgMaxSemanticSimilarity"] = 0
        logging.error(e)

    return row

def compute_token_statistics(row, tokenizer, total_rows):
    paperORKGID = row.name[0].split("/")[-1]
    contributionORKGID  = row.name[1].split("/")[-1]
    propertyORKGID = row.name[2].split("/")[-1]
    row_index = row["rowIndex"]

    logging.info(f"{row_index}/{total_rows}: Computing token counts for row: Paper ORKG ID: '{paperORKGID}' with Contribution ORKG ID: {contributionORKGID} and property ORKG ID: {propertyORKGID}.")
    gen_values = str(row["propertyValuePrediction"]).split("|") if len(str(row["errorType"])) != 0 else []

    if not gen_values:
        row["avgTokenCountPropertyValuePrediction"] = 0
        return row
    
    cnt_property_value_tokens = []
    for property_value in gen_values:
        tokens = tokenizer.tokenize(property_value)
        cnt_property_value_tokens.append(len(tokens))
    avg_cnt_property_value_tokens = np.mean(cnt_property_value_tokens)
    row["avgTokenCountPropertyValuePrediction"] = avg_cnt_property_value_tokens

    return row


##########################################################################################
# Pipeline
##########################################################################################
input_dataset_file_name = f"ontopop_predicted_{pdf_parser}_{llm_short}_{shots}.csv"
output_dataset_file_name = f"ontopop_evaluation_{pdf_parser}_{llm_short}_{shots}.csv"

# Assertions
logging.info("Running assertions.")
assert os.path.exists(f"{datasets_dir}/{input_dataset_file_name}")
ontopop_df = pd.read_csv(f"{datasets_dir}/{input_dataset_file_name}", escapechar='\\')

# Cosine similarity computation
# Define new columns for compute_metrics and compute_token_statistics task
ontopop_df["avgMaxSemanticSimilarity"] = None
ontopop_df["avgTokenCountPropertyValuePrediction"] = None

# Set the index and datatypes
ontopop_df.set_index(["paper", "contributionInstance", "property"], inplace=True)

# Set Sentence Embeddings
logging.info("Load HuggingFaceEmbeddings (Sentence Embeddings) with the specified parameters.")
embeddings = HuggingFaceEmbeddings(
    model_name="sentence-transformers/all-MiniLM-L6-v2", 
    model_kwargs={'device':'cuda'}, 
    encode_kwargs={'normalize_embeddings': False}
)
   

# Computing metrics
logging.info("Computing the following metrics based on the crowdsourced and generated property values.")
logging.info("Metric: Avg Max Semantic Similarity")
total_rows = ontopop_df.shape[0]
ontopop_df = ontopop_df.apply(compute_metrics, args=(embeddings, total_rows), axis=1)

# Compute token counts per property
logging.info("Computing the following metrics based on the generated property values.")
logging.info("Metric: Average count of tokens per generated property value")
tokenizer = AutoTokenizer.from_pretrained(llm_id, token=os.environ["HF_TOKEN"])
ontopop_df = ontopop_df.apply(compute_token_statistics, args=(tokenizer, total_rows), axis=1)

# Save
logging.info(f"Saving populated dataset to {output_dir}/{output_dataset_file_name}")
ontopop_df.reset_index(inplace=True)
ontopop_df.to_csv(f"{output_dir}/{output_dataset_file_name}")