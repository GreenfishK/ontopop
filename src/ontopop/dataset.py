##########################################################################################
# Paths, Endpoints, Tokens and Environment Variables
##########################################################################################
# Local file and directory paths: Set them depending on your server!
queries = "/home/ontopop/src/ontopop/sparql_queries/orkg"

log_file_path = "/home/ontopop/logs/create_dataset/create_dataset.txt"
pdf_dir = "/home/ontopop/data/create_dataset/papers"
parsed_pdf_dir = "/home/ontopop/data/create_dataset/parsed_papers"
orkg_dumps_dir = "/home/ontopop/data/create_dataset/orkg_dumps"
datasets_dir = "/home/ontopop/data/create_dataset"
databases_dir="/home/ontopop/data/databases"

##########################################################################################
# Imports, diretories setup and tokens
##########################################################################################
from utils.rdf import get_result_set
import logging
import requests
import os
from datetime import datetime
import pandas as pd
import re
from concurrent.futures import ThreadPoolExecutor, as_completed
from tika import parser as tika_parser
from langchain_core.documents.base import Document
from langchain_community.document_loaders import PyPDFLoader
import json

############################################################################################
# Logging configuration
############################################################################################ 
with open(log_file_path, "w") as log_file:
    log_file.write("")

logging.basicConfig(
    handlers=[logging.FileHandler(filename=log_file_path, encoding='utf-8', mode='a+')],
    format="%(asctime)s %(filename)s:%(levelname)s:%(message)s",
    datefmt="%F %A %T",
    level=logging.INFO
    )

##########################################################################################
# Classes
##########################################################################################
class TikaParserSingleton:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            logging.info("Initializing Tika server")
            cls._instance = super(TikaParserSingleton, cls).__new__(cls)
        return cls._instance

    def parse(self, pdf_file_path: str) -> list[Document]:
        parsed_data = tika_parser.from_file(pdf_file_path)
        page_content = parsed_data.get("content", "")
        cleaned_content = clean_parsed_text(page_content)
        
        return [Document(metadata={'source': pdf_file_path}, page_content=cleaned_content)]


##########################################################################################
# Functions
##########################################################################################
def clean_parsed_text(text: str) -> str:
    # Remove hyphenated line breaks (e.g., "improve-\nments" -> "improvements")
    text = re.sub(r"(\w+)-\n(\w+)", r"\1\2", text)
    # Remove line breaks within URLs (e.g., "https://www.github.com/\namazon" -> "https://www.github.com/amazon")
    text = re.sub(r"(https?://\S*)\n(\S+)", r"\1\2", text)

    return text.strip()


def parse_pdf(parser_model:str, pdf_file_path:str) -> list[Document]:
    if parser_model == "tika":
        tika_parser_instance = TikaParserSingleton()
        data = tika_parser_instance.parse(pdf_file_path)

    elif parser_model == "pypdfloader":
        print(pdf_file_path)
        loader = PyPDFLoader(pdf_file_path)   
        documents = loader.load()
        content = "\n".join([document.page_content for document in documents])
        data = [Document(metadata={'source': pdf_file_path}, page_content=content)]

    else:
        raise Exception("The parser must be one of: tika, pypdfloader")

    return data


def parse_pdf_files(group, parser_model, pdfs_not_found):
    pdf_file_path = group.iloc[0]["pdfContainerPath"]
    parsed_pdf_file_name = pdf_file_path.split("/")[-1][0:-3] + "txt"

    try:
        parsed_pdf = parse_pdf(parser_model, pdf_file_path)
        parsed_pdf_content = parsed_pdf[0].page_content
        with open(f"{parsed_pdf_dir}/{parser_model}/{parsed_pdf_file_name}", "w") as parsed_pdf_file:
            parsed_pdf_file.writelines(parsed_pdf_content)
    except FileNotFoundError as e:
        logging.error(e)
        logging.info(f"Parsing the following path failed: {pdf_file_path}")
        pdfs_not_found.append(pdf_file_path)

    return group

