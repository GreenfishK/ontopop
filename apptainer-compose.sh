#!/bin/bash
#SBATCH --nodes=1                   # select number of nodes
#SBATCH --ntasks-per-node=1        # select number of tasks per node
#SBATCH --gres=gpu:a100:1	   # select gpu
#SBATCH --partition=GPU-a100        # select a partition i.e. "GPU-a100"
#SBATCH --time=2-0            # request 1 day of wall clock time
#SBATCH --mem=80GB                   # memory size required per node
####################################################################################
# Notes
####################################################################################
# This script bundles all preprocessing steps, such as download, correct and ingest
# into apptainer-based executions. It also loads environment variables that 
# are used in the apptainer parameters.
# Also, the trinity app, grobid, and graphdb can be run.

####################################################################################
# Functions
####################################################################################
# Load environment variables
. .env

function download () {
    dataset=$1

    # Create directories
    mkdir -p ${ONTOPOP_LOGS}/download
    mkdir -p ${ONTOPOP_DATA}/download

    chmod 700 src/ontopop/download_data.sh 
    apptainer exec \
    --fakeroot \
    --writable-tmpfs \
    --bind ${ONTOPOP_LOGS}/download:${c_logs}/download \
    --bind ${ONTOPOP_DATA}/download:${c_data}/download \
    --bind src/ontopop/download_data.sh:${c_home}/src/ontopop/download_data.sh \
    $APPTAINER_IMG ${c_home}/src/ontopop/download_data.sh $dataset
}

# Does not work yet
function download_contributors () {

    # Create directories
    mkdir -p ${ONTOPOP_LOGS}/download
    mkdir -p ${ONTOPOP_DATA}/download

    chmod 700 src/ontopop/contributors.py
    apptainer exec \
    --fakeroot \
    --writable-tmpfs \
    --bind ${ONTOPOP_LOGS}/download:${c_logs}/download \
    --bind ${ONTOPOP_DATA}/download:${c_data}/download \
    --bind src/ontopop/contributors.py:${c_home}/src/ontopop/contributors.py \
    $APPTAINER_IMG python3 -u ${c_home}/src/ontopop/contributors.py 2>&1
}

function correct () {
    dataset=$1

    # Create directories
    mkdir -p ${ONTOPOP_LOGS}/correct
    mkdir -p ${ONTOPOP_DATA}/download
    mkdir -p ${ONTOPOP_DATA}/correct

    chmod 700 src/ontopop/correct_data.sh 
    apptainer exec \
    --fakeroot \
    --writable-tmpfs \
    --bind ${ONTOPOP_LOGS}/correct:${c_logs}/correct \
    --bind ${ONTOPOP_DATA}/download:${c_data}/download \
    --bind ${ONTOPOP_DATA}/correct:${c_data}/correct \
    --bind src/ontopop/correct_data.sh:${c_home}/src/ontopop/correct_data.sh \
    $APPTAINER_IMG ${c_home}/src/ontopop/correct_data.sh $dataset
}

function ingest () {
    dataset=$1

    # Create directories
    mkdir -p ${ONTOPOP_LOGS}/ingest
    mkdir -p $GRAPHDB_HOME
    mkdir -p $GRAPHDB_TMP  

    apptainer exec \
    --fakeroot \
    --writable-tmpfs \
    --bind ${ONTOPOP_LOGS}/ingest:${c_logs}/ingest\
    --bind ${ONTOPOP_DATA}/correct:/home/ontopop/databases/graphdb/graphdb-import \
    --bind $GRAPHDB_HOME:/home/ontopop/databases/graphdb/home \
    --bind $GRAPHDB_TMP:/home/ontopop/databases/graphdb/tmp \
    --bind src/ontopop/configs/${dataset}-repo-config.ttl:/home/ontopop/databases/graphdb/${dataset}-repo-config.ttl \
    --env "GDB_JAVA_OPTS=-Xmx50g -Xms50g -Dgraphdb.home=/home/ontopop/databases/graphdb/home -Dgraphdb.workbench.importDirectory=/home/ontopop/databases/graphdb/graphdb-import -Dgraphdb.workbench.cors.enable=true -Denable-context-index=true -Dentity-pool-implementation=transactional -Dhealth.max.query.time.seconds=60 -Dgraphdb.append.request.id.headers=true -Dreuse.vars.in.subselects=true -Dgraphdb.logger.root.level=INFO" \
    $APPTAINER_IMG /home/ontopop/databases/graphdb/dist/bin/importrdf preload \
            --force \
            --recursive \
            --recovery-point-interval 3600 \
            --parsing-tasks 8 \
            -q /home/ontopop/databases/graphdb/tmp  \
            -c /home/ontopop/databases/graphdb/${dataset}-repo-config.ttl \
            /home/ontopop/databases/graphdb/graphdb-import/${dataset}.ttl 2>&1 | tee ${ONTOPOP_LOGS}/ingest/ingest_${dataset}.txt
    
    cnt_loaded_files=`grep 'Loading file' $ONTOPOP_LOGS/ingest/ingest_${dataset}.txt | wc -l`
    echo "Number of loaded files: ${cnt_loaded_files}" >> ${ONTOPOP_LOGS}/ingest/loaded_files_${dataset}.txt

    #grep 'ERROR' $ONTOPOP_LOGS/ingest/ingest_${dataset}.txt >> ${ONTOPOP_LOGS}/ingest/errors_${dataset}.txt

}

