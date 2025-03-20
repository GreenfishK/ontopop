##########################################################################################
# Imports, diretories setup and tokens
##########################################################################################
import pandas as pd
import re
import logging
import os
from langchain_core.documents.base import Document
from langchain_community.vectorstores import FAISS
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_huggingface import HuggingFaceEmbeddings
from transformers import AutoModelForCausalLM, AutoTokenizer, pipeline
import torch
import xml.etree.ElementTree as ET
import re

##########################################################################################
# Paths, Endpoints, Tokens and Environment Variables
##########################################################################################
queries = f"/home/ontopop/src/ontopop/sparql_queries/orkg"
instructions_dir = f"/home/ontopop/src/ontopop/instructions"

log_dir = f"/home/ontopop/logs/generate"
parsed_pdf_dir = f"/home/ontopop/data/create_dataset/parsed_papers"
prompts_dir = f"/home/ontopop/data/generate/prompts"
datasets_dir = f"/home/ontopop/data/create_dataset"
generation_dir = f"/home/ontopop/data/generate"
model_dir= f"/home/ontopop/models"

# Endpoints
endpoint_url_orkg = "https://orkg.org/triplestore"

##########################################################################################
# Logging
##########################################################################################
# Read environment variables
llm_id=os.environ["LLM"]
llm_short=os.environ["LLM"].split("/")[1]
pdf_parser=os.environ["PDF_PARSER"]
shots=os.environ["SHOTS"]