def log_statistics(ontopop_dump_df: pd.DataFrame, statistics_ds_file_name):
    # Create dataframe for statistics
    data = {'cnt_papers': None,
            'cnt_duplicate_papers': None,
            'cnt_url_provided': None,
            'cnt_properties': None,
            'cnt_property_descriptions': None,
            'cnt_pdf_downloads': None}
                
    ontopop_statistics_df = pd.Series(data=data, index=data.keys())

    ontopop_statistics_df["cnt_papers"] = ontopop_dump_df["paper"].nunique()
    cnt_duplicate_papers_init_df = ontopop_dump_df.copy()
    cnt_duplicate_papers_init_df['numReferences'] = ontopop_dump_df.groupby('paperTitle')['paper'].transform('nunique')
    ontopop_statistics_df["cnt_duplicate_papers"] = cnt_duplicate_papers_init_df[cnt_duplicate_papers_init_df['numReferences'] > 1]["paperTitle"].nunique()
    ontopop_statistics_df["cnt_url_provided"] = ontopop_dump_df[["paper", "paperUrl"]].groupby(["paper"]).count().gt(0).sum().iloc[0]
    ontopop_statistics_df["cnt_properties"] = ontopop_dump_df["property"].nunique()
    ontopop_statistics_df["cnt_property_descriptions"] = ontopop_dump_df[["property", "propertyDescription"]].groupby(["property"]).count().gt(0).sum().iloc[0]
    
    cnt_pdf_downloads = ontopop_dump_df[["paper", "pdfDownloadTs"]].groupby(["paper"]).count().gt(0).sum().iloc[0]
    ontopop_statistics_df["cnt_pdf_downloads"] = max(0, cnt_pdf_downloads - ontopop_statistics_df["cnt_duplicate_papers"])

    logging.info(f"Saving statistics dataset to {datasets_dir}/{statistics_ds_file_name}")
    ontopop_statistics_df.to_json(f"{datasets_dir}/{statistics_ds_file_name}")


def generate_latex_table(json_files, output_file):
    # Load data from JSON files
    data = {}
    for file in json_files:
        with open(file, 'r') as f:
            data[file] = json.load(f)

    # Sort the keys for proper ordering
    sorted_keys = sorted(data.keys(), key=lambda x: (x.endswith("init.json"), int(x.split("_")[-1].split(".")[0]) if x.split("_")[-1].split(".")[0].isdigit() else -1))

    # Calculate deltas
    deltas = {}
    prev_key = None
    for key in sorted_keys:
        if prev_key is None:
            prev_key = key
            continue

        deltas[key] = {k: data[key][k] - data[prev_key][k] for k in data[key].keys()}
        prev_key = key

    # Summarize M3-M6
    m3_m6_keys = [key for key in sorted_keys if "3" <= key.split("_")[-1].split(".")[0] <= "6"]
    print(m3_m6_keys)
    m3_m6_summary = {k: sum(deltas[key][k] for key in m3_m6_keys) for k in data[m3_m6_keys[0]].keys()}

    # Calculate processed ORKG dump as initial minus all deltas
    processed_orkg_dump = data[sorted_keys[0]].copy()  # Start with the initial JSON
    print(processed_orkg_dump)

    for key in sorted_keys[1:]:
        print(f"key: {key}")
        for k in processed_orkg_dump.keys():
            processed_orkg_dump[k] += deltas[key].get(k, 0)
            print(f"processed_orkg_dump at {str(k)}: {processed_orkg_dump[k]}")
    
    print(processed_orkg_dump)

    # Prepare LaTeX table rows
    rows = []
    metric_names = {
        "cnt_papers": "\\# Paper IRIs",
        "cnt_duplicate_papers": "\\# Duplicate papers",
        "cnt_url_provided": "\\# URLs",
        "cnt_properties": "\\# Properties",
        "cnt_property_descriptions": "\\# Property descriptions",
        "cnt_pdf_downloads": "\\# PDFs downloaded",
    }

    def format_number(number):
        return f"{number:,}".replace(",", ".")
    
    for metric, name in metric_names.items():
        row = [
            name,
            format_number(data[sorted_keys[0]][metric]),  # Raw ORKG dump
            format_number(deltas[sorted_keys[1]].get(metric, 0)),  # F1
            format_number(deltas[sorted_keys[2]].get(metric, 0)),  # F2
            format_number(m3_m6_summary.get(metric, 0)),  # M3-M6
            format_number(deltas[sorted_keys[7]].get(metric, 0)),  # F7
            format_number(deltas[sorted_keys[8]].get(metric, 0)),  # F8
            format_number(deltas[sorted_keys[9]].get(metric, 0)),  # F9
            format_number(deltas[sorted_keys[10]].get(metric, 0)),  # F10
            format_number(processed_orkg_dump[metric]),  # Processed ORKG dump
        ]
        rows.append(row)

    # Generate LaTeX table
    table_header = r"""
    \begin{table}[H]
    \caption{Dataset statistics for the \textit{Ontopop} dataset. The figures for the "RAW ORKG dump" and "Processed ORKG dump" columns are absolute numbers, while those for the preprocessing steps are deltas.}\label{ontopop:preprocessing}

    \resizebox{1\textwidth}{!}{%
    \begin{tabular}{c|>{\centering\arraybackslash}p{0.08\textwidth} c c c c c c c >{\centering\arraybackslash}p{0.12\textwidth}}
        Dataset statistics           & Raw ORKG dump & F1 & F2 & M3--M6 & F7 & F8 & F9  & F10 & Processed ORKG dump \\
        \hline
    """
    table_footer = r"""
        \end{tabular}%
        }
    \end{table}
    """

    table_rows = "\n".join(
        "    " + " & ".join(map(str, row)) + r" \\"
        for row in rows
    )

    latex_table = table_header + table_rows + table_footer

    # Save to output file
    with open(output_file, 'w') as f:
        f.write(latex_table)

    logging.info(f"LaTeX table saved to {output_file}")