function create_dataset () {
    dataset=$1
    num_workers=64

    if [[ "$dataset" == "ontopop" ]]; then
        # Create directories
        mkdir -p ${ONTOPOP_LOGS}/create_dataset
        mkdir -p ${ONTOPOP_DATA}/create_dataset/papers
        mkdir -p ${ONTOPOP_DATA}/create_dataset/parsed_papers/tika
        mkdir -p ${ONTOPOP_DATA}/create_dataset/parsed_papers/pypdfloader
        mkdir -p ${ONTOPOP_DATA}/create_dataset/orkg_dumps
        mkdir -p ${ONTOPOP_DATABASES}
        mkdir -p ${GRAPHDB_HOME}

        apptainer exec \
        --fakeroot \
        --writable-tmpfs \
        --bind $GRAPHDB_HOME:/home/ontopop/databases/graphdb/home \
        --env "GDB_JAVA_OPTS=-Xmx50g -Xms50g -Dgraphdb.home=/home/ontopop/databases/graphdb/home -Dgraphdb.workbench.importDirectory=/opt/graphdb/home/graphdb-import -Dgraphdb.workbench.cors.enable=true -Denable-context-index=true -Dentity-pool-implementation=transactional -Dhealth.max.query.time.seconds=60 -Dgraphdb.append.request.id.headers=true -Dreuse.vars.in.subselects=true -Dgraphdb.connector.port=7213" \
        $APPTAINER_IMG /home/ontopop/databases/graphdb/dist/bin/graphdb &

        echo "Waiting for GraphDB to start..."
        while ! nc -z localhost 7213; do
            sleep 1
        done
        echo "GraphDB is running!"

        apptainer exec \
        --fakeroot \
        --writable-tmpfs \
        --bind src/ontopop/dataset.py:${c_home}/src/ontopop/dataset.py \
        --bind src/ontopop/sparql_queries:${c_home}/src/ontopop/sparql_queries \
        --bind src/utils:${c_home}/src/ontopop/utils \
        --bind ${ONTOPOP_LOGS}/create_dataset:${c_logs}/create_dataset\
        --bind ${ONTOPOP_DATABASES}:${c_databases} \
        --bind ${ONTOPOP_DATA}/create_dataset:${c_data}/create_dataset\
        --env "ORKG_ENDPOINT=${orkg_local_endpoint}" \
        --env "NUM_WORKERS=${num_workers}" \
        --env "EXECUTE_QUERY=1" \
        --env "DOWNLOAD_PAPERS=1" \
        $APPTAINER_IMG python3 -u ${c_home}/src/ontopop/dataset.py 2>&1 

        ps aux | grep 'graphdb' | grep -v 'grep' | awk '{print $2}' | xargs -r kill -9   
    else
        echo "Invalid dataset name: $dataset"
        echo "Usage: create_dataset {ontopop}"
    fi
}

function generate () {
    e_HF_TOKEN=$(grep 'HF_TOKEN' src/ontopop/configs/tokens.cfg | cut -d '=' -f2)
    PDF_PARSER=$1
    LLM=$2
    SHOTS=$3
    echo "Confiugration: ${PDF_PARSER} ${LLM} ${SHOTS}"

    LLM_SHORT=`echo $LLM | cut -d '/' -f 2`

    mkdir -p ${ONTOPOP_LOGS}/generate
    mkdir -p ${ONTOPOP_DATA}/create_dataset
    mkdir -p ${ONTOPOP_DATA}/generate/prompts/${PDF_PARSER}/${LLM_SHORT}/${SHOTS}
    mkdir -p ${ONTOPOP_MODELS}/huggingface


    apptainer exec \
    --nv \
    --fakeroot \
    --writable-tmpfs \
    --bind src/ontopop/generate.py:${c_home}/src/ontopop/generate.py \
    --bind src/ontopop/sparql_queries:${c_home}/src/ontopop/sparql_queries \
    --bind src/ontopop/instructions:${c_home}/src/ontopop/instructions \
    --bind src/utils:${c_home}/src/ontopop/utils \
    --bind ${ONTOPOP_LOGS}/generate:${c_logs}/generate \
    --bind ${ONTOPOP_DATA}/create_dataset:${c_data}/create_dataset \
    --bind ${ONTOPOP_DATA}/generate:${c_data}/generate \
    --bind ${ONTOPOP_MODELS}:${c_models} \
    --env "HF_HOME=${c_models}/huggingface" \
    --env "HF_TOKEN=${e_HF_TOKEN}" \
    --env "TORCH_USE_CUDA_DSA=1" \
    --env "CUDA_LAUNCH_BLOCKING=1" \
    --env "PDF_PARSER=${PDF_PARSER}" \
    --env "LLM=${LLM}" \
    --env "SHOTS=${SHOTS}" \
    $APPTAINER_IMG python3 -u ${c_home}/src/ontopop/generate.py 2>&1 

}


