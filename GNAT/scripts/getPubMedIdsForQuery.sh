# Retrieves a list of PubMed IDs matching the given query. Uses PubMed's eUtils for searching and can therefore
# handle PubMed query syntax.
# Enclose multi-term queries in double quotes:
#   bash getPubMedIdsForQuery "Human[MH] AND p53[TI]"
# Will print to STDOUT.

java -cp lib/gnat.jar:lib/jdom-1.0.jar gnat.retrieval.PubmedAccess --getids "$1"