##########################################################################################
##########################################################################################
# Pipeline
##########################################################################################
##########################################################################################
# Assertions
assert os.environ["ORKG_ENDPOINT"] != ""
assert os.environ["NUM_WORKERS"] != ""
assert os.environ["DOWNLOAD_PAPERS"] != ""
assert os.environ["EXECUTE_QUERY"] != ""

##########################################################################################
# Dataset 1: ORKG crowdsourcing and their full-text papers
##########################################################################################
# Creating the dataset: Obtaining user-annotations and their full-text papers 
orkg_endpoint = os.environ["ORKG_ENDPOINT"]
logging.info(f"Query the ORKG ({orkg_endpoint}) and save paper metadata and crowdsourced annotations.")

if int(os.environ["EXECUTE_QUERY"]) == 1:
    ontopop_dump_df = get_result_set(f"{queries}/ontopop_dump.sparql", os.environ["ORKG_ENDPOINT"], orkg_dumps_dir, True)
else:
    ontopop_dump_df = get_result_set(f"{queries}/ontopop_dump.sparql", os.environ["ORKG_ENDPOINT"], orkg_dumps_dir, False)
logging.info(f"Dataframe dimensions: {ontopop_dump_df.shape}")

# Add new columns
ontopop_dump_df['pdfUrlAccessible'] = None
ontopop_dump_df['pdfDownloadTs'] = None

# Add statistics
log_statistics(ontopop_dump_df, "ontopop_statistics_0.json")

# Filter out records that have no paper title 
logging.info("Remove records with no paper title.")
ontopop_dump_df = ontopop_dump_df[~ontopop_dump_df["paperTitle"].isna()]
logging.info(f"Dataframe dimensions: {ontopop_dump_df.shape}")
log_statistics(ontopop_dump_df, "ontopop_statistics_1.json")

# Filter out records that have no paper url 
logging.info("Remove records with no paper url.")
ontopop_dump_df = ontopop_dump_df[~ontopop_dump_df["paperUrl"].isna()]
logging.info(f"Dataframe dimensions: {ontopop_dump_df.shape}")
log_statistics(ontopop_dump_df, "ontopop_statistics_2.json")

# Remove non-printbale characters
def remove_non_printable(s):
    if isinstance(s, str):  
        return re.sub(r'[^\x20-\x7E]', '', s)  
    return s 

logging.info("Remove non-printable characters.")
ontopop_dump_df["contributionLabel"] = ontopop_dump_df["contributionLabel"].apply(remove_non_printable)
log_statistics(ontopop_dump_df, "ontopop_statistics_3.json")

# Remove line break characters
def remove_line_breaks(s):
    if isinstance(s, str): 
        return re.sub(r'[\r\n]+', '', s)  
    return s 

