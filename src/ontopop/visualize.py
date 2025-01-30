##########################################################################
# Questions I want to answer/Information i want to show
##########################################################################
# Plot 1: plot_similarities()
# What properties with over 50 instances excert the highest avg max semantic similarity, 
# i.e. what properties get good suggestions from the RAG? (done)

# Plot 2: plot_hallucination()
# How many generated property values exactly match the n-grams in the snippets? 
# How many generated property values are found in the full-text?
# line plot: subplot 1:
# y-axis:
# * exact_match_snippets
# x-axis:
# * property index
# filters:
# * mismatch_rate > 10%
# line plot: subplot 2:
# y-axis:
# * exact_match_full_text
# x-axis:
# * property index
# filters:
# * > 90th percentile

# Plot 3
# Retriever evaluation
# How many of the crowdsourced property values are contained within the full text
# * How many thereof contained within the snippets

# Plot 4
# How many crowdsourced and generated property value pairs exactly match, but are not found in the snippets?

# Plot 5
# Errors
# Note: Already covered in "Plot 1"? Still necessary?
# Correlation between number of property instances (x-axis) and number of errors (y-axis)
# How many XML parsing errors are there (XMLParsing)
# How many answers were delievered in the correct format (WrongAnswerFormat)?
# How many property values could not be assigned (NoValuesGenerated)?

# Plot(?): Confusion matrix
# True positive: generated property values that match the crowdsourced property values and are found in the snippets
# False positive: generated property values that match the crowdsourced property values and are not found in the snippets
# True negative: generated property values that do not match the crowdsourced property values and are not found in the snippets
# False negative: generated property values that do not match the crowdsourced propertys values and are found in the snippets

##########################################################################
# Considerations
##########################################################################
# Experiment variables:
# * LLMs (one with 8B and one with over 200B parameters)
# * PDF loaders (Tika, PyPDFLoader, ...)
# * Instructions 


##########################################################################################
# Imports, diretories setup and tokens
##########################################################################################
import matplotlib.pyplot as plt
import pandas as pd
import os
import logging
import numpy as np
import matplotlib.ticker as ticker
import matplotlib.patches as mpatches
from sklearn.cluster import KMeans
from matplotlib.ticker import FuncFormatter
import math
from matplotlib.cm import ScalarMappable


##########################################################################################
# Paths, Endpoints, Tokens and Environment Variables
##########################################################################################
log_dir = f"/home/ontopop/logs/visualize"
create_dataset_dir = f"/home/ontopop/data/create_dataset"
evaluate_dir = f"/home/ontopop/data/evaluate"
visualize_dir = f"/home/ontopop/data/visualize"
plots_dir = "/home/ontopop/plots"

##########################################################################################
# Logging
##########################################################################################
# Read environment variables
pdf_parser=os.environ["PDF_PARSER"]
shots=os.environ["SHOTS"]

# Logging configuration
log_file_path = f"{log_dir}/visualize_{pdf_parser}_{shots}.txt"

# Logging configuration
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
def set_size(width, fraction=1):
    """Set figure dimensions to avoid scaling in LaTeX.

    Parameters
    ----------
    width: float
            Document textwidth or columnwidth in pts
    fraction: float, optional
            Fraction of the width which you wish the figure to occupy

    Returns
    -------
    fig_dim: tuple
            Dimensions of figure in inches
    """
    # Width of figure (in pts)
    fig_width_pt = width * fraction

    # Convert from pt to inches
    inches_per_pt = 1 / 72.27

    # Golden ratio to set aesthetic figure height
    # https://disq.us/p/2940ij3
    golden_ratio = (5**.5 - 1) / 2

    # Figure width in inches
    fig_width_in = fig_width_pt * inches_per_pt
    # Figure height in inches
    fig_height_in = fig_width_in * golden_ratio

    fig_dim = (fig_width_in, fig_height_in)

    return fig_dim

##########################################################################################
# Plots
##########################################################################################
# Read environment variables
pdf_parser=os.environ["PDF_PARSER"]
shots=os.environ["SHOTS"]

# Formats for Latex template
#latex_width=347
fig_size = (16,9)
#plt.rc('font', size=12)  # Base font size
#plt.rc('axes', labelsize=14)  # Axis label size
#plt.rc('xtick', labelsize=14)  # X-tick label size
#plt.rc('ytick', labelsize=14)  # Y-tick label size
#plt.rc('legend', fontsize=12)  # Legend font size

