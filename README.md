# Pipeline
Ontopop is a repository for experimenting with RAG-based scientific information extraction. Our current experiment includes the following steps:
* Download: Downloads a snapshot from the ORKG. 

* Correct: Cleans the raw ORKG dataset dump so that they can be ingested into GraphDB without errors. The validation is done with the RDF4J 3.7.4 validator.

* Ingest: Ingests the Dataset into GraphDB.

* Create_Dataset: 
    * Ontopop: Queries a subetset from the ORKG and applies a set preprocessing steps, resulting in the _Ontopop_ dataset for evaluation. Also downloads the paper PDFs via links from the Ontopop dataset
    * Templates: Queries a subset from the ORKG, processes it, and outputs the _Templates_ dataset.

* Generate: Uses the _Ontopop_ dataset to extract/generate property values for a given property. The context in the prompt includes the k most relevant snippets from the paper, the property description, and optionally the contribution label. 

* Evaluate: Computes the semantic similarity between the pair-wise sets of contributor property values and generated property values, by using the **cosine similarity** of the respective sentence embeddings, a **max** and an **average** aggregation function. Also, it computes the average count of tokens per generated property value.

* Visualize: Creates the following plots: 
    * Template usages grouped by the number of properties
    * Usage and utilization of the top 5% mostly used ORKG templates, with the Contribution template in more detail.
    * Average semantic similarities between the contributor property values and the generated property values.


# Reproducing the experiment
## Requirements
We use apptainer version 1.3.6-1.el9 for this experiment, which should be available in the target environment. It can be installed following [this](https://apptainer.org/docs/admin/main/installation.html) documentation. The space and hardware requirements are:
* 60G for the data, i.e. the location where $DATA in .env points to
* A computing unit that can run 64 threads in parallel. Otherwise, the parameter in the fourth line below can be tuned for a different number of threads.
* A GPU with CUDA cores. We use an Nvidia A100.

## Execution
Access the .env file in the root director of this project and update the variables `$HOME` and `$DATA` to accommodate your local environment.

Give read and write permissions to your `$DATA` directory.

Build the apptainer image by executing the following command:
```
./setup.sh "datalab"
```

Execute each of the steps in [Pipeline](#Pipeline) using a self-written orchestration script, similar to docker-compose:
```
apptainer-compose download "orkg"
apptainer-compose correct "orkg"
apptainer-compose ingest "orkg"
apptainer-compose create_dataset "ontopop" 64
apptainer-compose generate "tika" "meta-llama/Meta-Llama-3-8B-Instruct" "two_shot"
apptainer-compose generate "tika" "tiiuae/Falcon3-10B-Instruct" "two_shot"
apptainer-compose generate "tika" "mistralai/Mistral-7B-Instruct-v0.3" "two_shot"
apptainer-compose visualize "tika" "two_shot"
```