function evaluate () {
    e_HF_TOKEN=$(grep 'HF_TOKEN' src/ontopop/configs/tokens.cfg | cut -d '=' -f2)
    PDF_PARSER=$1
    LLM=$2
    SHOTS=$3
    echo "Confiugration: ${PDF_PARSER} ${LLM} ${SHOTS}"

    mkdir -p ${ONTOPOP_LOGS}/evaluate
    mkdir -p ${ONTOPOP_DATA}/generate
    mkdir -p ${ONTOPOP_MODELS}

    apptainer exec \
    --nv \
    --fakeroot \
    --writable-tmpfs \
    --bind src/ontopop/evaluate.py:${c_home}/src/ontopop/evaluate.py \
    --bind ${ONTOPOP_LOGS}/evaluate:${c_logs}/evaluate \
    --bind ${ONTOPOP_DATA}/generate:${c_data}/generate \
    --bind ${ONTOPOP_DATA}/evaluate:${c_data}/evaluate \
    --bind ${ONTOPOP_MODELS}:${c_models} \
    --env "HF_HOME=${c_models}/huggingface" \
    --env "HF_TOKEN=${e_HF_TOKEN}" \
    --env "TORCH_USE_CUDA_DSA=1" \
    --env "CUDA_LAUNCH_BLOCKING=1" \
    --env "PDF_PARSER=${PDF_PARSER}" \
    --env "LLM=${LLM}" \
    --env "SHOTS=${SHOTS}" \
    $APPTAINER_IMG python3 -u ${c_home}/src/ontopop/evaluate.py 2>&1 

}

function visualize () {
    PDF_PARSER=$1
    SHOTS=$2
    echo "Confiugration: ${PDF_PARSER} ${SHOTS}"

    mkdir -p ${ONTOPOP_LOGS}/visualize
    mkdir -p ${ONTOPOP_DATA}/visualize

    apptainer exec \
    --nv \
    --fakeroot \
    --writable-tmpfs \
    --bind src/ontopop/visualize.py:${c_home}/src/ontopop/visualize.py \
    --bind ${ONTOPOP_LOGS}/visualize:${c_logs}/visualize \
    --bind ${ONTOPOP_DATA}/download:${c_data}/download\
    --bind ${ONTOPOP_DATA}/create_dataset:${c_data}/create_dataset\
    --bind ${ONTOPOP_DATA}/evaluate:${c_data}/evaluate\
    --bind ${ONTOPOP_DATA}/visualize:${c_data}/visualize\
    --bind ${ONTOPOP_PLOTS}:${c_plots}\
    --env "PDF_PARSER=${PDF_PARSER}" \
    --env "SHOTS=${SHOTS}" \
    $APPTAINER_IMG python3 -u ${c_home}/src/ontopop/visualize.py 2>&1 

}


# Models and PDF loaders to evaluate:
# tika, pypdfloader
# meta-llama/Meta-Llama-3-8B-Instruct, tiiuae/Falcon3-10B-Instruct, mistralai/Mistral-7B-Instruct-v0.3
if [ "$1" = "download" ]; then
    download $2
elif [ "$1" = "download_contributors" ]; then
    download_contributors
elif [ "$1" = "correct" ]; then
    correct $2
elif [ "$1" = "ingest" ]; then
    ingest $2
elif [ "$1" = "create_dataset" ]; then
    create_dataset $2
elif [ "$1" = "generate" ]; then
    generate $2 $3 $4
elif [ "$1" = "evaluate" ]; then
    evaluate $2 $3 $4
elif [ "$1" = "visualize" ]; then
    visualize $2 $3
else
    echo "Available functions are: download, download_contributors, correct, ingest, create_dataset, generate, evaluate, visualize."
fi