def plot_template_properties(input_dataset_file_name, output_dataset_file_name, plot_file_name):
    """
    Frequency of template property numbers grouped into three clusters
    """
    logging.info(f"Creating plot: {plot_file_name}")

    # Prepare data for plot
    templates_df = pd.read_csv(f"{create_dataset_dir}/{input_dataset_file_name}", sep=",")
    logging.info(f"Loading dataset: {create_dataset_dir}/{input_dataset_file_name}")

    # Cluster cntTemplateProperty
    cntTemplateProperty_values = templates_df["cntTemplateProperty"].values.reshape(-1, 1).astype(int)
    kmeans = KMeans(n_clusters=3, random_state=42, n_init=100)
    kmeans.fit(cntTemplateProperty_values)
    cluster_centers = kmeans.cluster_centers_.flatten()
    cluster_centers.sort()
    clusters = kmeans.predict(cntTemplateProperty_values)

    # Create a list of cluster ranges
    cluster_ranges_list = []
    for i in range(3):  # Iterate over cluster IDs
        cluster_values = cntTemplateProperty_values[clusters == i].flatten()
        min_value = cluster_values.min()
        max_value = cluster_values.max()
        cluster_ranges_list.append([min_value, max_value])

    # Create a mapping from original cluster IDs to new cluster IDs based on the cluster ranges
    sorted_cluster_ranges = sorted(cluster_ranges_list)
    remap_dict = {i: sorted_cluster_ranges.index(cluster_range) for i, cluster_range in enumerate(cluster_ranges_list)}

    # Get unique values and their counts
    unique_values, counts = np.unique(cntTemplateProperty_values, return_counts=True)
    cluster_colors = {0:'black', 1:'dimgrey', 2:'white'}
    label_margin = 1

    # Create a figure with the desired size
    fig, ax = plt.subplots(figsize=fig_size)

    # Plot bars for each unique value
    for value, count in zip(unique_values, counts):
        cluster_index = kmeans.predict([[value]])[0]
        remapped_cluster_index = remap_dict[cluster_index]
        color = cluster_colors[remapped_cluster_index]
        
        # Check if frequency is 1, and annotate with templateLabel vertically
        if count == 1:
            template_label = templates_df.loc[templates_df["cntTemplateProperty"] == value, "templateLabel"].values[0]
            ax.bar(value, count, color=color, edgecolor="black")
            ax.text(value, count + label_margin, template_label, ha='center', va='bottom', rotation='vertical', fontsize=10)
        else:
            ax.bar(value, count, color=color, edgecolor="black")

    for center in cluster_centers:
        cluster_index = kmeans.predict([[center]])[0]
        remapped_cluster_index = remap_dict[cluster_index]
        color = cluster_colors[remapped_cluster_index]
        ax.axvline(x=center, color="black", linestyle='--')

    # Set x-axis and y-axis labels
    ax.xaxis.set_label_text('Number of template properties', fontsize=16)
    ax.xaxis.set_ticks(unique_values)
    ax.xaxis.set_ticklabels(unique_values,rotation=90)
    
    ax.yaxis.set_label_text('Number of templates', fontsize=16)
    ax.yaxis.set_ticks(np.arange(0, max(counts) + 1, 10))

    # Spines
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)

    plt.tight_layout()
    plt.savefig(f"{plots_dir}/{plot_file_name}", format="eps")


def plot_template_usage(input_dataset_file_name, output_dataset_file_name, plot_file_name):
    """
    Usage of ORKG templates. The highlighted templates have no properties.
    """
    logging.info(f"Creating plot: {plot_file_name}")

    # Prepare data for plot
    templates_df = pd.read_csv(f"{create_dataset_dir}/{input_dataset_file_name}", sep=",")
    logging.info(f"Loading dataset: {create_dataset_dir}/{input_dataset_file_name}")
    
    cnt_templates = templates_df['template'].count()
    cnt_zero_usages = templates_df.loc[templates_df['cntTemplateInstances'] == 0, 'template'].count()

    # Plot
    fig, ax1 = plt.subplots(1, 1, figsize=fig_size, sharex=False)  # Create subplots

    # Plot a line with cntTemplateInstance
    templates_df = templates_df.sort_values("cntTemplateInstances", ascending=False)
    templates_df.plot(x='template', y='cntTemplateInstances', ax=ax1, 
                                kind="line", legend=False, color="black")

    # Plot a bar plot to highlight the templates without any properties
    templates_df_zero_properties = templates_df.copy()
    templates_df_zero_properties.plot(x='template', y='cntTemplateInstances',
                                ax=ax1,
                                kind="bar", 
                                fontsize=12,
                                legend=False,
                                color=['black' if cntTemplateProperty == 0 else 'white' for cntTemplateProperty in templates_df_zero_properties['cntTemplateProperty']],
                                xlabel=False
                                )

    # Add vertical labels for templates without properties and without template instances
    for index, row in templates_df.iterrows():
        if row['cntTemplateProperty'] == 0 and row['cntTemplateInstances'] > 0:
            bar_index = templates_df_zero_properties.index.get_loc(index)  # Get the index position of the bar
            text_y = row['cntTemplateInstances'] * 1.2  # Adjust the multiplication factor to increase or decrease the space between the bar and the text label
            ax1.text(bar_index, text_y, row['templateLabel'], rotation=90, va='bottom', ha='center', color='black')

    # Ticks
    max_cnt = templates_df['cntTemplateInstances'].max()
    max_power = math.floor(math.log10(max_cnt))

    x_tick_positions = [1, cnt_templates - cnt_zero_usages, cnt_templates]
    ax1.set_xticks(x_tick_positions)  
    
    y_tick_positions = [10 ** i for i in range(0, max_power + 1)]
    y_tick_positions.append(max_cnt)
    ax1.yaxis.set_ticks(y_tick_positions)
    
    def format_func(value, tick_number):
        return '{:,.0f}'.format(value).replace(',', '.')
    ax1.yaxis.set_major_formatter(FuncFormatter(format_func)) 

    # Set labels and title
    ax1.xaxis.set_label_text('Template index', fontsize=16)
    ax1.yaxis.set_label_text('Number of template instances (log scale)', fontsize=16)

    x_tick_labels = [1, cnt_templates - cnt_zero_usages, cnt_templates]
    ax1.xaxis.set_ticklabels(x_tick_labels, rotation=0) 

    ax1.set_yscale('log') 

    # Spines
    ax1.spines["top"].set_visible(False)
    ax1.spines["right"].set_visible(False)

    # Save plot
    plt.tight_layout()
    plt.savefig(f"{plots_dir}/{plot_file_name}", format="eps")


