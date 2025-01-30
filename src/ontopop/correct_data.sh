#!/bin/bash

######################################################
# ORKG
######################################################
function correct_orkg() {
    # Logging variables
    log_file=/home/ontopop/logs/correct/correct_orkg.txt
    log_timestamp() { date +%Y-%m-%d\ %A\ %H:%M:%S; }
    log_level="root:INFO"

    # Create empty log file
    > $log_file

    # Other variables
    orkg_input_dir=/home/ontopop/data/download
    orkg_output_dir=/home/ontopop/data/correct

    # Make a copy of the raw data and put it into the directory for processed data. 
    if [ -d "$orkg_input_dir" ]; then
        echo "$(log_timestamp) ${log_level}: Move data from $orkg_input_dir to $orkg_output_dir" >> $log_file
        cp ${orkg_input_dir}/* $orkg_output_dir
    else
        echo "$(log_timestamp) ${log_level}: Data will not be moved from $orkg_input_dir because it does not exist." >> $log_file
    fi

    # Rename orkg.nt to orkg.ttl. This is because in .ttl RDF literals can be enquoted like this: """<RDF literal>""" while in .nt they seemingly cannot.
    mv ${orkg_output_dir}/orkg.nt ${orkg_output_dir}/orkg.ttl


    ######################################################
    # Correction rules
    ######################################################

    # Remove ^^<null> datatypes
    echo "$(log_timestamp) ${log_level}: Removing <null> datatypes from: ${orkg_output_dir}/orkg.ttl" >> $log_file
    awk '{gsub(/\^\^<null>/, ""); print}' ${orkg_output_dir}/orkg.ttl > ${orkg_output_dir}/orkg_tmp.ttl
    mv ${orkg_output_dir}/orkg_tmp.ttl ${orkg_output_dir}/orkg.ttl

    # Replace Line Separators (=hex code e2 80 a8) with a space
    echo "$(log_timestamp) ${log_level}: Replacing Line Separators with the hex code e2 80 a8 with a space from: ${orkg_output_dir}/orkg.ttl" >> $log_file
    sed -i 's/\xe2\x80\xa8/ /g' ${orkg_output_dir}/orkg.ttl

    # Remove invalid datatype IRIs which do not start with 'http'
    echo "$(log_timestamp) ${log_level}: Removing invalid datatype IRIs which do not start with 'http' from: ${orkg_output_dir}/orkg.ttl" >> $log_file
    sed -i 's/\^\^<[^hH][^tT][^tT][^pP][^>]*>//g' ${orkg_output_dir}/orkg.ttl

    # Remove line break at the end of a literal
    echo "$(log_timestamp) ${log_level}: Remove line break at the end of a literal in: ${orkg_output_dir}/orkg.ttl" >> $log_file
    awk '{
    while (match($0, /\\n"(\^\^<http:[^>]+>)/)) {
        prefix = substr($0, 1, RSTART - 1);
        suffix = substr($0, RSTART + RLENGTH);
        replacement = "\"" substr($0, RSTART + 3, RLENGTH - 3);
        $0 = prefix replacement suffix;
    }
    print
    }' ${orkg_output_dir}/orkg.ttl > ${orkg_output_dir}/orkg_tmp.ttl
    mv ${orkg_output_dir}/orkg_tmp.ttl ${orkg_output_dir}/orkg.ttl

    # Replace empty strings typed with an XMLSchema datatype (string, int, ...) with the anyURI datatype
    echo "$(log_timestamp) ${log_level}: Type empty strings with anyURI in: ${orkg_output_dir}/orkg.ttl" >> $log_file
    awk '{ gsub(/> ""\^\^<http:\/\/www\.w3\.org\/2001\/XMLSchema#[^>]+>/, "> \"\"^^<http://www.w3.org/2001/XMLSchema#anyURI>"); print }' ${orkg_output_dir}/orkg.ttl > ${orkg_output_dir}/orkg_tmp.ttl 
    mv ${orkg_output_dir}/orkg_tmp.ttl ${orkg_output_dir}/orkg.ttl

    # Replace the decimal datatype with string for the version property
    echo "$(log_timestamp) ${log_level}: Replace the decimal datatype with the string datatype for the version property from: ${orkg_output_dir}/orkg.ttl" >> $log_file
    awk '
    BEGIN { FS = " " }
    {
    # If the predicate matches the specific URI
    if ($2 == "<http://orkg.org/orkg/predicate/P45088>") {
        # Replace decimal with string only for this predicate
        gsub(/\^\^<http:\/\/www\.w3\.org\/2001\/XMLSchema#decimal>/, "^^<http://www.w3.org/2001/XMLSchema#string>")
    }
    print
    }' ${orkg_output_dir}/orkg.ttl > ${orkg_output_dir}/orkg_tmp.ttl
    mv ${orkg_output_dir}/orkg_tmp.ttl ${orkg_output_dir}/orkg.ttl

    # Remove leading and trailing spaces, as well as invisible space characters around decimals
    echo "$(log_timestamp) ${log_level}: Remove leading and trailing spaces before decimals in: ${orkg_output_dir}/orkg.ttl" >> $log_file
    sed -E 's/"[[:space:]\xE2\x80\x89\xE2\x80\x85\xC2\xA0]*([0-9]+[.,][0-9]+)[[:space:]\xE2\x80\x89\xE2\x80\x85\xC2\xA0]*"\^\^<http:\/\/www.w3.org\/2001\/XMLSchema#decimal>/"\1"^^<http:\/\/www.w3.org\/2001\/XMLSchema#decimal>/g' ${orkg_output_dir}/orkg.ttl > ${orkg_output_dir}/orkg_tmp.ttl
    mv ${orkg_output_dir}/orkg_tmp.ttl ${orkg_output_dir}/orkg.ttl

    # Remove leading and trailing spaces, as well as invisible space characters around integers
    echo "$(log_timestamp) ${log_level}: Remove leading and trailing spaces before integers in: ${orkg_output_dir}/orkg.ttl" >> $log_file
    sed -E 's/"[[:space:]\xE2\x80\x89\xE2\x80\x85\xC2\xA0]*([0-9]+)[[:space:]\xE2\x80\x89\xE2\x80\x85\xC2\xA0]*"\^\^<http:\/\/www.w3.org\/2001\/XMLSchema#integer>/"\1"^^<http:\/\/www.w3.org\/2001\/XMLSchema#integer>/g' ${orkg_output_dir}/orkg.ttl > ${orkg_output_dir}/orkg_tmp.ttl
    mv ${orkg_output_dir}/orkg_tmp.ttl ${orkg_output_dir}/orkg.ttl

    # Substitute integer datatypes the decimal datatype for literals that contain decimals and that are typed with integer
    echo "$(log_timestamp) ${log_level}: Substitute integer datatypes the decimal datatype for literals that contain decimals and that are typed with integer in: ${orkg_output_dir}/orkg.ttl" >> $log_file
    sed -E 's/"([0-9]*\.[0-9]+)"\^\^<http:\/\/www\.w3\.org\/2001\/XMLSchema#integer>/"\1"^^<http:\/\/www\.w3\.org\/2001\/XMLSchema#decimal>/g' ${orkg_output_dir}/orkg.ttl > ${orkg_output_dir}/orkg_tmp.ttl 
    mv ${orkg_output_dir}/orkg_tmp.ttl ${orkg_output_dir}/orkg.ttl

    # Surround RDF literals with two double quotation marks if they contain escape characters
    #echo "$(log_timestamp) ${log_level}: Surround RDF literals with two double quotation marks if they contain escape characters in: ${orkg_output_dir}/orkg.ttl" >> $log_file
    #sed -E 's/(".*\\.*")(\^\^<[^>]*>)?(\s*\.$)/""\1""\2\3/g' ${orkg_output_dir}/orkg.ttl > ${orkg_output_dir}/orkg_tmp.ttl
    #mv ${orkg_output_dir}/orkg_tmp.ttl ${orkg_output_dir}/orkg.ttl

    # Escape backslashes in front of the closing quotation mark of an RDF literal
    echo "$(log_timestamp) ${log_level}: Escape backslashes in front of the closing quotation mark of an RDF literal in: ${orkg_output_dir}/orkg.ttl" >> $log_file
    sed -E 's/(".*)(\\""")(\^\^<[^>]*>)?(\s*\.$)/\1\\\\"""\3\4/g' ${orkg_output_dir}/orkg.ttl > ${orkg_output_dir}/orkg_tmp.ttl
    mv ${orkg_output_dir}/orkg_tmp.ttl ${orkg_output_dir}/orkg.ttl

    # Add a separating "T" inside a date literal of the form "YYYY-MM-DD hh:mm:dd"
    echo "$(log_timestamp) ${log_level}: Add a separating \"T\" inside a date literal of the form "YYYY-MM-DD hh:mm:dd" in: ${orkg_output_dir}/orkg.ttl" >> $log_file
    sed -E 's/("[0-9]{4}-[0-9]{2}-[0-9]{2})( )([0-9]{2}:[0-9]{2}:[0-9]{2}")(\^\^<http:\/\/www.w3.org\/2001\/XMLSchema#)(date>)/\1T\3\4dateTime>/g' ${orkg_output_dir}/orkg.ttl > ${orkg_output_dir}/orkg_tmp.ttl
    mv ${orkg_output_dir}/orkg_tmp.ttl ${orkg_output_dir}/orkg.ttl

}



# Check command-line argument and call corresponding function
if [ "$1" = "orkg" ]; then
    correct_orkg
else
    echo "$(log_timestamp) ${log_level}: Choose one from: ['orkg']" 
fi