logging.info("Remove line break characters.")
ontopop_dump_df["paperTitle"] = ontopop_dump_df["paperTitle"].apply(remove_line_breaks)
log_statistics(ontopop_dump_df, "ontopop_statistics_4.json")

# Remove excessive space characters
def remove_space_characters(s):
    if isinstance(s, str): 
        return re.sub(r'\s{2,}', '', s)  
    return s 

logging.info("Remove excessive space characters from paper titles.")
ontopop_dump_df["paperTitle"] = ontopop_dump_df["paperTitle"].apply(remove_space_characters)
log_statistics(ontopop_dump_df, "ontopop_statistics_5.json")

# Remove surrounding quotes in paper title
def remove_quotes(s):
    if isinstance(s, str) and s.startswith('"') and s.endswith('"'):
        return s[1:-1]
    return s

logging.info("Remove surrounding quotes in paper title")
ontopop_dump_df["paperTitle"] = ontopop_dump_df["paperTitle"].apply(remove_quotes)
log_statistics(ontopop_dump_df, "ontopop_statistics_6.json")

# Filter out records that have no property description
logging.info("Remove records with no property description.")
ontopop_dump_df.dropna(subset=["propertyDescription"], inplace=True)
logging.info(f"Dataframe dimensions: {ontopop_dump_df.shape}")
log_statistics(ontopop_dump_df, "ontopop_statistics_7.json")

# Filter out records that have no property values
logging.info("Remove records with no property values.")
ontopop_dump_df.dropna(subset=["propertyValues"], inplace=True)
logging.info(f"Dataframe dimensions: {ontopop_dump_df.shape}")
log_statistics(ontopop_dump_df, "ontopop_statistics_8.json")

# Filter out records with structured properties
logging.info("Remove records with structured properties.")
ontopop_dump_df['hasStructuredPropertyValue'] = ontopop_dump_df['hasStructuredPropertyValue'].map({'true': True, 'false': False})
ontopop_dump_df = ontopop_dump_df[ontopop_dump_df["hasStructuredPropertyValue"]==False]
logging.info(f"Dataframe dimensions: {ontopop_dump_df.shape}")
log_statistics(ontopop_dump_df, "ontopop_statistics_9.json")

def download_pdf(group):
    paperUrl = group['paperUrl'].iloc[0]
    paperTitle = group['paperTitle'].iloc[0]
    
    try:
        headers = {'Accept': 'application/pdf'}
        response = requests.get(paperUrl, headers=headers, timeout=60)
        
        if response.headers.get('Content-Type') != 'application/pdf':
            logging.info(f"The provided URL for '{paperTitle}' does not point to a PDF file.")
            group['pdfUrlAccessible'] = 'no_pdf_url'
            return group

        # TODO: implement other scraping methods, such as SERP-based scraping.
        
        if response.status_code == 200:
            with open(f"{pdf_dir}/{paperTitle}.pdf", "wb") as f:
                f.write(response.content)
            logging.info(f"Successfully downloaded PDF for '{paperTitle}'.")
            group['pdfUrlAccessible'] = 'yes'
            group['pdfDownloadTs'] = datetime.now()
        else:
            logging.info(f"Failed to download PDF for '{paperTitle}'. Status code: {response.status_code}")
            group['pdfUrlAccessible'] = 'no'
    except Exception as e:
        logging.info(f"Error downloading PDF for '{paperTitle}': {e}")
        group['pdfUrlAccessible'] = 'no'

    return group


# Download papers
def download_pdfs_in_parallel(df, num_workers):
    groups = list(df.groupby('paper')) 
    
    logging.info(f'Downloading papers with {os.environ["NUM_WORKERS"]} workers.')
    with ThreadPoolExecutor(max_workers=num_workers) as executor:
        futures = {executor.submit(download_pdf, group): group_key for group_key, group in groups}
        total_futures = len(futures)

    logging.info("Updating dataframe ...")
    results = [] 
    for index, future in enumerate(as_completed(futures)):
        try:
            result_group = future.result() 
            results.append(result_group)  
        except Exception as e:
            group_key = futures[future]
            logging.info(f"An error occurred for group '{group_key}': {e}")
        
    updated_df = pd.concat(results, ignore_index=True)

    return updated_df