def plot_template_utilization(input_dataset_file_name, contr_tmpl_dataset_file_name, output_dataset_file_name, plot_file_name):
    """
    Usage counts and utilization of the top 5% used templates.'
    """
    logging.info(f"Creating plot: {plot_file_name}")

    # Prepare data for plot
    templates_df = pd.read_csv(f"{create_dataset_dir}/{input_dataset_file_name}", sep=",")
    logging.info(f"Loading dataset: {create_dataset_dir}/{input_dataset_file_name}")

    # Sort dataset
    templates_df = templates_df.sort_values("cntTemplateInstances", ascending=False)

    # Cluster cntTemplateProperty
    cntTemplateProperty_values = templates_df["cntTemplateProperty"].values.reshape(-1, 1).astype(int)
    kmeans = KMeans(n_clusters=3, random_state=42, n_init=100)
    kmeans.fit(cntTemplateProperty_values)
    cluster_centers = kmeans.cluster_centers_.flatten()
    cluster_centers.sort()
    clusters = kmeans.predict(cntTemplateProperty_values)
    
    # Plots
    fig, ax1 = plt.subplots(1, 1, figsize=fig_size, sharex=False)  # Create subplots

    # Top 5% usage and utilization
    quantil = 0.95
    top_quant_threshold = templates_df['cntTemplateInstances'].quantile(quantil)
    top_quant_templates = templates_df[templates_df['cntTemplateInstances'] >= top_quant_threshold]
    top_quant_templates_index = top_quant_templates.index

    # Set colors for the bars based on the utilization ratio
    top_quant_data = templates_df.loc[top_quant_templates_index]
    top_quant_template_utilization_ratio = top_quant_data["templateUtilizationRatio"].fillna(0).to_list()
    top_quant_data["templateLabel"] = top_quant_data["templateLabel"].fillna("unknown")
    top_quant_data['templateLabelUnique'] = top_quant_data['templateLabel'] + "    " + top_quant_data["templateUtilizationRatio"].round(2).map(lambda x: f"{x:04.2f}") 

    # Set range for the y axis                              
    cnt_rows = len(top_quant_data["templateLabel"])
    y_min = -0.5
    y_max = cnt_rows - 0.5

    # Subplot 1 - Plot horizontal bars with colors based on utilization ratios
    cmap = plt.get_cmap('Greys')
    colors = [cmap(utilization_ratio) for utilization_ratio in top_quant_template_utilization_ratio]
    bars = ax1.barh(top_quant_data['templateLabelUnique'],
                    top_quant_data['cntTemplateInstances'],
                    color=colors,
                    edgecolor="black")

    # Customize plot
    def format_func(value, tick_number):
        return '{:,.0f}'.format(value/1000).replace(',', '.') + "k"
    ax1.xaxis.set_major_formatter(FuncFormatter(format_func))  

    # Labels and Fonts
    ax1.set_xlabel('Usage Count', fontsize=16)
    ax1.set_ylabel('Templates',fontsize=16)
    util_ratio_header = ax1.text(0,0, "Utilization ratio",
                               fontsize=12, rotation=45)
    
    for i, v in enumerate(top_quant_data["cntTemplateInstances"]):
        ax1.text(v + 1000, i, f"{v/1000:,.2f}k", va='center', fontsize=10)

    # Position
    ax1.set_position([0.2, 0.1, 0.7, 0.75])
    ax1.set_ylim(y_min, y_max)
    ax1.invert_yaxis()  # Invert y-axis to have the highest template on top
    util_ratio_header.set_position((ax1.get_position().x0 -2000, ax1.get_position().y1 -2))

    # Add colormap for utilization ratio
    sm = ScalarMappable(cmap=cmap)
    colorbar_axes = fig.add_axes([0.92, 0.1, 0.02, 0.75])  
    cbar = fig.colorbar(sm, cax=colorbar_axes, orientation='vertical')
    cbar.set_label('Utilization Ratio', fontsize=16)

    # Spines
    ax1.spines["top"].set_visible(False)
    ax1.spines["right"].set_visible(False)

    # Subplot 1.1 - Contribution template utilization
    contributions_df = pd.read_csv(f"{create_dataset_dir}/{contr_tmpl_dataset_file_name}", sep=",")
    logging.info(f"Loading dataset: {create_dataset_dir}/{contr_tmpl_dataset_file_name}")

    cnt_instances = contributions_df.loc[0, ["cntTemplateInstances"]].astype(int).item()
    contributions_df = contributions_df[["cntResearch_problem", "cntMaterial", "cntMethod", "cntResult"]]
    contributions_df = contributions_df.rename(columns={"cntResearch_problem": "Research_problem", 
                                                "cntMaterial": "Material",
                                                "cntMethod": "Method",
                                                "cntResult": "Result"})
    contributions_series = pd.Series(contributions_df.loc[0, :])
    contributions_series = contributions_series.squeeze()
    
    # Overlay plot: Contribution template utilization
    ax2 = fig.add_axes([
        ax1.get_position().x0 + 0.35,  
        ax1.get_position().y0 + 0.1,  
        0.2, 
        0.2  
    ])
    contributions_series.sort_values().plot.barh(ax=ax2, color='white', edgecolor='black')

    ax2.set_title("Contribution template utilization")
    ax2.xaxis.set_label_text("Number of template property usages")
    ax2.yaxis.set_label_text("'Contribution' template properties")

    for i, v in enumerate(contributions_series.sort_values()):
        ax2.text(v + 1000, i, f"{v/1000:,.2f}k", va='center', fontsize=10)

    ax2.set_xlim(0, cnt_instances)

    ax2.spines["top"].set_visible(False)
    ax2.spines["right"].set_visible(False)

    ticks = ax2.xaxis.get_ticklocs()
    new_ticks = [tick for tick in list(ticks) if tick < cnt_instances]
    new_ticks = new_ticks[0:-1]
    new_ticks.append(cnt_instances)
    ax2.xaxis.set_ticks(new_ticks)

    def format_func_contribution(value, tick_number):
        ticks = ax2.xaxis.get_majorticklocs()
        if value == ticks[-1]:
            return '{:,.2f}'.format(value / 1000).replace(',', '.') + "k"
        else:
            return '{:,.0f}'.format(value / 1000).replace(',', '.') + "k"
    ax2.xaxis.set_major_formatter(FuncFormatter(format_func_contribution)) 

    # Draw line to connect the contribution template bar from ax1 with ax2
    # Calculate the center of the third bar from the top (index 2, as index starts from 0)
    third_bar = bars[2]
    third_bar.set_linewidth(3)
    third_bar_center_x = third_bar.get_width() / 2
    third_bar_center_y = third_bar.get_y() + third_bar.get_height() / 2

    # Get the position of the title of ax2 (in ax2's coordinate system)
    title_position_ax2 = ax2.title.get_position()
    ax2_title_x = title_position_ax2[0] 
    ax2_title_y = title_position_ax2[1]  

    # Convert ax2's title position from ax2's coordinate system to figure coordinates 
    # and the figure coordinates to ax1's data coordinates
    ax2_title_fig_coords = ax2.transAxes.transform([ax2_title_x, ax2_title_y])
    ax1_title_x, ax1_title_y = ax1.transData.inverted().transform(ax2_title_fig_coords)

    ax1.plot([third_bar_center_x, ax1_title_x], [third_bar_center_y, ax1_title_y-2], 
             color='black', lw=1.5)

    # Save plot
    plt.savefig(f"{plots_dir}/{plot_file_name}", format="eps")