# Logging configuration
log_file_path = f"{log_dir}/generate_{pdf_parser}_{llm_short}_{shots}.txt"
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
def predict_property(row, embeddings, llm, tokenizer, total_rows, instruction_template,
                     user_query_template, feedback_template):
    """
    Read a scientific paper as PDF file from `paper_file_path` and split it into chunks.
    Embed the chunks and store them in a vector store. Do the following for the given
    property and property description:
    - retrieve the top k chunks from the vector store that match the template property and its description. 
    - Send a prompt to the LLM with the following content:
        * Instruction
        * Template property
        * Top k chunks from paper that are related to the given property
    - Extract the assigned property value from the prompt answer, given that the property-value pair
    is enclosed in an XML tag.
    
    Return the updated dataframe row.
    
    """

    # Keywords:
    # - Retrieval Augmented Generation (RAG) over unstructured documents
    # - Question Answering
    # - Information Extraction
    # - Naive RAG (advanced RAG with pre- and post-retrieval porcessing and modular RAG also possible)
    # - Semantic Search
    # - Domain-specific Entity Recognition

    # Based on articles:
    # - https://learnopencv.com/rag-with-llms/
    # - https://python.langchain.com/docs/use_cases/question_answering/quickstart/
    # - https://huggingface.co/meta-llama/Meta-Llama-3-8B

    paperORKGID = row.name[0].split("/")[-1]
    contributionORKGID  = row.name[1].split("/")[-1]
    propertyORKGID = row.name[2].split("/")[-1]

    row_index = row["rowIndex"]
    property_name = row["propertyName"]
    property_values = row["propertyValues"]
    property_description = row["propertyDescription"]
    contribution_label = row["contributionLabel"]
    paper_file_path = row["pdfContainerPath"]

    logging.info(f"{row_index}/{total_rows}: Processing row: Paper ORKG ID: '{paperORKGID}' with Contribution ORKG ID: {contributionORKGID} and property ORKG ID: {propertyORKGID}.")

    # TextSplitter: Load the paper PDF files and split them according 
    # to the separators defined in the TextSplitter 
    logging.info(f"Load the paper PDF file and split it according to the separators defined in the TextSplitter.")
    parsed_pdf_file_name = paper_file_path.split("/")[-1][0:-3] + "txt"
    logging.info(f"Path to the parsed PDF file: {parsed_pdf_file_name}")

    pdf_parser = os.environ["PDF_PARSER"]
    with open(f"{parsed_pdf_dir}/{pdf_parser}/{parsed_pdf_file_name}", "r") as parsed_pdf_file:
        page_content = parsed_pdf_file.read()
    data = [Document(metadata={'source': paper_file_path}, page_content=page_content)]

    text_splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=100,
                                                   separators=["\n\n",".","\n"," "])
    all_splits = text_splitter.split_documents(data)

    # Vectorstore: Create and persist the vectors locally on disk or load them if they 
    # exist already
    logging.info("Save split embeddings into an in-memory vectorstore.")
    try:
        vectorstore = FAISS.from_documents(all_splits, embeddings)
    except RuntimeError as e:
        logging.error(e)
        row["errorType"] = "DeviceSideAssertionError_ChunkEmbeddings"
        row["propertyValuePrediction"] = ""
        row["ynCrowdsourcedValuesInSnippets"] = "unknown"
        row["ynCrowdsourcedValuesInText"] = "unknown"
        row["ynExactMatchSnippets"] = "unknown"
        row["ynExactMatchFullText"] = "unknown"
        row["ynAnswerExtractionSuccessful"] = "no"  

        return row

    # Preprocessing properties and contributions
    propertyProcessed = property_name.replace(" ", "_").replace("/", "_or_")
    
    pattern = "^(Contribution)?(\s*\d+$)?(.*)"
    regex = re.compile(pattern)
    contr = re.match(regex, contribution_label).group(3)

    # Retrieving splits
    logging.info(f"Retrieve splits/documents from vectorstore for the property-description pair:{propertyProcessed}:{property_description}.")
    question_add=f"In the context of the scientific contribution '{contr}', " if contr else ""
    question_to_retriever = f"{question_add}What matches the following property-description pair: {propertyProcessed}:{property_description}"
    retriever = vectorstore.as_retriever(search_kwargs={"k":10})
    docs = retriever.invoke(question_to_retriever)
    snippets = "\nSnippet:" + "\n\nSnippet:".join(doc.page_content for doc in docs)

    # Retrieve snippets
    logging.info(f"Set template for prompt and question based on the given property.")

    # Check if the crowdsourced property value is contained in the snippets and in the text
    logging.info("Check if the crowdsourced property value is contained in the snippets and in the text")
    property_values_list = property_values.split("|")
    snippets_lower = snippets.lower()
    full_text_lower = data[0].page_content.lower()
    row["ynCrowdsourcedValuesInSnippets"] = "yes" if all(value.lower() in snippets_lower for value in property_values_list) else "no"
    row["ynCrowdsourcedValuesInText"] = "yes" if all(value.lower() in full_text_lower for value in property_values_list) else "no"

    # Prepare prompt: Set template and user query
    instruction = instruction_template.format(snippets=snippets)
    user_query = user_query_template.format(property_name=property_name, 
                                            property_description=property_description, 
                                            contribution=contr)
    
    if llm_short == "Mistral-7B-Instruct-v0.3":
        messages = [
            {"role": "user", "content": instruction + "\n\n" + user_query},
        ]
    else:
        messages = [
            {"role": "system", "content": instruction},
            {"role": "user", "content": user_query},
        ]

    retry_limit = 3  # Maximum number of retries
    retry_count = 0
    success = False
    matches = set()

    def log_error_and_save_prompt(error: str):
        row["errorType"] = error
        row["propertyValuePrediction"] = ""
        row["ynExactMatchSnippets"] = "unknown"
        row["ynExactMatchFullText"] = "unknown"
        row["ynAnswerExtractionSuccessful"] = "no"  

        # Save prompts
        prompt = tokenizer.apply_chat_template(messages,tokenize=False)
        with open(f"{prompts_dir}/{pdf_parser}/{llm_short}/{shots}/prompt_{row_index}.txt", "w") as prompt_file:
            prompt_file.write(f"{prompt} \n\n!!{error}!!")

    while retry_count < retry_limit and not success:
        # Encode messages
        input_ids = tokenizer.apply_chat_template(
            messages,
            add_generation_prompt=True,
            return_tensors="pt"
        ).to(llm.device)
            
        # Send prompt including top k text splits (context) and question to LLM 
        prompt_length = input_ids.shape[-1]
        logging.info(f"Send prompt with length {prompt_length} including top k text splits (context) and question to LLM. \
        The eos_token_id is: {tokenizer.eos_token_id}. the pad_token_id is: {tokenizer.pad_token_id}")
        try:
            outputs = llm.generate(
                input_ids,
                max_new_tokens=512,
                eos_token_id=tokenizer.eos_token_id,
                pad_token_id=tokenizer.pad_token_id, 
                # The settings below help the model generate text that is more deterministic 
                # and less exploratory, 
                # which is beneficial for tasks requiring high precision.
                do_sample=True, 
                temperature=0.1, # the highest probable next token is always picked
                top_p=0.2, # considers only tokens that cumulatively add up to 20% of the probability
            )
            response = outputs[0][input_ids.shape[-1]:]
            decoded_response = tokenizer.decode(response, skip_special_tokens=True)
            messages.append({"role": "assistant", "content": decoded_response})
        except RuntimeError as e:
            logging.error(e)
            log_error_and_save_prompt("DeviceSideAssertionError_Generation")

            return row

        # Parse answer
        logging.info("Parse generated XML tree from the model's answer.")
        pattern = r'<PropertyValueAssignments>.*?</PropertyValueAssignments>'
        xml_answer = re.search(pattern, decoded_response, re.DOTALL)

        if not xml_answer:
            logging.info(f"Could not extract XML data from prompt response.")
            log_error_and_save_prompt("WrongAnswerFormat")
            return row

        try:
            # Parse XML string using ElementTree
            root = ET.fromstring(xml_answer.group(0))
            property_values_extracted = root[1].text
            row['ynAnswerExtractionSuccessful'] = 'yes'  
        except ET.ParseError as e:
            logging.error(f"XML parsing error: {e}")
            row["errorType"] = "XMLParsing"
            log_error_and_save_prompt("XMLParsing")
            return row

        # Verify that propertyValuePrediction is in the snippets and the full text
        logging.info("Verification that generated property values are in the text snippets.")
        unknown_answer_pattern="(n\/a|na|no exact match|none|null|no result|not( explicitly)?)?( found| provided| specified| available| applicable| mentioned)?( in the( given| provided)?( context| snippets))?(\.)?"
        if not property_values_extracted or re.fullmatch(unknown_answer_pattern, property_values_extracted.lower()):
            row["errorType"] = "NoValuesGenerated"
            log_error_and_save_prompt("NoValuesGenerated")
            return row
        
        # Check if generated property value is contained in the snippets
        property_values_extracted_list_raw = property_values_extracted.split("|")
        property_values_extracted_list = [v.strip() for v in property_values_extracted_list_raw]
        missmatches = []

        for value in property_values_extracted_list:
            if value.lower() not in snippets_lower:
                missmatches.append(value)
            else:
                matches.add(value)

        if missmatches:
            logging.info("Property values do not match the values in the snippets. Give feedback to the model and retry.")
            retry_count += 1
            feedback = feedback_template.format(property_values="|".join(missmatches),
                                                missmatches_cnt=len(missmatches))
            messages.append({"role": "user", "content": feedback})
            continue  
        else:
            success = True

    # Assign the generated property values to the dataframe
    logging.info(f"Generated property-value pair: {property_name}: {property_values_extracted}")
    row['propertyValuePrediction'] = property_values_extracted

    # Check if extracted property values are contained in the full-text
    row["ynExactMatchFullText"] = "yes" if all(value.lower() in full_text_lower for value in property_values_extracted_list) else "no" 

    row["retries"] = retry_count
    
    if success:
        row["ynExactMatchSnippets"] = "yes" 
        logging.info(f"Sucessfully generate property values that are found in the snippets within retry limit of {retry_limit}.")
    else:
        row["ynExactMatchSnippets"] = "no"
        logging.info(f"Failed to generate property values that are found in the snippets within retry limit of {retry_limit}.")

    
    # Save prompts
    prompt = tokenizer.apply_chat_template(messages,tokenize=False)
    with open(f"{prompts_dir}/{pdf_parser}/{llm_short}/{shots}/prompt_{row_index}.txt", "w") as prompt_file:
        prompt_file.write(prompt)

    return row


