#####
# Annotates all *.xml files found in the folder given as parameter to this script.
# The output can be found in the folder annotations/.
#
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