def plot_similarities(input_dataset_file_names, output_dataset_file_name, plot_file_name):
    logging.info(f"Creating plot: {plot_file_name}")


    # Prepare data for plot
    ontopop_df_viz_top_dfs = []
    for input_dataset_file_name in input_dataset_file_names:
        ontopop_df = pd.read_csv(f"{evaluate_dir}/{input_dataset_file_name}", escapechar='\\')
        logging.info(f"Loading dataset: {evaluate_dir}/{input_dataset_file_name}")

        ontopop_df_sub_1 = ontopop_df[["property", "avgMaxSemanticSimilarity", "ynCrowdsourcedValuesInText", "ynCrowdsourcedValuesInSnippets", "avgTokenCountPropertyValuePrediction"]]
        ontopop_df_sub_agg_1 = ontopop_df_sub_1.groupby(["property"]).agg(semantic_similarity_mean=pd.NamedAgg(column="avgMaxSemanticSimilarity", aggfunc="mean"),
                                                                          semantic_similarity_mean_no_errors=pd.NamedAgg(column="avgMaxSemanticSimilarity", aggfunc=lambda x: x[ontopop_df.loc[x.index, "errorType"].isna()].mean()),
                                                                    support_properties=pd.NamedAgg(column="property", aggfunc="count"),
                                                                    full_text_matches=pd.NamedAgg(column="ynCrowdsourcedValuesInText", aggfunc=lambda x: (x =="yes").sum()),
                                                                    snippets_matches=pd.NamedAgg(column="ynCrowdsourcedValuesInSnippets", aggfunc=lambda x: (x == "yes").sum()),
                                                                    avgTokenCount = pd.NamedAgg(column="avgTokenCountPropertyValuePrediction", aggfunc="mean"))
        ontopop_df_sub_2 = ontopop_df[["property", "errorType", "avgMaxSemanticSimilarity"]]
        ontopop_df_sub_2_filled_1 = ontopop_df_sub_2.copy()
        ontopop_df_sub_2_filled_1["errorType"] = ontopop_df_sub_2_filled_1["errorType"].fillna("NoError")
        ontopop_df_sub_2_agg = ontopop_df_sub_2_filled_1.groupby(by=["property","errorType"]).agg(support=pd.NamedAgg(column="avgMaxSemanticSimilarity", aggfunc="count"))
        ontopop_df_sub_2_agg = ontopop_df_sub_2_agg.reset_index()
        ontopop_df_sub_2_piv = ontopop_df_sub_2_agg.pivot(index="property", columns="errorType", values="support")
        ontopop_df_sub_2_filled_2 = ontopop_df_sub_2_piv.copy()
        ontopop_df_sub_2_filled_2 = ontopop_df_sub_2_filled_2.fillna(0)

        ontopop_df_sub_3 = ontopop_df[["property", "propertyName", "paperTitle"]]
        ontopop_df_agg_3 = ontopop_df_sub_3.groupby(["property", "propertyName"]).agg(support_papers=pd.NamedAgg(column="paperTitle", aggfunc=pd.Series.nunique))
        ontopop_df_agg_3 = ontopop_df_agg_3.reset_index()
        ontopop_df_agg_3 = ontopop_df_agg_3.set_index("property")
        ontopop_df_viz = pd.concat([ontopop_df_sub_agg_1, ontopop_df_sub_2_filled_2, ontopop_df_agg_3], join="inner", axis=1)

        # Add model label to dataset
        model = input_dataset_file_name.split("_")[3].split("-")[0]
        ontopop_df_viz.loc[:,"model"] = model

        # Reset index
        ontopop_df_viz = ontopop_df_viz.reset_index()

        # Append dataset to list
        ontopop_df_viz_top_dfs.append(ontopop_df_viz)

    # Combine all datasets into one dataset
    ontopop_df_viz_top = pd.concat(ontopop_df_viz_top_dfs, axis=0)

    # Round avgTokenCountPropertyValuePrediction
    ontopop_df_viz_top["avgTokenCount"] = ontopop_df_viz_top["avgTokenCount"].round().astype(int)

    logging.info(f'Global average semantic similarity: {ontopop_df_viz_top["semantic_similarity_mean"].mean()}')

    # Take only the rows with over 50 crowdsourced property values per property
    ontopop_df_viz_top = ontopop_df_viz_top[ontopop_df_viz_top["support_properties"] >= 50]
    ontopop_df_viz_top = ontopop_df_viz_top.reset_index()

    logging.info(f'Global average semantic similarity over properties with support >= 50: {ontopop_df_viz_top["semantic_similarity_mean"].mean()}')

    # Rename model label
    ontopop_df_viz_top.loc[ontopop_df_viz_top["model"]=="Meta", "model"] = "LLama3"

    # Save dataset
    ontopop_df_viz_top.to_csv(f"{visualize_dir}/{output_dataset_file_name}", escapechar='\\')

    # Plots
    fig, axs = plt.subplots(1,2,figsize=fig_size)

    similarities_df = ontopop_df_viz_top.copy()
    similarities_df = similarities_df.reset_index()
    similarities_df = similarities_df.sort_values(by=["support_properties", "property", "model"])

    # Global settings: Ticks
    cnt_rows = len(similarities_df["propertyName"])
    y_ticks = list(range(len(similarities_df["propertyName"])))

    # Global settings: Positions
    x0=0.18
    x1=0.24
    y0=0.10
    y1=0.85
    y_min = -0.5
    y_max = cnt_rows - 0.5


    # Subplot 1
    # Plot
    column_margin=0.5
    for i, v in enumerate(similarities_df["support_papers"].values):
        axs[0].text(0.5, i, v, va='center', ha="right", fontsize=10) if i%3 == 1 else None
    for i, v in enumerate(similarities_df["support_properties"].values):
        axs[0].text(0.5 + column_margin, i, v, va='center',ha="right", fontsize=10) if i%3 == 1 else None
    for i, v in enumerate(similarities_df["full_text_matches"].values):
        axs[0].text(0.5 + 2*column_margin, i, v, va='center', ha="right",fontsize=10) if i%3 == 1 else None
    for i, v in enumerate(similarities_df["snippets_matches"].values):
        axs[0].text(0.5 + 3*column_margin, i, v, va='center', ha="right",fontsize=10) if i%3 == 1 else None
    for i, v in enumerate(similarities_df["avgTokenCount"].values):
        axs[0].text(0.5 + 4*column_margin, i, v, va='center', ha="right",fontsize=10)

    # Ticks
    axs[0].yaxis.set_ticks(y_ticks[1::3])

    # Labels and Fonts
    support_paps_header = axs[0].text(0,0, "Support (papers)", fontsize=12, rotation=45)
    support_prop_header = axs[0].text(0,0, "Support (properties)", fontsize=12, rotation=45)
    full_text_matches = axs[0].text(0,0, "Full-text matches", fontsize=12, rotation=45)
    snippets_matches = axs[0].text(0,0, "Snippets matches", fontsize=12, rotation=45)
    tokenCnt_header = axs[0].text(0,0, "Average \ngenerated tokens", fontsize=12, rotation=45)
    axs[0].yaxis.set_label_text("Property labels", fontsize=16)

    ylabels = list(similarities_df["propertyName"])
    ylabels = ylabels[1::3]
    axs[0].yaxis.set_ticklabels(ylabels)
    axs[0].yaxis.set_tick_params(labelsize=12)

    # Visibility
    axs[0].spines['right'].set_visible(False)
    axs[0].spines['left'].set_visible(True)
    axs[0].spines['top'].set_visible(False)
    axs[0].spines['bottom'].set_visible(False)
    axs[0].xaxis.set_tick_params(which="both", length=0, labelbottom=False)
    axs[0].yaxis.set_tick_params(which="both", length=5, labelleft=True)

    # Position
    axs[0].set_position([x0, y0, x1-x0, y1-y0])
    axs[0].set_ylim(y_min, y_max)

    header_offset=0.1
    header_margin=0.5
    support_paps_header.set_position((x0 + header_offset,len(similarities_df[["support_properties"]])))
    support_prop_header.set_position((x0 + header_margin + header_offset,len(similarities_df[["support_properties"]])))
    full_text_matches.set_position((x0 + 2*header_margin + header_offset,len(similarities_df[["support_properties"]])))
    snippets_matches.set_position((x0 + 3*header_margin + header_offset,len(similarities_df[["support_properties"]])))
    tokenCnt_header.set_position((x0 + 4*header_margin + header_offset,len(similarities_df[["support_properties"]])))

    # Subplot 2
    similarities_df["NoValuesGenerated"] = similarities_df["NoValuesGenerated"] / similarities_df["support_properties"] 
    similarities_df["WrongAnswerFormat"] = similarities_df["WrongAnswerFormat"] / similarities_df["support_properties"] 
    similarities_df["XMLParsing"] = similarities_df["XMLParsing"] / similarities_df["support_properties"] 
    
    # Plot   
    bars_ax = similarities_df.plot.barh(ax=axs[1], x="propertyName", y="semantic_similarity_mean",
                                     facecolor='white', edgecolor='black')
    bars = bars_ax.patches

    # Bars
    linesstyles = [":", "--", "-"]
    facecolors = ["black", "dimgray", "lightgrey"]

    for j, bar in enumerate(bars[:len(similarities_df)]):
        bar.set_edgecolor('black')  
        bar.set_linestyle(linesstyles[j % 3]) 

    # Stacked error segments
    for i, bar in enumerate(bars[:len(similarities_df)]):  # Limit to main plot bars
        left = 0  # Start stacking from the left
        for col, facecolor, label in zip(
            ["NoValuesGenerated", "WrongAnswerFormat", "XMLParsing"],
            facecolors,
            ["NoValuesGenerated", "WrongAnswerFormat", "XMLParsing"]
        ):
            # Check if the error value is non-zero to decide whether to draw the sub-bar
            error_width = similarities_df.iloc[i][col] * bar.get_width()
            if error_width > 0:
                axs[1].barh(
                bar.get_y() + bar.get_height()  / 2,
                    error_width,
                    height=bar.get_height(), 
                    left=left,
                    linestyle=linesstyles[i % 3],
                    facecolor=facecolor,  
                    edgecolor="black",
                    label=label if i == 0 else ""  # Add label only once for legend
                )
            left += error_width  # Increment left to stack the next segment
    
    # Labels and Fonts
    axs[1].xaxis.set_label_text("Average semantic similarity (arithmetic mean)",
                                fontsize=16)

    for i, v in enumerate(similarities_df["semantic_similarity_mean"]):
        axs[1].text(v + 0.01, i, f"{v:.2f}", va='center', fontsize=10)

    axs[1].xaxis.set_tick_params(labelsize=12)

    y_tick_labels=list(similarities_df["model"].str[0])
    axs[1].yaxis.set_ticklabels(y_tick_labels)
    axs[1].yaxis.set_tick_params(labelsize=12)

    # Visibility
    axs[1].spines['right'].set_visible(False)
    axs[1].spines['top'].set_visible(False)
    axs[1].yaxis.set_label_text("", fontsize=16)
    axs[1].get_legend().remove()

    # Position
    axs[1].set_position([x1 + 0.13, y0, 0.6, y1-y0])
    axs[1].set_ylim(y_min, y_max)

    # Scale
    axs[1].xaxis.set_view_interval(0, 1)

    # Legend: Model
    model_handles = [
        mpatches.Patch(facecolor="white", edgecolor="black", linestyle="-", label="M: Mistral"),
        mpatches.Patch(facecolor="white", edgecolor="black", linestyle="--", label="L: LLama3"),
        mpatches.Patch(facecolor="white", edgecolor="black", linestyle=":", label="F: Falcon"),
    ]
    model_legend = axs[1].legend(
        handles=model_handles, 
        loc="upper right",  # Position for the model legend
        fontsize=10, 
        title="Models", 
        title_fontsize=16
    )

    axs[1].add_artist(model_legend)

    # Model: Error types
    error_handles = [
        mpatches.Patch(facecolor=color, edgecolor="black", label=label)
        for color, label in zip(facecolors, ["NoValuesGenerated", "WrongAnswerFormat", "XMLParsing"])
    ]
    axs[1].legend(
        handles=error_handles, 
        loc="lower right", 
        fontsize=10, 
        title="Errors", 
        title_fontsize=16
    )

    # Save the plot
    plt.savefig(f"{plots_dir}/{plot_file_name}", format="eps")


