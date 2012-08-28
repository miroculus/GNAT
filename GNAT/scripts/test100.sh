
# Annotate the set of 100 documents
echo "Annotating..."
java -cp lib/gnat.jar:lib/mysql-connector.jar gnat.client.JustAnnotate -nodir texts/test100 > test100.result.out

# Evaluate
echo "Evaluating"
python texts/test100/bc2scoring.py texts/test100/test100.genelist test100.result.out


