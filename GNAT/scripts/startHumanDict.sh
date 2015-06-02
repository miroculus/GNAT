# Launches a memory-resident dictionary server that can recognize
# gene names and also provides a list of candidate EntrezGene IDs
# for each such recognized entity. Does NOT do normalization/grounding
# per se, just provides ALL possible IDs that share the same/similar
# gene name.
# In GNAT, every organism has its own dictionary server. Depending on
# which species you are interested in, you will have to launch the 
# corresponding servers. Some scripts are provided already (for
# mouse, fruit fly, and human); the dictionaries/ folder contains 
# the binary dictionaries for several more species, stored by their
# respective NCBI taxonomy ID -- 9606 for human, etc. You can copy
# the provided startup scripts and change them for the other species
# that you might need; experiment with the memory sizei (-Xmx parameter),
# not all species need a max of 1.5GB like human does.
#####

JAVA="java -XX:ThreadStackSize=256k -XX:+UseCompressedOops -XX:+UseParallelGC"
CP="lib/gnat.jar"
DICT="dictionaries"

#human
nohup ${JAVA} -cp ${CP} -Xmx2500M gnat.server.dictionary.DictionaryServer 56001 ${DICT}/9606/ &

