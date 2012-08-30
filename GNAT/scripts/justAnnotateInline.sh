#####
# Annotates all *.xml files found in the folder given as parameter to this script.
# The output can be found in the folder annotations/.
#
##
# JustAnnotateInline -- annotates genes in a text or text collection by adding
# inline XML tags. Supported file formats:
# - plain text (one file each),
# - Medline XML (MedlineCitation and MedlineCitationSet),
# - PubMed XML (PubmedArticle and PubmedArticleSet),
# - GZipped Medline/Pubmed XML files like 'medline12n0123.xml.gz'
# As a convention, Medline/Pubmed XML files have to be named as such:
# - *.medline.xml   -  single MedlineCitation/PubmedArticle in XML,
# - *.medlines.xml  -  MedlineCitationSet/PubmedArticleSet,
# - medline<year>n<number>.xml(.gz)  -  MedlineCitationSet,
# - <id>.txt        -  single plain text,
# - <id>.xml        -  single text in XML format
#
##
# Call: JustAnnotateInline <dir>
#  <dir>     -  directory with one or more .txt, .xml, or .xml.gz files
# Optional parameters:
#  -v=<int>  -  Set verbosity level for progress and debugging information
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
    java -cp lib/gnat.jar:lib/jdom-1.0.jar:lib/mysql-connector.jar gnat.client.JustAnnotateInline -v=0 -outdir annotations $1 $2 $3
fi

