#!/bin/bash

. .env

function install_apptainer() {
    sudo add-apt-repository -y ppa:apptainer/ppa
    sudo apt update
    sudo apt install -y apptainer

    # Set the range size
    RANGE_SIZE=65536

    # Find the highest used UID/GID range from /etc/subuid and /etc/subgid
    MAX_UID=$(cut -d':' -f2 /etc/subuid | sort -n | tail -1)
    MAX_GID=$(cut -d':' -f2 /etc/subgid | sort -n | tail -1)

    # Calculate the starting UID and GID for root
    START_UID=$((MAX_UID + RANGE_SIZE))
    START_GID=$((MAX_GID + RANGE_SIZE))

    if grep -q "^root:" /etc/subuid; then
        echo "A root entry already exists in /etc/subuid. No changes made."
    else
        # Add the new entry for root to /etc/subuid
        echo "root:${START_UID}:${RANGE_SIZE}" | sudo tee -a /etc/subuid
    fi

    if grep -q "^root:" /etc/subgid; then
        echo "A root entry already exists in /etc/subgid. No changes made."
    else
        # Add the new entry for root to /etc/subgid
        echo "root:${START_GID}:${RANGE_SIZE}" | sudo tee -a /etc/subgid
    fi

}

ENV_FILE=".env"
# Create directories. Make sure that they are the same as 
# in the "data sub directories" and "home sub directories" section of the .env file
# Read each line from the .env file
while IFS= read -r line; do
  # Check if the line is within the "Create directories" section
  if [[ $line == "# !!!Create directories!!!" ]]; then
    start_parsing=true
    continue
  fi

  # Stop parsing once we exit the section
  if [[ $start_parsing == true && $line == "# !!!Create directories!!!" ]]; then
    break
  fi

  # Extract and create directories if the line is an assignment in the section
  if [[ $start_parsing == true && $line =~ ^[A-Z_]+=(.+) ]]; then
    # Expand environment variables in the path
    DIR_PATH=$(eval echo "${BASH_REMATCH[1]}")
    # Create the directory if it doesn't already exist
    mkdir -p "$DIR_PATH"
    echo "Created directory: $DIR_PATH"
  fi
done < "$ENV_FILE"

server="$1"

if [[ $server == "vsc5" ]]; then
    module load --auto apptainer/1.0.2-gcc-12.2.0-ufkuvan 
    if [[ ! -f $APPTAINER_IMG ]]; then
        echo "Apptainer image ontopop.sif was not found in ${APPTAINER_IMG}. Make sure to bring your own image and place it into the aforementioned path."
    else
        echo "Apptainer image ontopop.sif available in ${APPTAINER_IMG}."
    fi
elif [[ $server == "datalab" ]]; then
    # Note: Make sure you have read and write permissions in your $DATA directory
    # e.g. change permissions for $DATA with `chmod 775``
    # Note: Our apptainer commands are executed in fakeroot mode. It means that the 
    # directories in $DATA where the data is written to are owned by root.
    # Thus, they can be manipulated e.g. by you, which should, however, 
    # only be done during experimentation.
    apptainer build $APPTAINER_IMG $APPTAINER_DEF
else 
    # Sudo rights required!!
    install_apptainer
    apptainer build $APPTAINER_IMG $APPTAINER_DEF
fi
