
# Annotate the set of 100 documents
java -cp lib/gnat.jar:lib/mysql-connector-java-5.1.17-bin.jar gnat.client.JustAnnotate -nodir texts/test100 > texts/test100.result.out

# Evaluate
python texts/bc2scoring.py texts/test100.genelist texts/test100.result.out


