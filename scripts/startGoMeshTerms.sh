# Launches a memory-resident dictionary server that can recognize
# GeneOntology (GO) and MeSH terms

JAVA="java -XX:ThreadStackSize=256k -XX:+UseCompressedOops -XX:+UseParallelGC"
CP="lib/gnat.jar"
DICT="dictionaries"

#GO and MeSH terms in a single dictionary
nohup ${JAVA} -cp ${CP} -Xmx2000M gnat.server.dictionary.DictionaryServer 56099 ${DICT}/goMesh/ &

