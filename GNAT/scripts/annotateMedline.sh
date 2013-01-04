#!/bin/bash

#####
# Annotates all Medline XML files found in the folder given as parameter to this script.
# Identifies files to annotate by file extension .xml, zipped files (file extension .xml.gz) are supported as well.
# The output can be found in the folder annotations/.
#
# If your machine has less than the amount of memory specified below, change the parameter "-Xmx...M" accordingly.
#
##
# AnnotateMedline -- annotates genes in a Medline citation set
# Supported file formats:
# - Medline XML (MedlineCitation and MedlineCitationSet),
# - PubMed XML (PubmedArticle and PubmedArticleSet),
# - GZipped Medline/Pubmed XML files like 'medline12n0123.xml.gz'
# As a convention, Medline/Pubmed XML files have to be named as such:
# - medline<year>n<number>.xml(.gz)
# Call: AnnotateMedline <dir>
#  <dir>     -  directory with one or more .xml or .xml.gz files
# Optional parameters:
#  -g        -  Print only those texts to the output that have a gene
#  -v=<n>    -  Set verbosity level for progress and debugging information
#               Default: 0; warnings: 1, status: 2, ... debug: 6
#  --outdir  -  Folder in which to write the output XML
#               By default, will write into the current directory.
#
##
# Written for BASH shells; in others shells, you might get an error like
# "if: Expression Syntax." In this case, just delete everything except for the 
#   java -cp ...
# line --- or adjust the IF statment according to your shell's syntax.
#####

if [ $# -eq 0 ]
  then
    echo "Please specify a directory with *.xml files!"
  else
    java -Xmx25000M -cp lib/gnat.jar:lib/jdom-1.0.jar:lib/mysql-connector.jar gnat.client.AnnotateMedline -v=0 -outdir annotations $1 $2 $3 $4 $5 $6 $7 $8 $9
fi