##########################################################################################
# Pipeline
##########################################################################################
# Problems to deal with

def main():
    # Dataset name: Should be on your disk
    dataset_file_name="ontopop.csv"

    # Assertions
    logging.info("Running assertions.")
    assert os.path.exists(f"{datasets_dir}/{dataset_file_name}")
    assert os.environ["HF_TOKEN"] != ""
    assert os.environ["SHOTS"] in ["zero_shot", "one_shot", "few_shot"]
    assert os.environ["PDF_PARSER"] in ["tika", "pypdfloader"]
    assert os.environ["LLM"] in ["meta-llama/Meta-Llama-3-8B-Instruct",
                                 "tiiuae/Falcon3-10B-Instruct", 
                                 "mistralai/Mistral-7B-Instruct-v0.3"]

    assert torch.__version__ == "2.5.0+cu124"
    assert torch.cuda.is_available() == True, "CUDA is not available on your system."

    # Read dataset 
    logging.info(f"Reading dataset {datasets_dir}/{dataset_file_name}")
    ontopop_df = pd.read_csv(f"{datasets_dir}/{dataset_file_name}", escapechar='\\')
    logging.info(f"Dataframe dimensions: {ontopop_df.shape}")
    logging.info(f'Number of papers: {ontopop_df["paper"].nunique()}; Number of Properties: {ontopop_df["property"].nunique()}')

    # Set the index and datatypes
    ontopop_df.set_index(["paper", "contributionInstance", "property"], inplace=True)

    # Define new columns for predict_property task
    # Terms in comments are the terms used in the paper
    ontopop_df["rowIndex"] = range(1, len(ontopop_df) + 1)
    ontopop_df["propertyValuePrediction"] = None # Property Value Generation 
    ontopop_df["ynCrowdsourcedValuesInSnippets"] = None # Yn CPVs in snippets
    ontopop_df["ynCrowdsourcedValuesInText"] = None # Yn CPVs in Text
    ontopop_df["ynAnswerExtractionSuccessful"] = None
    ontopop_df['ynExactMatchSnippets'] = None # Yn Exact Match Snippets
    ontopop_df["ynExactMatchFullText"] = None 
    ontopop_df["retries"] = None # Retries
    ontopop_df["errorType"] = None # Error type

    # Set datatypes
    ontopop_df["propertyValues"] = ontopop_df["propertyValues"].astype("str")
    ontopop_df["propertyValuePrediction"] = ontopop_df["propertyValuePrediction"].astype("string")

    # Set Sentence Embeddings
    logging.info("Load HuggingFaceEmbeddings (Sentence Embeddings) with the specified parameters.")
    embeddings = HuggingFaceEmbeddings(
        model_name="sentence-transformers/all-MiniLM-L6-v2", 
        model_kwargs={'device':'cuda'}, 
        encode_kwargs={'normalize_embeddings': False}
    )

    # Set LLM   
    model_id = llm_id
    logging.info(f"Load LLM: {model_id}.")
    llm = AutoModelForCausalLM.from_pretrained(model_id, token=os.environ["HF_TOKEN"], torch_dtype=torch.bfloat16, device_map="auto")

    # Set Tokenizer
    logging.info(f"Load Tokenizer: {model_id}.")
    tokenizer = AutoTokenizer.from_pretrained(model_id, token=os.environ["HF_TOKEN"])

    # Add padding token if the model has none to avoid warnings during llm.generate
    if tokenizer.pad_token_id is None:
        tokenizer.add_special_tokens({"pad_token": "<|pad|>"})
        llm.resize_token_embeddings(len(tokenizer))
        tokenizer.pad_token_id = tokenizer.convert_tokens_to_ids('<|pad|>')

    # Load feebback template
    with open(f"{instructions_dir}/feedback_template.txt", "r") as feedback_template_file:
        feedback_template = feedback_template_file.read()
        logging.info("Feedback templates loaded")

    # Load instruction template
    with open(f'{instructions_dir}/instruction_template.txt', "r") as instruction_template_file:
        instruction_template = instruction_template_file.read()

    # Load user query template
    with open(f"{instructions_dir}/user_query_template_{shots}.txt", "r") as user_query_template_file:
        user_query_template = user_query_template_file.read()
        
    logging.info(f"Predicting property values")
    total_rows = ontopop_df.shape[0]
    
    ontopop_df = ontopop_df.apply(predict_property, args=(embeddings, llm, tokenizer, total_rows,
                                                         instruction_template, user_query_template,
                                                         feedback_template), axis=1)
    ontopop_df.to_csv(f"{generation_dir}/ontopop_predicted_{pdf_parser}_{llm_short}_{shots}.csv", escapechar='\\')
    
    # Finish
    logging.info("Finished processing.")

    # Assertions
    # Assert that that there are as many paper IDs and property IDs in the log files as in the ontopop_statistics_10.json file
    # grep -o "Paper ORKG ID: 'R[0-9]*'" ontopop_predicted_tika_Falcon3-10B-Instruct_zero_shot.txt | awk -F"'" '{print $2}' | sort | uniq | wc -l
    # grep -o "Paper ORKG ID: 'R[0-9]*'" ontopop_predicted_tika_Meta-Llama-3-8B-Instruct_zero_shot.txt | awk -F"'" '{print $2}' | sort | uniq | wc -l
    # grep -o "Paper ORKG ID: 'R[0-9]*'" ontopop_predicted_tika_Mistral-7B-Instruct-v0.3_zero_shot.txt | awk -F"'" '{print $2}' | sort | uniq | wc -l
    # Assertion passed on 24.01.2025 11:25

    # grep -o "property ORKG ID: .*" ontopop_predicted_tika_Falcon3-10B-Instruct_zero_shot.txt | awk -F": " '{print $2}' | sort | uniq | wc -l
    # grep -o "property ORKG ID: .*" ontopop_predicted_tika_Meta-Llama-3-8B-Instruct_zero_shot.txt | awk -F": " '{print $2}' | sort | uniq | wc -l
    # grep -o "property ORKG ID: .*" ontopop_predicted_tika_Mistral-7B-Instruct-v0.3_zero_shot.txt | awk -F": " '{print $2}' | sort | uniq | wc -l
    # Assertion passed on 24.01.2025 11:25

if __name__ == "__main__":
    main()
    
