#####
# Annotates all *.txt files found in the folder given as parameter to this script.
# The output can be found in the file justAnnotate.result.out
#
# Written for BASH shells; in others shells, you might get an error like
# "if: Expression Syntax." In this case, just delete everything except for the 
#   java -cp ...
# line --- or adjust the IF statment according to your shell's syntax.
#####

if [ $# -eq 0 ]
  then
    echo "Please specify a directory with *.txt files!"
  else
    java -cp lib/gnat.jar:lib/mysql-connector.jar gnat.client.JustAnnotate -v=0 -nodir -out justAnnotate.result.out $1 $2 $3
fi

