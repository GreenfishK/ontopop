from SPARQLWrapper import Wrapper, SPARQLWrapper, POST, JSON
import logging
import csv
import pandas as pd
import os
import numpy as np

def to_list(result: Wrapper.QueryResult) -> list:
    """

    :param result:
    :return: Dataframe
    """

    results = result.convert()

    def format_value(res_value):
        value = res_value["value"]
        lang = res_value.get("xml:lang", None)
        datatype = res_value.get("datatype", None)
        if lang is not None:
            value += "@" + lang
        if datatype is not None:
            value += " [" + datatype + "]"
        return value

    header = []
    values = []

    if not "head" in results or not "vars" in results["head"]:
        return header

    if not "results" in results or not "bindings" in results["results"]:
        return values

    for var in results["head"]["vars"]:
        header.append(var)

    for r in results["results"]["bindings"]:
        row = []
        for col in results["head"]["vars"]:
            if col in r:
                result_value = format_value(r[col])
            else:
                result_value = None
            row.append(result_value)
        values.append(row)
    
    return [header] + values


def load_query(sparql_engine: Wrapper.SPARQLWrapper, query_file_path: str, query_args: dict = {}):
    assert type(sparql_engine) == Wrapper.SPARQLWrapper

    logging.info(f"Loading query {query_file_path} with parameters {query_args}")
    file = open(query_file_path, "r")
    query_text = file.read()
    file.close()

    for key, value in query_args.items():
        query_text = query_text.replace(key, value)
    sparql_engine.setQuery(query_text)

    return query_text


def save_result_set(result_set_name: str, result_set: list):
    logging.info(f"Writing result set...: {result_set_name} \n")
    file = open(result_set_name, 'w')
    write = csv.writer(file, delimiter=";")
    write.writerows(result_set)
    file.close()


def get_results_as_df(query: str, sparql_endpoint: str, prefixes: dict = {}):
    def get_results(endpoint_url, query, format=JSON):
        #user_agent = "WDQS-example Python/%s.%s" % (sys.version_info[0], sys.version_info[1])
        # TODO adjust user agent; see https://w.wiki/CX6
        sparql = SPARQLWrapper(endpoint_url)
        sparql.setQuery(query)
        sparql.setOnlyConneg(True)
        sparql.setMethod(POST)
        sparql.setReturnFormat(format)
        return sparql.query().convert()

    attributes = get_results(sparql_endpoint, query)
    print("Results retrieved. Now converting to a dataframe ...")

    check = False
    variables = attributes['head']['vars']
    bindings = attributes['results']['bindings']
    result_list1 = []
    for binding in bindings:
        row = []
        for v in variables:
            if v in binding:
                binding_value = binding[v]['value']
                if prefixes:
                    for key, value in prefixes.items():
                        binding_value = binding_value.replace(value, key + ":") if binding_value.startswith(value) else binding_value
                row.append(binding_value)
            else:
                row.append(np.nan)
                
        result_list1.append(row)

    df = pd.DataFrame(result_list1, columns=variables)
    df = df.replace("", np.nan)
    logging.info(f"Dataframe columns information: {df.dtypes}")
    logging.info(f"Dataframe NaNs: {df.isna().sum()}")
    
    return df


def get_result_set(query_file_path: str, endpoint_url: str, result_sets_dir: str, execute: bool = False):
    """
    Execute a query, :query_file_path:, against a SPARQL endpoint, :endpoint_url:, 
    save the result set as CSV to :result_sets_dir and return the result set as a 
    pandas DataFrame.
    If :execute: is False and a result set from a previous execution exists, this 
    result set will be returned.
    """
    query_file_name = query_file_path.split("/")[-1].split(".")[0]
    if os.path.exists(f"{result_sets_dir}/{query_file_name}.csv") and not execute:
        result_set_df = pd.read_csv(f"{result_sets_dir}/{query_file_name}.csv", escapechar='\\')
        logging.info(f"Dataframe columns information: {result_set_df.dtypes}")
        logging.info(f"Dataframe NaNs: {result_set_df.isna().sum()}")

    else:
        with open(query_file_path, "r") as query_file:
            query = query_file.read()
        # Query ORKG
        result_set_df = get_results_as_df(query, endpoint_url)
        result_set_df.to_csv(f"{result_sets_dir}/{query_file_name}.csv", index=False, escapechar='\\')
    return result_set_df