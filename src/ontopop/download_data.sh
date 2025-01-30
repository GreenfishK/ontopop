#!/bin/bash

# Logging variables
log_timestamp() { date +%Y-%m-%d\ %A\ %H:%M:%S; }
log_level="root:INFO"

download_orkg() {
    base_dir=/home/ontopop/data/download
    
    # Create empty log files
    log_file_orkg=/home/ontopop/logs/download/download_orkg.txt
    > $log_file_orkg

    # Remove previous downloads
    echo "$(log_timestamp) ${log_level}: Removing previous downloads from ${base_dir} ..." >> $log_file_orkg
    rm -rf ${base_dir}/*
    mkdir -p $base_dir

    # Download orkg
    echo "$(log_timestamp) ${log_level}: Downloading ORKG snapshot..." >> $log_file_orkg
    wget -t 3 -c -P ${base_dir} "https://orkg.org/api/rdf/dump" -O ${base_dir}/orkg.nt >> $log_file_orkg
}

# Check command-line argument and call corresponding function
if [ "$1" = "orkg" ]; then
    download_orkg
else
    echo "$(log_timestamp) ${log_level}: Choose one from: ['orkg']" 
fi
