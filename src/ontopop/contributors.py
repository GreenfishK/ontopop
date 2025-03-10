##########################################################################################
# Imports, diretories setup and tokens
##########################################################################################
import requests
from bs4 import BeautifulSoup
import csv
import logging

##########################################################################################
# Paths, Endpoints, Tokens and Environment Variables
##########################################################################################
log_file_path = "/home/ontopop/logs/download/contributors.txt"
download_dir = "/home/ontopop/data/download"


##########################################################################################
# Logging
##########################################################################################
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
def scrape_orkg_papers(start_page=0, end_page=10, output_file=f"{download_dir}/contributors.csv"):
    base_url = "https://orkg.org"
    papers = []
    
    for page in range(start_page, end_page + 1):
        url = f"{base_url}/papers?page={page}"
        response = requests.get(url)
        if response.status_code != 200:
            print(f"Failed to fetch page {page}")
            continue
        
        soup = BeautifulSoup(response.text, "html.parser")
        
        container = soup.find("div", class_="p-0 container")
        if not container:
            print(f"No container found on page {page}")
            continue
        
        paper_list = container.find("ul")
        if not paper_list:
            print(f"No paper list found on page {page}")
            continue
        
        for paper_div in paper_list.find_all("div", recursive=False):
            paper_section = paper_div.find_all("div", recursive=False)
            if len(paper_section) < 2:
                continue
            
            # Extract paper IRI
            paper_info_div = paper_section[0].find_all("div", recursive=False)
            if len(paper_info_div) < 3:
                continue
            
            mb2_div = paper_info_div[2].find("div", class_="mb-2")
            if not mb2_div:
                continue
            
            paper_link = mb2_div.find("a", href=True)
            if not paper_link:
                continue
            
            paper_iri = base_url + paper_link["href"]
            
            # Extract contributor IRI
            user_span = paper_section[1].find("span")
            if not user_span:
                continue
            
            user_link = user_span.find("a", href=True)
            if not user_link:
                continue
            
            contributor_iri = base_url + user_link["href"]
            
            papers.append([paper_iri, contributor_iri])
    
    with open(output_file, "w", newline="") as f:
        writer = csv.writer(f, delimiter=';')
        writer.writerow(["paper_IRI", "contributor"])
        writer.writerows(papers)
    
    print(f"Saved {len(papers)} records to {output_file}")

scrape_orkg_papers()