def plot_hallucination(input_dataset_file_names, output_dataset_file_name, plot_file_name):
    # Hypothesis: The LLM hallucinates.
    # Filter: Percentage of generated property values that are not found in the snippets.
    # Cases: 
    # Case1: High semantic similarity and high number of matches
    # Conclusion: properties for which RAG performs reasonably well.
    # Case2: High semantic similarity and low number of matches
    # Conclusion: properties for which RAG hallucinates but generates a property value that semantically aligns with the user value.
    # Subsequent NER as solution to match the exact value that the user used?
    # Case3: Low semantic similarity and high number of matches
    # does not exist
    # Case4: 
    # Low semantic similarity and low number of matches
    # The model does not perform well

    logging.info(f"Creating plot: {plot_file_name}")

    # Prepare data for plot
    ontopop_df_viz_top_dfs = []
    for input_dataset_file_name in input_dataset_file_names:
        ontopop_df = pd.read_csv(f"{evaluate_dir}/{input_dataset_file_name}", escapechar='\\')
        logging.info(f"Loading dataset: {evaluate_dir}/{input_dataset_file_name}")

        model = input_dataset_file_name.split("_")[3].split("-")[0]
        ontopop_df.loc[:,"model"] = model
        ontopop_df_sub = ontopop_df[["model", "paper", "property", "paperTitle",
                                    "propertyName", "propertyValues", "ynExactMatchSnippets",
                                    "ynExactMatchFullText", "avgMaxSemanticSimilarity",
                                     "avgTokenCountPropertyValuePrediction", "errorType"]]
        
        ontopop_df_errors = ontopop_df_sub[~ontopop_df_sub["errorType"].isna()]
        logging.info(f"Total rows for {model}: {len(ontopop_df)}")
        logging.info(f"Thereof rows with generated property values: {len(ontopop_df) - len(ontopop_df_errors)}")
        
        # Take the subset where no errors occured during generation
        ontopop_df_sub = ontopop_df_sub[ontopop_df_sub["errorType"].isna()].copy()
        cnt_match = len(ontopop_df_sub[ontopop_df_sub["ynExactMatchSnippets"] == "yes"])
        logging.info(f"Thereof lookup matches of generated property values in snippets, as percentage: {round(np.mean(cnt_match/len(ontopop_df_sub))*100,2)}% and as fraction: {cnt_match}/{len(ontopop_df_sub)}")    

        ontopop_df_sub_agg_1 = ontopop_df_sub.groupby(["model", "property", "propertyName"]).agg(semantic_similarity_mean=pd.NamedAgg(column="avgMaxSemanticSimilarity", aggfunc="mean"),
                                                                                                 support_properties=pd.NamedAgg(column="property", aggfunc="count"),
                                                                                                 avgTokenCount = pd.NamedAgg(column="avgTokenCountPropertyValuePrediction", aggfunc="mean"))
        ontopop_df_sub_agg_1 = ontopop_df_sub_agg_1[ontopop_df_sub_agg_1["support_properties"] >= 50]

        ontopop_df_sub_agg_2 = ontopop_df_sub[ontopop_df_sub["ynExactMatchSnippets"] == "yes"]
        ontopop_df_sub_agg_2 = ontopop_df_sub_agg_2.groupby(["model", "property", "propertyName"]).agg(cnt_matches=pd.NamedAgg(column="avgMaxSemanticSimilarity", aggfunc="count"))

        ontopop_df_sub_agg = pd.merge(ontopop_df_sub_agg_1, ontopop_df_sub_agg_2, on=["model", "property", "propertyName"], how="inner")

        ontopop_df_sub_agg["pct_hallucination"] = 1 - np.round(ontopop_df_sub_agg["cnt_matches"]/ontopop_df_sub_agg["support_properties"], 2)
        ontopop_df_sub_agg = ontopop_df_sub_agg.reset_index()
        ontopop_df_viz_top_dfs.append(ontopop_df_sub_agg)
    ontopop_df_viz_top = pd.concat(ontopop_df_viz_top_dfs, axis=0)
    
    # Filter out the properties for which there is not enough support from all three models
    ontopop_df_viz_top["cnt_models_per_property"] = ontopop_df_viz_top.groupby(["property"])["model"].transform("count")
    ontopop_df_viz_top = ontopop_df_viz_top[ontopop_df_viz_top["cnt_models_per_property"] == 3]

    # Rename model label
    ontopop_df_viz_top.loc[ontopop_df_viz_top["model"]=="Meta", "model"] = "LLama3"
    
    # Round avgTokenCountPropertyValuePrediction
    ontopop_df_viz_top["avgTokenCount"] = ontopop_df_viz_top["avgTokenCount"].round().astype(int)

    # Save dataset
    ontopop_df_viz_top.to_csv(f"{visualize_dir}/{output_dataset_file_name}", escapechar='\\')
    
    # Plots
    fig, axs = plt.subplots(1,2,figsize=fig_size)

    hallucination = ontopop_df_viz_top.copy()
    hallucination = hallucination.reset_index()
    hallucination = hallucination.sort_values(by=["property", "model"])

    # Global settings: Positions
    x0=0.25
    x1=0.28
    y0=0.10
    y1=0.85
    y_min = -0.5
    y_max = 20.5

    # Global settings: Ticks
    y_ticks = list(range(len(hallucination["propertyName"])))

    # Subplot 1
    # Plot: support (properties)
    for i, v in enumerate(hallucination["support_properties"].values):
        axs[0].text(0.3, i, v, va='center', fontsize=10)
    for i, v in enumerate(hallucination["avgTokenCount"].values):
        axs[0].text(1.3, i, v, va='center', fontsize=10)

    # Ticks
    axs[0].yaxis.set_ticks(y_ticks)

    # Labels and Fonts
    support_header = axs[0].text(0,0, "Support \n(properties)", fontsize=12, rotation=45)
    tokenCnt_header = axs[0].text(0,0, "Average \ngenerated tokens", fontsize=12, rotation=45)
    axs[0].yaxis.set_label_text("Property labels", fontsize=16)

    ylabels = list(hallucination["propertyName"] + " - " + hallucination["model"].str[0] + ":")
    for i, label in enumerate(ylabels):
        if(i%3!=1):
            ylabels[i] = label[-2:]
    axs[0].yaxis.set_ticklabels(ylabels)
    axs[0].yaxis.set_tick_params(labelsize=12)

    # Visibility
    axs[0].spines['right'].set_visible(False)
    axs[0].spines['left'].set_visible(True)
    axs[0].spines['top'].set_visible(False)
    axs[0].spines['bottom'].set_visible(False)
    axs[0].xaxis.set_tick_params(which="both", length=0, labelbottom=False)
    axs[0].yaxis.set_tick_params(which="both", length=5, labelleft=True)

    # Position
    axs[0].set_position([x0, y0, x1-x0, y1-y0])
    axs[0].set_ylim(y_min, y_max)

    header_offset=-0.3
    support_header.set_position((x0 + header_offset,len(hallucination[["support_properties"]])))
    tokenCnt_header.set_position((x0 + 1.5 + header_offset,len(hallucination[["support_properties"]])))

    # Subplot 2
    # Plot   
    bars = hallucination.plot.barh(ax=axs[1], x="propertyName", y="pct_hallucination",
                        facecolor='white', edgecolor='black')
    
    # Bars
    linesstyles = [":", "--", "-"]
    for j, bar in enumerate(bars.patches):
        bar.set_edgecolor('black')  
        bar.set_linestyle(linesstyles[j % 3]) 

    # Create custom legend handles with labels M, L, and F
    legend_handles = [
        mpatches.Patch(facecolor='white', edgecolor='black', linestyle="-", label="M: Mistral"),
        mpatches.Patch(facecolor='white', edgecolor='black', linestyle="--", label="L: LLama3"),
        mpatches.Patch(facecolor='white', edgecolor='black', linestyle=":", label="F: Falcon")
    ]
    
    # Labels and Fonts
    axs[1].xaxis.set_label_text("Generated property values not contained in snippets (in percent)",
                      fontsize=16)

    for i, v in enumerate(hallucination["pct_hallucination"]):
        axs[1].text(v + 0.01, i, f"{v:.2f}", va='center', fontsize=10)

    # Ticks
    axs[1].xaxis.set_tick_params(labelsize=12)
    axs[1].yaxis.set_tick_params(labelsize=12)

    # Visibility
    axs[1].spines['right'].set_visible(False)
    axs[1].spines['top'].set_visible(False)
    axs[1].yaxis.set_tick_params(which="both", length=0, labelleft=False)
    axs[1].yaxis.set_label_text("", fontsize=16)
    axs[1].get_legend().remove()

    # Position
    axs[1].set_position([x1 + 0.03, y0, 0.6, y1-y0])
    axs[1].set_ylim(y_min, y_max)

    # Scale
    axs[1].xaxis.set_view_interval(0, 1)

    # Figure
    # Add the legend to the middle of the plot
    fig.legend(handles=legend_handles, loc='lower left',
                frameon=False, fontsize=10, title="Bar Styles", title_fontsize=16, ncol=3)
    
    # Save the plot
    plt.savefig(f"{plots_dir}/{plot_file_name}", format="eps")