def add_pdf_infos(row): 
    paperTitle = row["paperTitle"]
    pdf_path = f"{pdf_dir}/{paperTitle}.pdf"
    
    if os.path.exists(pdf_path):
        row["pdfUrlAccessible"] = "yes"
        row['pdfDownloadTs'] = datetime.fromtimestamp(os.path.getctime(pdf_path))
    else:
        row["pdfUrlAccessible"] = "no"
        row['pdfDownloadTs'] = None 
    
    return row

if int(os.environ["DOWNLOAD_PAPERS"]) == 1:
    logging.info("Download papers as PDFs based on their URLs and record the accessibility and download timestamp of the paper in the dataframe.")
    ontopop_dump_df = download_pdfs_in_parallel(ontopop_dump_df, num_workers=int(os.environ["NUM_WORKERS"]))
else:
    logging.info("Assigning PDF accessibility and download timestamp to the dataframe.")
    ontopop_dump_df = ontopop_dump_df.apply(add_pdf_infos, axis=1)
logging.info(f"Dataframe dimensions: {ontopop_dump_df.shape}")
logging.info(f'Number of papers at this step: {ontopop_dump_df["paper"].nunique()}')

# Add statistics
cnt_pdf_downloads = ontopop_dump_df[["paper", "pdfDownloadTs"]].groupby(["paper"]).count().gt(0).sum().iloc[0]

# Remove records for which the download was unsuccessful
logging.info("Remove records for which the download was unsuccessful.")
ontopop_dump_df = ontopop_dump_df[pd.notna(ontopop_dump_df["pdfDownloadTs"])]
logging.info(f"Dataframe dimensions: {ontopop_dump_df.shape}")
log_statistics(ontopop_dump_df, "ontopop_statistics_10.json")

# Add an integer column with the number of references for each paper title
logging.info("Add an integer column with the number of references for each paper title.")
ontopop_dump_df['numReferences'] = ontopop_dump_df.groupby('paperTitle')['paper'].transform('nunique')
logging.info(f"Dataframe dimensions: {ontopop_dump_df.shape}")

# Add a boolean column indicating whether a paper title has more than one reference
logging.info("Add a boolean column with the information whether a 'paperTitle' has more than one reference.")
ontopop_dump_df['hasMultipleReferences'] = ontopop_dump_df['numReferences'] > 1
logging.info(f"Dataframe dimensions: {ontopop_dump_df.shape}")

# Add an integer column with the number of rows that each paper has
logging.info("Add an integer column with the number of rows that each 'paper' has.")
ontopop_dump_df['numRowsPerPaper'] = ontopop_dump_df.groupby('paper')['paper'].transform('count')
logging.info(f"Dataframe dimensions: {ontopop_dump_df.shape}")

# Add container path
logging.info("Add a string column for container path.")
ontopop_dump_df['pdfContainerPath'] = pdf_dir + "/" + ontopop_dump_df['paperTitle'] + ".pdf"
logging.info(f"Dataframe dimensions: {ontopop_dump_df.shape}")

# Save datasets
ontopop_ds_file_name = "ontopop.csv"

logging.info(f"Saving dataset to {datasets_dir}/{ontopop_ds_file_name}")
ontopop_dump_df.to_csv(f"{datasets_dir}/{ontopop_ds_file_name}", escapechar='\\', index=False)

# Post processing
ontopop_dump_verify_df = pd.read_csv(f"{datasets_dir}/{ontopop_ds_file_name}", escapechar='\\')

# Parse PDF files
logging.info(f"Parse PDF files and save them to {parsed_pdf_dir}.")
pdfs_not_found = []
parser_model = "tika"
ontopop_dump_verify_df = ontopop_dump_verify_df.groupby(["paper"]).apply(parse_pdf_files, parser_model, pdfs_not_found, include_groups=False)

if pdfs_not_found:
    logging.info("The following PDFs were not found: " + ",\n".join(pdfs_not_found) + "\nThis is likely because the PDF file name contains special characters. Please make sure to remove them with a function that targets those special characters.")
else:
    logging.info("Parsing of all PDF files was successful")

# TODO: see what todo with paper's that have two ORKG entries, i.e. two paper IRIs.

logging.info("Finish parsing PDFs.")

