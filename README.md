Diachronic Analysis of Entities by Exploiting Wikipedia Page revisions
=========================================================================

Code for the paper "Diachronic Analysis of Entities by Exploiting Wikipedia Page revisions" accepted to RANLP 2019 - Recent Advances in Natural Language Processing.

In the last few years, the increasing availability of large corpora spanning several time periods has opened new opportunities for the diachronic analysis of language.
This type of analysis can bring to the light not only linguistic phenomena related to the shift of word meanings over time, but it can also be used to study the impact that societal and cultural trends have on this language change.
This paper introduces a new resource for performing the diachronic analysis of named entities built upon Wikipedia page revisions.
This resource enables the analysis over time of changes in the relations between entities (concepts), surface forms (words), and the contexts surrounding entities and surface forms, by analysing the whole history of Wikipedia internal links.

This repository contains the code for creating the dataset by exploiting Wikipedia page revisions. Details about the algorithm are published in the following paper:

**BibTex is not yet available**

Please, cite the paper if you adopt our tool.

Setup
--------

1. Install Java JDK 1.8, Maven, Git.
2. Clone the project using Git.
3. Compile the project using the command: mvn package.
4. Execute the bash script run.sh followed by the class name and arguments (see Usage for more details).

Usage
--------

The dump processing requires three steps:
1. Download Wikipedia history dump
2. Dump processing
3. Aggregation

### Download Wikipedia history dump

For downloading the Wikipedia dump you can run the class **di.uniba.dae.processing.Downloader**.

> usage: Downloader - Download the whole history Wikipedia dump<br>
 -d <arg>   Dump date (for example 20181101)<br>
 -n <arg>   Number of download threads (default 3)<br>

You can check available English dumps here: https://dumps.wikimedia.org/enwiki/. The tool works also with other languages.

You can download the dump by using other tools, but you need all the pages-meta-history files for creating the dataset.

### Dump processing

During the download a fold with the dump date is created in the current directory. The fold contains a subfold named **dowload** that contains all the dumps.

For processing the dumps you must run the class **di.uniba.dae.processing.ProcessDump**.

> usage: ProcessDump - Build CSV files for entities extracted from a Wikipedia dump<br>
 -d <arg>     Input directory<br>
 -exc <arg>   Load exclude file<br>
 -l <arg>     Download log<br>
 -o <arg>     Output directory<br>
 -t <arg>     Number of processing threads (default 4)

This tool creates a fold named **csv** into the output directory containing the csv files with information about year, target, surface and context. Read the paper for more details.

You can exclude a list of files from the processing by providing a text file with the filepath for each line. This file must be provided by the option **-exc**. The option **-l** can be used for providing the log of the **di.uniba.dae.processing.Downloader**, in this case, the class processes only files that have been successfully downloaded.

### Aggregation

Since the tuple **(year, surface form, target)** can occur multiple times, we aggregate multiple tuple occurrences in a single record. The aggregation step is performed several times, one time for each dump file plus a final step that aggregates all the records in a single file that represents the final dataset.

For performing the first step you must run the class **di.uniba.dae.post.Aggregate**

> usage: Aggregate data - Build final dataset by aggregating data from CSVs<br>
 -d         Delete CSV file<br>
 -i <arg>   Input dir<br>
 -o <arg>   Output directory<br>
 -w         Overwrite files

The **input dir** is the fold that contains CSV files. The option **-w** overwrites existing files into the output dir.

The dataset can be used without the second aggregation step by reading the files produced by the first aggregation step. If you want to perfom the final aggregation step you need to run the class **di.uniba.dae.analysis.BuildFinalDataset**.

> usage: Build final dataset<br>
 -b <arg>   Batch size<br>
 -d <arg>   Dictionary<br>
 -i <arg>   Input dir<br>
 -m <arg>   Min occurrences<br>
 -o <arg>   Output file

This process requires a lot of memory, for that reason is possible to run the process on a subset of all targets in the dataset, you can provide the subset in a text file using the option **-d** in the form of one target for each line. You can process the dataset using a batch size to avoid the out of memory error. The batch processing divides the dictionary into several batches.
The **-m** option removes, from the context bag-of-word, words occurring less than the **-m** argument times. 

### Pre-processed dataset

You can download the aggregated CSV files for the dump *20190201* here: **URL not yet available**.

You can download the final dataset for the dump *20190201* here: **URL not yet available**.

### Dataset indexing

It is possible to index the dataset by using Apache Lucene. For indexing, you can run the class **di.uniba.dae.api.CreateIndex**.

> usage: CreateIndex - Creates the index given the input dataset<br>
 -i <arg>    Input dateset<br>
 -mb <arg>   Max context BoW size (default 1000)<br>
 -mo <arg>   Min token occurrences (default 5)<br>
 -ms <arg>   Min surface count (default 5)<br>
 -o <arg>    Index directory

The class **di.uniba.dae.api.TestSearch** shows some example usages of the index for searching and analysing how contexts, surface forms and targets change over time.
