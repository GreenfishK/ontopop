#!/bin/bash
export JAVA_HOME=/opt/java/java11/openjdk
export PATH=/opt/java/java11/openjdk:$PATH

# Logging variables
log_file=/home/ontopop/logs/validate/validate_data.txt
log_timestamp() { date +%Y-%m-%d\ %A\ %H:%M:%S; }
log_level="root:INFO"

# Prepare directories and files
rm -rf /home/ontopop/logs/validate
mkdir -p /home/ontopop/logs/validate
> $log_file

# Path variables
input_file="/home/ontopop/data/ontopop/orkg/orkg.nt"
tmp_file="/home/ontopop/data/ontopop/orkg/orkg_tmp.nt"
invalid_lines_file="/home/ontopop/data/ontopop/orkg/orkg_invalid_lines.nt"
output_file=$input_file

# Read dataset $input_file line by line. 
# If the triple is invalid write it to $output_file with a '#' upfront. Otherwise write the line as it is.
echo "$(log_timestamp) ${log_level}:Validating $input_file" >> $log_file

java -jar $c_tools/rdfvalidator-1.0-jar-with-dependencies.jar $input_file $tmp_file
grep -a '^# ' ${tmp_file} > ${invalid_lines_file}
orkg_invalid_lines=`grep -c '^# ' ${invalid_lines_file}`
orkg_invalid_lines=$(($orkg_invalid_lines))
sed -i "1s/^/# invalid_lines_excluded: ${orkg_invalid_lines}\n/" $invalid_lines_file
cp ${tmp_file} ${output_file}
rm ${tmp_file}
echo "$(log_timestamp) ${log_level}:${input_file}: Invalid lines: $orkg_invalid_lines" >> $log_file


        
    