# Assertions
pdfs = [pdf for pdf in os.listdir(f"{pdf_dir}")]
paper_titles = ontopop_dump_df["paperTitle"].unique().tolist()
if len(pdfs) == len(paper_titles):
    logging.info(f"The number of saved PDFs ({len(pdfs)}) matches the number of paper titles ({len(paper_titles)}) in the dataframe.")
    logging.info(f'The number of paper ORKG IDs is: {ontopop_dump_df["paper"].nunique()}')
else:
    adds = list(set(pdfs) - set(paper_titles))
    logging.error(f"The following papers are saved on disk that are not found in the dataframe: ")
    logging.error(adds)

    subs = list(set(pdfs) - set(paper_titles))
    logging.error(f"The following papers are in the dataframe that are not saved on disk:")
    logging.error(subs)


# Create latex table
json_files = [
    f"{datasets_dir}/ontopop_statistics_0.json",
    f"{datasets_dir}/ontopop_statistics_1.json",
    f"{datasets_dir}/ontopop_statistics_2.json",
    f"{datasets_dir}/ontopop_statistics_3.json",
    f"{datasets_dir}/ontopop_statistics_4.json",
    f"{datasets_dir}/ontopop_statistics_5.json",
    f"{datasets_dir}/ontopop_statistics_6.json",
    f"{datasets_dir}/ontopop_statistics_7.json",
    f"{datasets_dir}/ontopop_statistics_8.json",
    f"{datasets_dir}/ontopop_statistics_9.json",
    f"{datasets_dir}/ontopop_statistics_10.json",
]

generate_latex_table(json_files, f"{datasets_dir}/ontopop_statistics_table.tex")


##########################################################################################
# Dataset 2: ORKG templates
##########################################################################################
logging.info("Query template data from the ORKG")

if int(os.environ["EXECUTE_QUERY"]) == 1:
    results_templates_usage=get_result_set(f"{queries}/template_usage.sparql", os.environ["ORKG_ENDPOINT"], orkg_dumps_dir, True)
    results_template_utilization=get_result_set(f"{queries}/template_utilization.sparql", os.environ["ORKG_ENDPOINT"], orkg_dumps_dir, True)
else:
    results_templates_usage=get_result_set(f"{queries}/template_usage.sparql", os.environ["ORKG_ENDPOINT"], orkg_dumps_dir, False)
    results_template_utilization=get_result_set(f"{queries}/template_utilization.sparql", os.environ["ORKG_ENDPOINT"], orkg_dumps_dir, False)

# Preprocess templates dataset
templates_df = results_templates_usage.merge(results_template_utilization, on="template", how="left")
na_cols = ['templateUtilizationRatio', 'cntTemplateProperty']
templates_df[na_cols] = templates_df[na_cols].fillna(0)

templates_df['cntTemplateInstances'] = templates_df['cntTemplateInstances'].astype(int)
templates_df["templateUtilizationRatio"] = templates_df["templateUtilizationRatio"].astype(float)
templates_df['cntTemplateProperty'] = templates_df['cntTemplateProperty'].astype(int)
templates_df.to_csv(f"{datasets_dir}/templates.csv", sep=",", index=False)
logging.info(f"Saved data to {datasets_dir}/templates.csv")

##########################################################################################
# Dataset 3: Contribution template instances
##########################################################################################
logging.info("Query contribution template utilization from the ORKG")
if int(os.environ["EXECUTE_QUERY"]) == 1:
    contributions_df = get_result_set(f"{queries}/contribution_template_util.sparql", os.environ["ORKG_ENDPOINT"], orkg_dumps_dir, True)
else:
    contributions_df = get_result_set(f"{queries}/contribution_template_util.sparql", os.environ["ORKG_ENDPOINT"], orkg_dumps_dir, False)
contributions_df.to_csv(f"{datasets_dir}/contribution_template_util.csv", sep=",", index=False)
logging.info(f"Saved data to {datasets_dir}/contribution_template_util.csv")

logging.info("Finish processing.")

##########################################################################################
# Additional queries
##########################################################################################
cnt_papers = get_result_set(f"{queries}/cnt_papers.sparql", os.environ["ORKG_ENDPOINT"], orkg_dumps_dir, True)

# Print number of papers
logging.info(f"Number of papers in the ORKG that have a title and a contribution: {cnt_papers['cnt_paper'].astype(int).sum()}")