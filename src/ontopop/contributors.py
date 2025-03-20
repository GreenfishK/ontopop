##########################################################################################
# Imports, diretories setup and tokens
##########################################################################################
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from webdriver_manager.chrome import ChromeDriverManager
from bs4 import BeautifulSoup
import csv
import time
import logging
import os
import datetime
import matplotlib.pyplot as plt
import pandas as pd
from collections import Counter

##########################################################################################
# Paths, Endpoints, Tokens and Environment Variables
##########################################################################################
log_file_path = "/home/ontopop/logs/download/contributors.log"
download_dir = "/home/ontopop/data/download"
ts = datetime.datetime.now().strftime("%Y-%m-%d")

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
def scrape_orkg_papers(output_file=f"{download_dir}/orkg_contributors_{ts}.csv"):
    base_url = "https://orkg.org"
    papers = []
    page = 0  # Start page

    # Install ChromeDriver
    options = webdriver.ChromeOptions()
    options.add_argument("--headless")  
    options.add_argument("--no-sandbox") 
    options.add_argument("--disable-dev-shm-usage")  # Overcome limited resource issues

    # Use webdriver-manager to handle ChromeDriver installation
    service = Service(ChromeDriverManager().install())
    driver = webdriver.Chrome(service=service, options=options)

    with open(output_file, "w", newline="") as f:
        writer = csv.writer(f, delimiter=';')
        writer.writerow(["paper_IRI", "contributor"])

    while True:
        url = f"{base_url}/papers?page={page}"
        driver.get(url)

        # Wait for the page to load (adjust timeout if needed)
        WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((By.CLASS_NAME, "p-0.container"))
        )
        time.sleep(3)  # Give time for dynamic content to load

        soup = BeautifulSoup(driver.page_source, "html.parser")
        
        container = soup.find("div", class_="p-0 container")
        if not container:
            logging.info(f"No container found on page {page}")
            break

        paper_list = container.find("ul")
        if not paper_list:
            logging.info(f"No paper list found on page {page}")
            break

        new_papers = False

        for paper_div in paper_list.find_all("div", recursive=False):
            paper_section = paper_div.find_all("div", recursive=False)
            if len(paper_section) < 2:
                continue

            # Extract paper IRI
            paper_info_div = paper_section[0].find_all("div", recursive=False)
            if len(paper_info_div) < 2:
                continue

            mb2_div = paper_info_div[1].find("div", class_="mb-2")
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

            # Save to CSV
            with open(output_file, "a", newline="") as f:
                writer = csv.writer(f, delimiter=';')
                writer.writerow([paper_iri, contributor_iri])
                new_papers = True

        if not new_papers:
            break  # Stop if no new papers are found on this page

        page += 1  # Move to the next page

    # Close Selenium WebDriver
    driver.quit()
    logging.info(f"Saved records to {output_file}")



##########################################################################################
# Pipeline
##########################################################################################
scrape_orkg_papers()