def calc_corr_tokens_vs_errors(input_dataset_file_names, output_dataset_file_name):
    corr = 0

    logging.info(f"calculating correlation between number of tokens and generation errors")

    # Prepare data for plot
    ontopop_dfs = []
    for input_dataset_file_name in input_dataset_file_names:
        ontopop_df = pd.read_csv(f"{evaluate_dir}/{input_dataset_file_name}", escapechar='\\')
        
        model = input_dataset_file_name.split("_")[3].split("-")[0]
        ontopop_df.loc[:,"model"] = model

        ontopop_dfs.append(ontopop_df)

    ontopop_df = ontopop_df.reset_index()
    ontopop_df = pd.concat(ontopop_dfs, axis=0)

    # Rename model label
    ontopop_df.loc[ontopop_df["model"]=="Meta", "model"] = "LLama3"

    # Select columns
    ontopop_df = ontopop_df[["property", "avgTokenCountPropertyValuePrediction", "errorType"]]

    # Group by property and calculate average token count per property
    property_avg_token = ontopop_df.groupby("property")["avgTokenCountPropertyValuePrediction"].mean().reset_index()

    # Calculate the error ratio for each property (NoValuesGenerated errors / total property values)
    error_count = ontopop_df[ontopop_df["errorType"] == "NoValuesGenerated"].groupby("property").size().reset_index(name="errorCount")
    total_property_count = ontopop_df.groupby("property").size().reset_index(name="totalCount")

    # Merge error counts and total counts to get the error ratio
    property_error_ratio = pd.merge(error_count, total_property_count, on="property")
    property_error_ratio["errorRatio"] = property_error_ratio["errorCount"] / property_error_ratio["totalCount"]

    # Merge the error ratio with the average token counts
    property_data = pd.merge(property_avg_token, property_error_ratio[["property", "errorRatio"]], on="property")

    # Calculate the correlation between average token count and error ratio
    corr = property_data["avgTokenCountPropertyValuePrediction"].corr(property_data["errorRatio"])
    logging.info(f"Calculated correlation: {corr}")

    # save results to a file
    property_data.to_csv(f"{visualize_dir}/{output_dataset_file_name}", index=False)

    return corr


##########################################################################################
# Pipeline
##########################################################################################
input_dataset_file_names = []
for file in os.listdir(f"{evaluate_dir}"):
    input_dataset_file_names.append(file)

# template properties
input_dataset_file_name="templates.csv"
output_dataset_file_name=""
plot_file_name="template_properties.eps"
plot_template_properties(input_dataset_file_name, output_dataset_file_name, plot_file_name)

# template usage
input_dataset_file_name="templates.csv"
output_dataset_file_name=""
plot_file_name="template_usage.eps"
plot_template_usage(input_dataset_file_name, output_dataset_file_name, plot_file_name)

# template utilization
input_dataset_file_name="templates.csv"
contr_tmpl_dataset_file_name="contribution_template_util.csv"
output_dataset_file_name=""
plot_file_name="template_utilization.eps"
plot_template_utilization(input_dataset_file_name, contr_tmpl_dataset_file_name, output_dataset_file_name, plot_file_name)

# llm:similarities
output_dataset_file_name = f"ontopop_similarities_{pdf_parser}_{shots}.csv"
plot_file_name = f"ontopop_similarities_{pdf_parser}_{shots}.eps"
plot_similarities(input_dataset_file_names, output_dataset_file_name, plot_file_name)

# llm:hallucination
output_dataset_file_name = f"ontopop_hallucination_{pdf_parser}_{shots}.csv"
plot_file_name = f"ontopop_hallucination_{pdf_parser}_{shots}.eps"
plot_hallucination(input_dataset_file_names, output_dataset_file_name, plot_file_name)

# correlation between number of tokens and generated errors per property
output_dataset_file_name = f"ontopop_correlation_{pdf_parser}_{shots}.csv"
corr = calc_corr_tokens_vs_errors(input_dataset_file_names, output_dataset_file_name)
logging.info(corr)